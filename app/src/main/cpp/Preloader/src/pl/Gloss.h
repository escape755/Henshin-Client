#pragma once
// API estilo Gloss usada por HenshinMods y MotionBlur (GlossInit/Open/Symbol/Hook).
#include <cstddef>
#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif

typedef void *GHandle;
typedef void *GHook;

void      GlossInit(bool enableLog);
GHandle   GlossOpen(const char *libName);
void      GlossClose(GHandle handle, bool unload);
uintptr_t GlossSymbol(GHandle handle, const char *name, size_t *symSize);
GHook     GlossHook(void *target, void *detour, void **original);
bool      GlossHookDelete(GHook hook);

#ifdef __cplusplus
}
#endif
