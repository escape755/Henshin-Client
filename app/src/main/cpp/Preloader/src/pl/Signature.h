#pragma once
#include <cstdint>

namespace pl::signature {

// identifier: patrón de bytes ("? ? ? 96 ? ? ? A9", '?' = comodín) o nombre de símbolo
// exportado ("_ZN9Microsoft...").  module: "libminecraftpe.so".  Devuelve 0 si no se encuentra.
uintptr_t pl_resolve_signature(const char *identifier, const char *moduleName);

} // namespace pl::signature
