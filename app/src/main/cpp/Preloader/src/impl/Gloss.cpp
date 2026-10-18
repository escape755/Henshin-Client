#include "pl/Gloss.h"
#include "pl/Hook.h"
#include "pl/Logger.h"
#include "Module.h"

#include <dlfcn.h>
#include <map>
#include <mutex>
#include <string>

namespace {

struct Handle {
    std::string name;
    void *dl = nullptr;
    pl::impl::Module mod;
    bool hasMod = false;
};

std::mutex g_mutex;
std::map<void *, void *> g_hooks; // GHook -> detour (el GHook es el target)

} // namespace

extern "C" {

void GlossInit(bool) {}

GHandle GlossOpen(const char *libName) {
    if (!libName) return nullptr;
    auto *h = new Handle;
    h->name = libName;
    h->hasMod = pl::impl::findModule(libName, h->mod);
    h->dl = dlopen(libName, RTLD_NOW | RTLD_NOLOAD);
    if (!h->dl) h->dl = dlopen(libName, RTLD_NOW);
    if (!h->hasMod && !h->dl) { delete h; return nullptr; }
    return h;
}

void GlossClose(GHandle handle, bool) {
    auto *h = static_cast<Handle *>(handle);
    if (!h) return;
    if (h->dl) dlclose(h->dl);
    delete h;
}

uintptr_t GlossSymbol(GHandle handle, const char *name, size_t *symSize) {
    auto *h = static_cast<Handle *>(handle);
    if (!h || !name) return 0;
    if (h->hasMod) {
        if (uintptr_t a = pl::impl::lookupSymbol(h->mod, name, symSize)) return a;
    }
    if (h->dl) return reinterpret_cast<uintptr_t>(dlsym(h->dl, name));
    return 0;
}

GHook GlossHook(void *target, void *detour, void **original) {
    if (pl::hook::pl_hook(target, detour, original, pl::hook::Priority::Normal) != 0) return nullptr;
    std::lock_guard<std::mutex> lock(g_mutex);
    g_hooks[target] = detour;
    return target;
}

bool GlossHookDelete(GHook hook) {
    std::lock_guard<std::mutex> lock(g_mutex);
    auto it = g_hooks.find(hook);
    if (it == g_hooks.end()) return false;
    bool ok = pl::hook::pl_unhook(hook, it->second);
    g_hooks.erase(it);
    return ok;
}

} // extern "C"
