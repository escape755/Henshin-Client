#pragma once
// Capa entre el JNI (Bridge.cpp) y el juego. Cada capacidad es un puntero a función que
// queda en nullptr hasta que Backend.cpp resuelve las firmas necesarias en libminecraftpe.so.
// Mientras sea nullptr, el JNI devuelve el valor "no soportado" y Java usa sus fallbacks.
#include <cstdint>
#include <string>
#include <vector>

namespace henshin {

struct Backend {
    // x, y, z, yaw, pitch
    bool (*playerState)(float out[5]) = nullptr;
    // 8 floats por entidad: id, type(0 jugador,1 mob,2 item), x, y, z, distancia, vida, yawHaciaEntidad
    bool (*entities)(float range, std::vector<float> &out) = nullptr;
    bool (*attack)(int64_t id) = nullptr;
    bool (*wallCollide)() = nullptr;
    bool (*setMotion)(float x, float y, float z) = nullptr;
    int  (*hunger)() = nullptr;
    bool (*selectSlot)(int slot) = nullptr;
    bool (*useItem)(bool hold) = nullptr;
    bool (*itemEsp)(bool on, uint32_t argb, float range, bool tracers, int style, const std::string &filter) = nullptr;
    bool (*desync)(bool on, int mode, int delayMs) = nullptr;
    bool (*setRotation)(float yaw, float pitch) = nullptr;
    bool (*swing)() = nullptr;
    bool (*hitbox)(bool on, float w, float h, bool mobs) = nullptr;
    bool (*knockback)(bool on, float hPct, float vPct) = nullptr;
    bool (*canPlace)(int x, int y, int z) = nullptr;
    bool (*place)(int x, int y, int z) = nullptr;
    int  (*findHotbar)(int itemId) = nullptr;
    int  (*selectedSlot)() = nullptr;

    // Listo = se puede leer el estado del jugador. Sin esto Java sigue con sus fallbacks.
    bool ready() const { return playerState != nullptr; }
};

Backend &backend();
// Resuelve firmas y rellena backend(). Idempotente.
void installBackend();

} // namespace henshin
