# henshin_core (libhenshin_core.so)

Biblioteca que carga `NativeGameBridge` (`System.loadLibrary("henshin_core")`).

## Estado
* Exporta los 19 métodos JNI que declara `NativeGameBridge.java` (nombres verificados).
* `Backend.cpp` tiene la tabla de firmas **vacía**, por lo que `nReady()` devuelve `false`:
  `isAvailable()` sigue siendo `false` y Aura, Fly, Hitbox, Velocity, Surround, Spider,
  ItemEsp y Desync usan sus fallbacks por input, igual que antes. Lo que cambia es que la
  librería ya existe y carga sin `UnsatisfiedLinkError`.
* Las funciones que leen o modifican el juego (jugador, entidades, ataque, hitbox, knockback…)
  **no están implementadas**: son punteros `nullptr` en `Backend.h`.

## Qué falta para activarlo
1. Encontrar en `libminecraftpe.so` (cada versión del juego cambia) las funciones y offsets
   de LocalPlayer / entidades / GameMode. El `.so` no exporta símbolos de esas clases, así que
   hay que sacarlas por ingeniería inversa y escribir firmas.
2. Añadirlas a `kSignatures` en `Backend.cpp` y enlazar cada capacidad en `installBackend()`.
3. Probar en un dispositivo con `adb logcat -s henshin_core` (cada firma se registra como OK
   o NO ENCONTRADA).

`nReady()` solo pasa a `true` cuando `playerState` está enlazado, así los fallbacks no se
desactivan mientras no haya nada real detrás.
