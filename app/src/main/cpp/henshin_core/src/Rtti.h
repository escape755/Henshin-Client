// Version independent discovery of C++ vtables through RTTI.
//
// libminecraftpe.so is stripped (no symbols for the game classes) but it keeps the RTTI strings
// ("11LocalPlayer", "8GameMode", ...). From a type name string we can walk
//   name string  <-  type_info object (ptr at +8)  <-  vtable[-1]
// and get the vtable of any polymorphic class at runtime, whatever the game version is.
// Used by the probe (report.txt) and as the base for vtable hooks.
#pragma once

#include <link.h>

#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <vector>

namespace henshin::rtti {

struct Range { std::uintptr_t begin = 0, end = 0; };

struct Image {
    std::uintptr_t base = 0;
    std::vector<Range> data;   // readable, not executable (rodata, data.rel.ro, ...)
    std::vector<Range> rw;     // writable at load time (type_info objects + vtables live here)
    std::vector<Range> code;   // executable
};

struct VtableInfo {
    std::uintptr_t nameAddr = 0;   // address of the mangled name string
    std::uintptr_t typeinfo = 0;
    std::uintptr_t vtable = 0;     // address of slot 0 (just after the type_info pointer)
    int slots = 0;
};

inline bool inRanges(const std::vector<Range>& r, std::uintptr_t a) {
    for (const Range& x : r) if (a >= x.begin && a < x.end) return true;
    return false;
}

/** Finds a loaded library whose path contains {@code libName} and records its segments. */
inline bool loadImage(const char* libName, Image& img) {
    struct Ctx { const char* name; Image* img; bool found; } ctx{libName, &img, false};
    dl_iterate_phdr(
        [](dl_phdr_info* info, std::size_t, void* user) -> int {
            Ctx* c = static_cast<Ctx*>(user);
            if (!info->dlpi_name || !std::strstr(info->dlpi_name, c->name)) return 0;
            c->img->base = info->dlpi_addr;
            for (int i = 0; i < info->dlpi_phnum; i++) {
                const ElfW(Phdr)& ph = info->dlpi_phdr[i];
                if (ph.p_type != PT_LOAD) continue;
                Range r;
                r.begin = info->dlpi_addr + ph.p_vaddr;
                r.end = r.begin + ph.p_memsz;
                if (ph.p_flags & PF_X) c->img->code.push_back(r);
                else if (ph.p_flags & PF_R) c->img->data.push_back(r);
                if (ph.p_flags & PF_W) c->img->rw.push_back(r);
            }
            c->found = true;
            return 1;
        },
        &ctx);
    return ctx.found && !img.code.empty();
}

inline const char* findBytes(const char* hay, std::size_t n, const char* needle, std::size_t m) {
    if (m == 0 || n < m) return nullptr;
    const char* last = hay + (n - m);
    const char* p = hay;
    while (p <= last) {
        p = static_cast<const char*>(std::memchr(p, needle[0], static_cast<std::size_t>(last - p) + 1));
        if (!p) return nullptr;
        if (std::memcmp(p, needle, m) == 0) return p;
        ++p;
    }
    return nullptr;
}

/** Address of the standalone string {@code name} (NUL before and after), or 0. */
inline std::uintptr_t findTypeName(const Image& img, const std::string& name) {
    const std::size_t m = name.size() + 1;   // include the terminating NUL
    for (const Range& r : img.data) {
        const char* p = reinterpret_cast<const char*>(r.begin);
        std::size_t left = r.end - r.begin;
        while (left >= m) {
            const char* hit = findBytes(p, left, name.c_str(), m);
            if (!hit) break;
            const bool startsString = (hit == reinterpret_cast<const char*>(r.begin)) || hit[-1] == '\0';
            if (startsString) return reinterpret_cast<std::uintptr_t>(hit);
            left -= static_cast<std::size_t>(hit - p) + 1;
            p = hit + 1;
        }
    }
    return 0;
}

inline std::vector<std::uintptr_t> findWords(const Image& img, std::uintptr_t value, std::size_t max = 32) {
    std::vector<std::uintptr_t> out;
    for (const Range& r : img.rw) {
        std::uintptr_t a = (r.begin + 7) & ~static_cast<std::uintptr_t>(7);
        for (; a + 8 <= r.end; a += 8) {
            if (*reinterpret_cast<const std::uintptr_t*>(a) == value) {
                out.push_back(a);
                if (out.size() >= max) return out;
            }
        }
    }
    return out;
}

/** Slots = consecutive non-zero entries that are not pointers into the image's data. */
inline int countSlots(const Image& img, std::uintptr_t start) {
    const Range* seg = nullptr;
    for (const Range& r : img.rw) if (start >= r.begin && start < r.end) { seg = &r; break; }
    if (!seg) return 0;
    int n = 0;
    while (start + 8u * (n + 1) <= seg->end) {
        std::uintptr_t v = *reinterpret_cast<const std::uintptr_t*>(start + 8u * n);
        if (v == 0 || inRanges(img.data, v)) break;
        ++n;
    }
    return n;
}

/** {@code mangled} is the Itanium type name without the _ZTS prefix, e.g. "11LocalPlayer". */
inline VtableInfo findVtable(const Image& img, const std::string& mangled) {
    VtableInfo best;
    const std::uintptr_t name = findTypeName(img, mangled);
    if (!name) return best;
    best.nameAddr = name;
    for (std::uintptr_t loc : findWords(img, name)) {
        const std::uintptr_t ti = loc - 8;
        for (std::uintptr_t ref : findWords(img, ti)) {
            const std::uintptr_t start = ref + 8;
            const int n = countSlots(img, start);
            if (n > best.slots) { best.typeinfo = ti; best.vtable = start; best.slots = n; }
        }
    }
    return best;
}

inline std::string hex(std::uintptr_t v) {
    char buf[32];
    std::snprintf(buf, sizeof buf, "0x%llx", static_cast<unsigned long long>(v));
    return buf;
}

/** Text report: where each class' vtable is (as RVA, comparable with the static binary). */
inline std::string buildReport(const Image& img, const std::vector<std::string>& classes, int maxSlots) {
    std::string out;
    out += "base=" + hex(img.base) + "\n";
    for (const std::string& c : classes) {
        VtableInfo v = findVtable(img, c);
        out += "\n[" + c + "] ";
        if (!v.nameAddr) { out += "type name not found\n"; continue; }
        if (!v.vtable) { out += "name rva=" + hex(v.nameAddr - img.base) + " but no vtable found\n"; continue; }
        out += "typeinfo rva=" + hex(v.typeinfo - img.base) + " vtable rva=" + hex(v.vtable - img.base)
             + " slots=" + std::to_string(v.slots) + "\n";
        const int n = v.slots < maxSlots ? v.slots : maxSlots;
        for (int i = 0; i < n; i++) {
            std::uintptr_t f = *reinterpret_cast<const std::uintptr_t*>(v.vtable + 8u * i);
            out += std::to_string(i) + ": " + (inRanges(img.code, f) ? hex(f - img.base) : std::string("ext")) + "\n";
        }
    }
    return out;
}

}  // namespace henshin::rtti
