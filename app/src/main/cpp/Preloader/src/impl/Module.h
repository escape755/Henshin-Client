#pragma once
#include <cstdint>
#include <string>
#include <vector>

namespace pl::impl {

struct ExecSeg { uintptr_t start; size_t size; };

struct Module {
    std::string path;
    uintptr_t bias = 0;      // load bias
    uintptr_t dynamic = 0;   // dirección de PT_DYNAMIC en memoria (0 si no hay)
    std::vector<ExecSeg> exec; // segmentos legibles y ejecutables
};

// Busca un módulo ya cargado por nombre (basename o ruta completa).
bool findModule(const char *name, Module &out);
// Símbolo exportado (.dynsym) vía DT_GNU_HASH / DT_HASH. 0 si no existe. size es opcional.
uintptr_t lookupSymbol(const Module &m, const char *name, size_t *size = nullptr);
// Busca un patrón "AA BB ? CC" en los segmentos ejecutables. 0 si no hay coincidencia.
uintptr_t scanPattern(const Module &m, const char *pattern);
// true si el texto parece un patrón de bytes (solo hex, espacios y '?').
bool looksLikePattern(const char *s);

} // namespace pl::impl
