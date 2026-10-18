#include "pl/Hook.h"
#include "pl/Logger.h"
#include "A64Relocator.h"

#include <sys/mman.h>
#include <unistd.h>
#include <cstring>
#include <map>
#include <mutex>
#include <vector>

namespace pl::hook {
namespace {

using namespace pl::impl::a64;

pl::log::Logger g_log("preloader");

struct Link { void *detour; void **origPtr; };

struct Entry {
    uint8_t saved[16];
    void *trampoline = nullptr;
    std::vector<Link> chain; // chain[0] = primer hook; chain.back() = el que está activo
};

std::mutex g_mutex;
std::map<void *, Entry> g_entries;

bool patchCode(void *dst, const void *src, size_t n) {
    const uintptr_t ps = static_cast<uintptr_t>(sysconf(_SC_PAGESIZE));
    const uintptr_t s = reinterpret_cast<uintptr_t>(dst) & ~(ps - 1);
    const uintptr_t e = (reinterpret_cast<uintptr_t>(dst) + n + ps - 1) & ~(ps - 1);
    if (mprotect(reinterpret_cast<void *>(s), e - s, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) return false;
    std::memcpy(dst, src, n);
    __builtin___clear_cache(static_cast<char *>(dst), static_cast<char *>(dst) + n);
    mprotect(reinterpret_cast<void *>(s), e - s, PROT_READ | PROT_EXEC);
    return true;
}

// LDR X17,#8 ; BR X17 ; .quad detour
void makePatch(uint8_t out[16], void *detour) {
    const uint32_t a = kLdrX17_8, b = kBrX17;
    const uint64_t d = reinterpret_cast<uint64_t>(detour);
    std::memcpy(out, &a, 4);
    std::memcpy(out + 4, &b, 4);
    std::memcpy(out + 8, &d, 8);
}

void *makeTrampoline(void *target) {
    void *mem = mmap(nullptr, 4096, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (mem == MAP_FAILED) return nullptr;
    const size_t n = buildTrampoline(reinterpret_cast<const uint32_t *>(target),
                                     reinterpret_cast<uint64_t>(target),
                                     static_cast<uint32_t *>(mem));
    __builtin___clear_cache(static_cast<char *>(mem), static_cast<char *>(mem) + n);
    if (mprotect(mem, 4096, PROT_READ | PROT_EXEC) != 0) { munmap(mem, 4096); return nullptr; }
    return mem;
}

} // namespace

int pl_hook(void *target, void *detour, void **originalFunc, Priority) {
#if !defined(__aarch64__)
    (void)target; (void)detour; (void)originalFunc;
    g_log.error("pl_hook solo está implementado para arm64");
    return -1;
#else
    if (!target || !detour) return -1;
    std::lock_guard<std::mutex> lock(g_mutex);

    auto it = g_entries.find(target);
    if (it == g_entries.end()) {
        Entry e;
        std::memcpy(e.saved, target, 16);
        e.trampoline = makeTrampoline(target);
        if (!e.trampoline) { g_log.error("no se pudo crear el trampolín"); return -2; }
        it = g_entries.emplace(target, std::move(e)).first;
    }
    Entry &e = it->second;

    void *prev = e.chain.empty() ? e.trampoline : e.chain.back().detour;
    uint8_t patch[16];
    makePatch(patch, detour);
    if (!patchCode(target, patch, sizeof(patch))) {
        g_log.error("mprotect falló al parchear %p", target);
        return -3;
    }
    if (originalFunc) *originalFunc = prev;
    e.chain.push_back({detour, originalFunc});
    return 0;
#endif
}

bool pl_unhook(void *target, void *detour) {
#if !defined(__aarch64__)
    (void)target; (void)detour;
    return false;
#else
    std::lock_guard<std::mutex> lock(g_mutex);
    auto it = g_entries.find(target);
    if (it == g_entries.end()) return false;
    Entry &e = it->second;

    size_t idx = e.chain.size();
    for (size_t i = 0; i < e.chain.size(); ++i)
        if (e.chain[i].detour == detour) { idx = i; break; }
    if (idx == e.chain.size()) return false;

    void *prev = idx == 0 ? e.trampoline : e.chain[idx - 1].detour;
    if (idx + 1 < e.chain.size() && e.chain[idx + 1].origPtr) *e.chain[idx + 1].origPtr = prev;
    e.chain.erase(e.chain.begin() + static_cast<long>(idx));

    if (e.chain.empty()) return patchCode(target, e.saved, sizeof(e.saved));
    if (idx == e.chain.size()) { // se quitó el activo: el anterior pasa a ser el activo
        uint8_t patch[16];
        makePatch(patch, e.chain.back().detour);
        return patchCode(target, patch, sizeof(patch));
    }
    return true;
#endif
}

} // namespace pl::hook
