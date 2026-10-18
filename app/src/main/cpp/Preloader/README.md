# Preloader (implementación propia)

Reemplaza el submódulo `Viablecobra/Preloader`, que no venía en el zip. Es una reescritura
mínima con la misma API que usa Henshin; produce `libpreloader.so`
(`MinecraftActivity` hace `System.loadLibrary("preloader")`).

| Header | Qué ofrece | Quién lo usa |
|---|---|---|
| `pl/Logger.h` | `pl::log::Logger` → logcat | unpaircore |
| `pl/Signature.h` | `pl_resolve_signature(patrón o símbolo, módulo)` | unpaircore |
| `pl/Hook.h` | `pl_hook` / `pl_unhook` (inline ARM64, cadena de hooks) | unpaircore |
| `pl/Gloss.h` | `GlossInit/Open/Symbol/Hook/HookDelete` | HenshinMods, MotionBlur |

## Cómo funciona
* **Firmas:** si el texto es solo hex/`?` se escanea el segmento ejecutable del módulo
  (paso de 4 bytes en arm64); si no, se busca como símbolo exportado (DT_GNU_HASH / DT_HASH).
* **Hooks:** se sobrescriben 16 bytes (`LDR X17,#8; BR X17; .quad detour`). El trampolín copia
  las 4 instrucciones originales reubicando ADR/ADRP, LDR literal, B/BL, B.cond, CBZ/CBNZ y
  TBZ/TBNZ (`impl/A64Relocator.h`). Varios hooks sobre el mismo destino se encadenan; `orig`
  del nuevo hook apunta al anterior.
* No suspende hilos mientras parchea y la prioridad solo se guarda, no ordena la cadena.

## Qué se probó (en x86, sin dispositivo)
* Compila sin avisos con `-Wall -Wextra` (C++20).
* `tests/reloc_test.cpp`: las 15 formas de instrucción dan la reubicación esperada.
* `tests/mod_test.cpp`: búsqueda de símbolos y de patrones contra `libc` del host.
* `tests/elf_test.cpp`: 3000 símbolos de `libminecraftpe.so` y el de `MyHook1` en
  `libmaesdk.so` coinciden con `nm`.
* Las firmas de `HookManager.cpp` (timer ×2, logo ×1) dan **1 coincidencia exacta** en el
  `libminecraftpe.so` que se subió; la 2ª firma del logo no aparece (es solo respaldo).

## Sin probar
No hay NDK ni dispositivo aquí: **no se compiló para Android ni se ejecutó un hook real**.
Un fallo del reubicador cierra el juego, así que prueba primero con `adb logcat`.

## libminecraftpe.so subido
ELF aarch64, strip, 313 MB, cadenas de versión 1.21.x (hasta 1.21.120). Exporta ~88 000
símbolos, pero ninguno de las clases del juego (`Actor`, `Player`, `LocalPlayer`…), así que
un `libhenshin_core.so` necesita firmas y offsets propios.
