#include "Backend.h"
#include "pl/Logger.h"
#include "pl/Signature.h"

#include <initializer_list>
#include <mutex>

namespace henshin {
namespace {

pl::log::Logger g_log("henshin_core");

// Una entrada = una dirección del juego que se busca por firmas (la primera que coincida gana).
struct SigEntry {
    const char *name;
    std::initializer_list<const char *> sigs;
    uintptr_t *out;
};

// ---- Direcciones resueltas ---------------------------------------------------------------
// Añade aquí las que vayas necesitando, p. ej.:  uintptr_t g_localPlayerGetter = 0;

// ---- Tabla de firmas ---------------------------------------------------------------------
// VACÍA a propósito: libminecraftpe.so no exporta símbolos de Actor/Player, así que cada
// firma hay que obtenerla con ingeniería inversa de la versión concreta del juego.
// Formato de firma: "AA BB ? ? CC" ('?' = comodín), igual que unpaircore/HookManager.cpp.
//   { "LocalPlayer::getPos", { "? ? ? A9 ? ? ? 91 ..." }, &g_localPlayerGetter },
const SigEntry kSignatures[] = {
    {nullptr, {}, nullptr},
};

constexpr const char *kModule = "libminecraftpe.so";

} // namespace

Backend &backend() {
    static Backend b;
    return b;
}

void installBackend() {
    static std::once_flag once;
    std::call_once(once, [] {
        int resolved = 0, total = 0;
        for (const SigEntry &e : kSignatures) {
            if (!e.name) break;
            ++total;
            uintptr_t addr = 0;
            for (const char *s : e.sigs) {
                if ((addr = pl::signature::pl_resolve_signature(s, kModule)) != 0) break;
            }
            if (addr) { *e.out = addr; ++resolved; }
            g_log.info("%-32s %s", e.name, addr ? "OK" : "NO ENCONTRADA");
        }
        // Aquí se enlazan las capacidades de Backend cuando sus firmas se hayan resuelto, p. ej.:
        //   if (g_localPlayerGetter) backend().playerState = &readPlayerState;
        if (!backend().ready())
            g_log.warn("henshin_core: sin firmas del juego (%d/%d). Los módulos usan sus fallbacks por input.",
                       resolved, total);
        else
            g_log.info("henshin_core listo (%d/%d firmas).", resolved, total);
    });
}

} // namespace henshin
