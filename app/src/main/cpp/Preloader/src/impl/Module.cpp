#include "Module.h"

#include <elf.h>
#include <link.h>
#include <cstdlib>
#include <cstring>

namespace pl::impl {
namespace {

struct Query { const char *name; Module *out; bool found; };

const char *baseName(const char *p) {
    const char *s = strrchr(p, '/');
    return s ? s + 1 : p;
}

int iterCb(dl_phdr_info *info, size_t, void *data) {
    auto *q = static_cast<Query *>(data);
    if (!info->dlpi_name || !*info->dlpi_name) return 0;
    if (strcmp(info->dlpi_name, q->name) != 0 &&
        strcmp(baseName(info->dlpi_name), baseName(q->name)) != 0)
        return 0;

    Module &m = *q->out;
    m = Module{};
    m.path = info->dlpi_name;
    m.bias = info->dlpi_addr;
    for (int i = 0; i < info->dlpi_phnum; ++i) {
        const ElfW(Phdr) &ph = info->dlpi_phdr[i];
        if (ph.p_type == PT_DYNAMIC) m.dynamic = m.bias + ph.p_vaddr;
        if (ph.p_type == PT_LOAD && (ph.p_flags & PF_X) && (ph.p_flags & PF_R))
            m.exec.push_back({m.bias + ph.p_vaddr, static_cast<size_t>(ph.p_memsz)});
    }
    q->found = true;
    return 1;
}

uint32_t gnuHash(const char *s) {
    uint32_t h = 5381;
    for (; *s; ++s) h = h * 33 + static_cast<uint8_t>(*s);
    return h;
}

uint32_t sysvHash(const char *s) {
    uint32_t h = 0, g;
    while (*s) {
        h = (h << 4) + static_cast<uint8_t>(*s++);
        g = h & 0xf0000000;
        if (g) h ^= g >> 24;
        h &= ~g;
    }
    return h;
}

} // namespace

bool findModule(const char *name, Module &out) {
    if (!name || !*name) return false;
    Query q{name, &out, false};
    dl_iterate_phdr(iterCb, &q);
    return q.found;
}

uintptr_t lookupSymbol(const Module &m, const char *name, size_t *size) {
    if (!m.dynamic || !name) return 0;

    uintptr_t symtab = 0, strtab = 0, gnu = 0, sysv = 0;
    for (auto *d = reinterpret_cast<const ElfW(Dyn) *>(m.dynamic); d->d_tag != DT_NULL; ++d) {
        switch (d->d_tag) {
            case DT_SYMTAB:   symtab = d->d_un.d_ptr; break;
            case DT_STRTAB:   strtab = d->d_un.d_ptr; break;
            case DT_GNU_HASH: gnu = d->d_un.d_ptr; break;
            case DT_HASH:     sysv = d->d_un.d_ptr; break;
            default: break;
        }
    }
    // bionic deja los d_ptr sin reubicar; glibc los reubica. Soporta ambos.
    auto fix = [&](uintptr_t v) { return (v && v < m.bias) ? v + m.bias : v; };
    symtab = fix(symtab); strtab = fix(strtab); gnu = fix(gnu); sysv = fix(sysv);
    if (!symtab || !strtab) return 0;

    auto *syms = reinterpret_cast<const ElfW(Sym) *>(symtab);
    auto *str = reinterpret_cast<const char *>(strtab);
    auto match = [&](const ElfW(Sym) &s) -> uintptr_t {
        if (s.st_shndx == SHN_UNDEF || s.st_value == 0) return 0;
        if (strcmp(str + s.st_name, name) != 0) return 0;
        if (size) *size = s.st_size;
        return m.bias + s.st_value;
    };

    if (gnu) {
        auto *gh = reinterpret_cast<const uint32_t *>(gnu);
        uint32_t nbuckets = gh[0], symoffset = gh[1], bloomSize = gh[2], bloomShift = gh[3];
        auto *bloom = reinterpret_cast<const ElfW(Addr) *>(gh + 4);
        auto *buckets = reinterpret_cast<const uint32_t *>(bloom + bloomSize);
        auto *chain = buckets + nbuckets;
        if (nbuckets == 0 || bloomSize == 0) return 0;

        uint32_t h = gnuHash(name);
        constexpr uint32_t kBits = sizeof(ElfW(Addr)) * 8;
        ElfW(Addr) word = bloom[(h / kBits) % bloomSize];
        ElfW(Addr) mask = (ElfW(Addr)(1) << (h % kBits)) | (ElfW(Addr)(1) << ((h >> bloomShift) % kBits));
        if ((word & mask) != mask) return 0;

        uint32_t idx = buckets[h % nbuckets];
        if (idx < symoffset) return 0;
        for (;; ++idx) {
            uint32_t h2 = chain[idx - symoffset];
            if ((h2 | 1) == (h | 1)) {
                if (uintptr_t a = match(syms[idx])) return a;
            }
            if (h2 & 1) break;
        }
        return 0;
    }

    if (sysv) {
        auto *sh = reinterpret_cast<const uint32_t *>(sysv);
        uint32_t nbucket = sh[0];
        auto *buckets = sh + 2;
        auto *chain = buckets + nbucket;
        if (nbucket == 0) return 0;
        for (uint32_t i = buckets[sysvHash(name) % nbucket]; i != 0; i = chain[i]) {
            if (uintptr_t a = match(syms[i])) return a;
        }
    }
    return 0;
}

bool looksLikePattern(const char *s) {
    if (!s || !*s) return false;
    for (; *s; ++s) {
        char c = *s;
        bool ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') ||
                  c == '?' || c == ' ' || c == '\t' || c == '.';
        if (!ok) return false;
    }
    return true;
}

uintptr_t scanPattern(const Module &m, const char *pattern) {
    std::vector<uint8_t> bytes, mask;
    for (const char *p = pattern; *p;) {
        if (*p == ' ' || *p == '\t') { ++p; continue; }
        if (*p == '?' || *p == '.') {
            bytes.push_back(0); mask.push_back(0);
            while (*p == '?' || *p == '.') ++p;
        } else {
            char tok[3] = {0, 0, 0};
            tok[0] = *p++;
            if (*p && *p != ' ' && *p != '\t' && *p != '?' && *p != '.') tok[1] = *p++;
            bytes.push_back(static_cast<uint8_t>(strtoul(tok, nullptr, 16)));
            mask.push_back(1);
        }
    }
    const size_t n = bytes.size();
    if (n == 0) return 0;
    size_t anchor = 0;
    while (anchor < n && !mask[anchor]) ++anchor;
    if (anchor == n) return 0; // todo comodines

#if defined(__aarch64__)
    constexpr size_t kStep = 4; // las instrucciones ARM64 siempre están alineadas a 4
#else
    constexpr size_t kStep = 1;
#endif
    for (const ExecSeg &seg : m.exec) {
        if (seg.size < n) continue;
        const uint8_t *begin = reinterpret_cast<const uint8_t *>(seg.start);
        const uint8_t *end = begin + seg.size - n;
        for (const uint8_t *p = begin; p <= end; p += kStep) {
            if (p[anchor] != bytes[anchor]) continue;
            size_t i = 0;
            for (; i < n; ++i)
                if (mask[i] && p[i] != bytes[i]) break;
            if (i == n) return reinterpret_cast<uintptr_t>(p);
        }
    }
    return 0;
}

} // namespace pl::impl
