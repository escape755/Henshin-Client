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

## ClickGUI "TBUI" (OpenTBUI)
Se añadió el módulo `:OpenTBUI` (librería de 1503Dev, LGPLv3, ver `OpenTBUI/LICENSE`) y una segunda
ClickGUI con su interfaz tipo Toolbox: `henshin/gui/TbuiGuiWindow.java`.
- Categorías = categorías de Henshin (+ "Settings"); cada módulo es un toggle con sus valores
  (Bool→toggle, Int/Float→slider, Enum→dropdown, Color→selector, Text→campo) y botón de shortcut.
- `StatusManager` de OpenTBUI sincroniza todo por path (`m/<Módulo>/enabled`, `m/<Módulo>/v/<Valor>`),
  así que un módulo activado por keybind, shortcut o comando `.toggle` se refleja en la GUI.
- Se elige en Settings (fila "GUI style": Panel / TBUI); `GuiLauncher` abre la elegida y, si TBUI falla,
  vuelve a Panel.
- Sin compilar: no había SDK/NDK al prepararlo.

## libhenshin_core.so (de BedrockTools)
Nuevo target `app/src/main/cpp/henshin_core` (ya en el CMake raíz). Usa las firmas y offsets de
BedrockTools (MrPokeG, ver `LICENSE.BedrockTools`) sobre el Preloader C de Henshin (`pl/Hook.h`, `pl/Signature.h`).
- `NativeGameBridge`: ahora existe la librería. Funcionan `nReady`, `nPlayerState` (x,y,z,yaw,pitch) y
  `nEntities` (jugadores y mobs cercanos, calculado en el tick del juego). Attack, motion, hitbox,
  knockback, bloques, desync, item ESP, hambre y hotbar NO tienen offsets en BedrockTools: son stubs que
  devuelven false/-1, así que esos módulos siguen usando sus fallbacks por input.
- `NativeFx` + módulos nuevos: Fullbright (Render), Time Changer y Weather Changer (Player).
- Las firmas son de Minecraft 1.26.5x arm64 (`Signatures.h`). Para otra versión hay que regenerarlas.
- El Preloader (Viablecobra/Preloader) ya está en `app/src/main/cpp/Preloader`. `main.cpp` pasa la
  comprobación de sintaxis contra sus cabeceras reales, pero no se compiló con el NDK. El Preloader
  baja nlohmann_json con FetchContent al configurar, así que la primera build necesita internet.
- No portado: los módulos HUD/visuales de BedrockTools que dibujan con el UI del juego (Keystrokes,
  Coords, Tablist...). Dependen de hooks de render (MinecraftUIRenderContext) y de LeviLauncher.

## libmtbinloader2 (Xelo-Mod, Rust)
`app/src/main/rust/mtbinloader2` es el código fuente de la `libmtbinloader2.so` que ya va en `jniLibs`
(ahí viven los toggles Legit: No Fog, Fancy Clouds, No hurt cam, título, crosshair, etc.). Se copió de
Xelo-Mod y se renombró a Henshin (rutas `games/henshin_app/...`, clave `henshin_title`, JNI `HenshinMods`).
- **Bug arreglado:** el .so precompilado lee `games/henshin_app/hnsn_mods/config.json` (clave `hnsn_title`),
  pero `ConfigModule` escribía en `henshin_mods/config.json`. Los toggles Legit no tenían efecto. Ahora
  escribe en las dos rutas.
- **Nuevos Legit** (solo funcionan con el .so recompilado): No Weather, No Stars, No Sun & Moon.
- **Recompilar:** workflow `.github/workflows/build-mtbinloader2.yml` (o `cargo build --profile githubci`
  con target aarch64-linux-android) y copiar el .so a `jniLibs/arm64-v8a/`. No se compiló aquí.
- `src/minecraft_title_5.png` y `Diskksks.png` son los originales de Xelo: cambia el título por el tuyo.

## LeviLaunchroid (revisión)
- LeviLaunchroid es el upstream de Henshin: no trae offsets de combate/velocidad ni el código fuente del Preloader (su carpeta está vacía).
- Aprovechado: `resources/preloader/preloader_signature_rules_source.json` → `kScreenRules[]` en `henshin_core/src/Signatures.h` (firmas de pauseMenu/hudScreen para 1.26.30/40/50, ordenadas de nueva a vieja). Todavía no están enganchadas; sirven de modelo para tablas de firmas por versión.
- Pendiente para que Killaura funcione de verdad: `libminecraftpe.so` (arm64) y la versión exacta del juego.
