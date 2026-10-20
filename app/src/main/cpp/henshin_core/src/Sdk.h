// Minimal Minecraft Bedrock SDK used by henshin_core.
// Layout/offsets come from BedrockTools (MrPokeG, https://github.com/QYCottage/BedrockTools) for
// Bedrock 1.26.5x arm64. They are version specific, like the signatures in Signatures.h.
#pragma once

#include <cstddef>
#include <cstdint>
#include <cstring>
#include <sys/mman.h>
#include <unistd.h>

namespace henshin::sdk {

struct Vec2 { float x, y; };   // rotation: x = pitch, y = yaw
struct Vec3 { float x, y, z; };

namespace off {
inline constexpr std::size_t ActorStateVector = 0x208;   // component ptr, Vec3 position at +0
inline constexpr std::size_t ActorRotation    = 0x218;   // component ptr, Vec2 {pitch, yaw}
inline constexpr std::size_t ActorLevel       = 464;
inline constexpr std::size_t ActorDimension   = 448;
inline constexpr std::size_t ActorCategories  = 512;
inline constexpr std::size_t LevelActorManager = 0x470;
inline constexpr std::size_t DimensionWeather = 0x1B8;
inline constexpr std::size_t WeatherOldRain = 0x34, WeatherRain = 0x38, WeatherTargetRain = 0x3C;
inline constexpr std::size_t WeatherOldLightning = 0x40, WeatherLightning = 0x44, WeatherTargetLightning = 0x48;
}

template <class T>
inline T& field(void* object, std::size_t offset) {
    return *reinterpret_cast<T*>(reinterpret_cast<std::uintptr_t>(object) + offset);
}

inline bool readPosition(void* actor, Vec3& out) {
    void* comp = field<void*>(actor, off::ActorStateVector);
    if (!comp) return false;
    out = field<Vec3>(comp, 0);
    return true;
}

inline bool readRotation(void* actor, Vec2& out) {
    void* comp = field<void*>(actor, off::ActorRotation);
    if (!comp) return false;
    out = field<Vec2>(comp, 0);
    return true;
}

inline bool patchMemory(void* address, const void* data, std::size_t size) {
    const long pageSizeValue = sysconf(_SC_PAGESIZE);
    if (!address || !data || size == 0 || pageSizeValue <= 0) return false;
    const auto page = static_cast<std::uintptr_t>(pageSizeValue);
    const auto start = reinterpret_cast<std::uintptr_t>(address);
    const auto pageStart = start & ~(page - 1);
    const auto pageEnd = (start + size + page - 1) & ~(page - 1);
    if (mprotect(reinterpret_cast<void*>(pageStart), pageEnd - pageStart, PROT_READ | PROT_WRITE | PROT_EXEC) != 0) return false;
    std::memcpy(address, data, size);
    __builtin___clear_cache(static_cast<char*>(address), static_cast<char*>(address) + size);
    mprotect(reinterpret_cast<void*>(pageStart), pageEnd - pageStart, PROT_READ | PROT_EXEC);
    return true;
}

}
