# Análisis de libminecraftpe.so (arm64) — notas

## Estado real de los módulos con ESTE binario
- Las 14 firmas de `henshin_core/src/Signatures.h` dan **0 coincidencias** en el .so analizado
  (313 198 848 bytes). Por eso `libhenshin_core.so` no instala el hook de tick y el bridge no carga.
- Con el bridge sin cargar, KillAura/Aura usan el fallback de tap al centro (autoclicker).
- Los offsets de `Sdk.h` (0x208, 0x218, ...) también son de otra versión: no usarlos aquí.

## Lo que sí se encontró (RTTI intacto, sin símbolos del juego)
Vtables por RTTI (RVA estático en el .so; el tamaño es una cota mínima):
| Clase | vtable RVA | slots |
|---|---|---|
| LocalPlayer | 0x120821b0 | 246 |
| Player | 0x12382978 | 185 |
| Mob | 0x123279c8 | 179 |
| Actor | 0x12332498 | 139 |
| GameMode | 0x12365670 | 21 |
| ClientInstance | 0x1204d6a8 | 442 |

## Probe (nuevo)
Al abrir el juego, `henshin_core` busca esas vtables en memoria (src/Rtti.h) y escribe un reporte en
`/storage/emulated/0/games/henshin_app/rtti_probe.txt` (y en logcat, tag `HenshinRtti`).
- Si los RVA del reporte coinciden con la tabla de arriba, el juego instalado es exactamente el
  binario analizado y se pueden enganchar slots de vtable.
- No engancha nada: solo lee memoria.

## Cambio de comportamiento (Java)
`GameBridge.caps()` / `canAttack()` / `canMove()`: `isAvailable()` solo dice que se puede LEER el
estado; los stubs nativos devuelven false. KillAura, Aura, Fly y Jetpack ahora usan su fallback por
input mientras `nCaps()` no declare esas capacidades (hoy devuelve 0).

## Pendiente (no hecho)
Identificar por desensamblado qué slot es: Actor tick, GameMode::attack, y cómo mover al jugador.
Sin eso no hay ataque ni movimiento nativos. Probar en el dispositivo antes de enganchar.
