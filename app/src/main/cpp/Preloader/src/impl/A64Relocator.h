#pragma once
// Copia las primeras 4 instrucciones ARM64 de una función a un trampolín, reubicando las
// que dependen del PC (ADR/ADRP, LDR literal, B/BL, B.cond, CBZ/CBNZ, TBZ/TBNZ).
// Es código puro (sin llamadas al sistema) para poder probarlo en el host.
#include <cstdint>
#include <cstring>

namespace pl::impl::a64 {

constexpr uint32_t kLdrX17_8  = 0x58000051; // LDR X17, #8   (literal en +8)
constexpr uint32_t kLdrX17_12 = 0x58000071; // LDR X17, #12  (literal en +12)
constexpr uint32_t kBrX17     = 0xD61F0220;
constexpr uint32_t kBlrX17    = 0xD63F0220;
constexpr uint32_t kB_plus12  = 0x14000003; // B +12

constexpr int kPatchInsns = 4;                 // 16 bytes sobrescritos en la función
constexpr size_t kMaxTrampolineBytes = 4 * 20 + 16;

struct Emitter {
    uint32_t *p;
    void w32(uint32_t v) { *p++ = v; }
    void w64(uint64_t v) { std::memcpy(p, &v, 8); p += 2; }
};

inline int64_t sext(uint64_t v, int bits) {
    const uint64_t m = 1ull << (bits - 1);
    return static_cast<int64_t>((v ^ m) - m);
}

// Salto absoluto incondicional: LDR X17,#8 ; BR X17 ; .quad target  (16 bytes)
inline void emitJump(Emitter &e, uint64_t target) {
    e.w32(kLdrX17_8); e.w32(kBrX17); e.w64(target);
}

inline void relocateOne(uint32_t insn, uint64_t pc, Emitter &e) {
    // ADR / ADRP
    if ((insn & 0x1F000000) == 0x10000000) {
        const uint32_t rd = insn & 31;
        const int64_t imm = sext((((insn >> 5) & 0x7FFFF) << 2) | ((insn >> 29) & 3), 21);
        const uint64_t addr = (insn & 0x80000000)
                                  ? (pc & ~0xFFFull) + static_cast<uint64_t>(imm << 12)
                                  : pc + static_cast<uint64_t>(imm);
        e.w32(0x58000040 | rd);   // LDR Xd, #8
        e.w32(kB_plus12 - 0);     // B +12 (salta el literal)
        e.w64(addr);
        return;
    }
    // LDR (literal): GPR y SIMD, LDRSW, PRFM
    if ((insn & 0x3B000000) == 0x18000000) {
        const uint32_t opc = insn >> 30, v = (insn >> 26) & 1, rt = insn & 31;
        const uint64_t addr = pc + static_cast<uint64_t>(sext((insn >> 5) & 0x7FFFF, 19) * 4);
        uint32_t ld;
        if (!v) {
            static const uint32_t t[4] = {0xB9400000, 0xF9400000, 0xB9800000, 0xF9800000};
            ld = t[opc];
        } else {
            static const uint32_t t[4] = {0xBD400000, 0xFD400000, 0x3DC00000, 0};
            ld = t[opc];
            if (!ld) { e.w32(insn); return; } // encoding reservado
        }
        e.w32(kLdrX17_12);                 // LDR X17, #12
        e.w32(ld | (17u << 5) | rt);       // LDR Rt, [X17]
        e.w32(kB_plus12);                  // B +12
        e.w64(addr);
        return;
    }
    // B / BL
    if ((insn & 0x7C000000) == 0x14000000) {
        const uint64_t tgt = pc + static_cast<uint64_t>(sext(insn & 0x3FFFFFF, 26) * 4);
        if (insn & 0x80000000) { // BL: vuelve al trampolín tras el BLR
            e.w32(kLdrX17_12); e.w32(kBlrX17); e.w32(kB_plus12); e.w64(tgt);
        } else {
            emitJump(e, tgt);
        }
        return;
    }
    // B.cond  (se invierte la condición y se salta 20 bytes si NO se cumplía)
    if ((insn & 0xFF000010) == 0x54000000) {
        const uint32_t cond = insn & 0xF;
        const uint64_t tgt = pc + static_cast<uint64_t>(sext((insn >> 5) & 0x7FFFF, 19) * 4);
        if (cond >= 0xE) { emitJump(e, tgt); return; }
        e.w32(0x54000000 | (5u << 5) | (cond ^ 1u));
        emitJump(e, tgt);
        return;
    }
    // CBZ / CBNZ
    if ((insn & 0x7E000000) == 0x34000000) {
        const uint64_t tgt = pc + static_cast<uint64_t>(sext((insn >> 5) & 0x7FFFF, 19) * 4);
        e.w32(((insn & ~(0x7FFFFu << 5)) ^ (1u << 24)) | (5u << 5));
        emitJump(e, tgt);
        return;
    }
    // TBZ / TBNZ
    if ((insn & 0x7E000000) == 0x36000000) {
        const uint64_t tgt = pc + static_cast<uint64_t>(sext((insn >> 5) & 0x3FFF, 14) * 4);
        e.w32(((insn & ~(0x3FFFu << 5)) ^ (1u << 24)) | (5u << 5));
        emitJump(e, tgt);
        return;
    }
    e.w32(insn); // no depende del PC
}

// Construye el trampolín en `out` (>= kMaxTrampolineBytes). Devuelve los bytes escritos.
inline size_t buildTrampoline(const uint32_t *src, uint64_t srcPc, uint32_t *out) {
    Emitter e{out};
    for (int i = 0; i < kPatchInsns; ++i) relocateOne(src[i], srcPc + 4ull * i, e);
    emitJump(e, srcPc + 4ull * kPatchInsns);
    return static_cast<size_t>(reinterpret_cast<uint8_t *>(e.p) - reinterpret_cast<uint8_t *>(out));
}

} // namespace pl::impl::a64
