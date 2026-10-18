# Henshin-Launcher: módulos de Abysm sin relay

Esta carpeta es Henshin con los módulos de Abysm que **pueden vivir dentro del launcher**
(sin proxy / relay, sin `:relay`, sin CloudburstMC).

## Añadidos (Java, en `henshin/module/`)
| Módulo | Origen Abysm | Cómo funciona aquí |
|---|---|---|
| Watermark (Render) | WaterMarkModule | TextView sobre el juego: texto, versión, 9 posiciones, tamaño, RGB/Static, fuente |
| AutoWalk (Movement) | AutoWalkModule | Mantiene W con eventos de teclado sintéticos (opción Sprint = Ctrl) |
| AntiAFK (Player) | AntiAFKModule | Salto/agacharse cada N segundos |

Registrados en `ModuleManager`, aparecen en el ClickGUI y guardan su config como el resto.

## Por qué el resto de Abysm no se pudo portar
Los ~50 módulos de Abysm (Fly, Speed, Timer, CrystalAura, Surround, ESP, Minimap...) son
`beforePacketBound(...)`: leen/modifican paquetes Bedrock (`PlayerAuthInputPacket`,
`UpdateAbilitiesPacket`, ...) y necesitan `GameSession` + el proyecto `:relay`.
Sin relay no existe ese flujo de paquetes. Solo funcionarían escribiendo un `libhenshin_core.so`
con hooks a la memoria de `libminecraftpe.so` (offsets/firmas por versión del juego).

## Problema original de los módulos de Henshin
Aura, Fly, Hitbox, Velocity, Surround, Spider, ItemEsp, Desync y el modo Smart de AutoEat
llaman a `libhenshin_core.so` vía `NativeGameBridge`. Esa librería **no se compila** (el CMake
no tiene ningún target `henshin_core`), así que `isAvailable()` es false y solo actúan sus
fallbacks por input. El submódulo `Preloader` también falta en el zip (`git submodule update --init`).

## Sin compilar
No hay SDK/NDK en el entorno donde se preparó esto: el código se revisó solo por sintaxis.
