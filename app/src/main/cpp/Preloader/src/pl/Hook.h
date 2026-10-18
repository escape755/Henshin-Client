#pragma once

namespace pl::hook {

// Mismos valores que memory::HookPriority (unpaircore/src/Hook.h).
enum class Priority : int { Highest = 0, High = 100, Normal = 200, Low = 300, Lowest = 400 };

// Hook inline ARM64. *originalFunc recibe la función original (o la detour anterior de la cadena).
// Devuelve 0 si tuvo éxito, distinto de 0 si falló.
int pl_hook(void *target, void *detour, void **originalFunc, Priority priority);
bool pl_unhook(void *target, void *detour);

} // namespace pl::hook
