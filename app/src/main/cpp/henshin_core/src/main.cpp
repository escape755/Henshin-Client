// libhenshin_core.so
//
// Native side of Henshin's GameBridge (read-only game state) plus a few client-side visual patches
// ported from BedrockTools. Built on Henshin's own Preloader C API (pl/Hook.h, pl/Signature.h),
// the same one unpaircore uses.
//
// What is exported:
//   * NativeGameBridge natives: nReady, nPlayerState, nEntities, nHunger, nSelectedSlot ... The
//     entries that BedrockTools has no offsets for (attack, motion, hitbox, knockback, blocks,
//     desync, item ESP) are exported as safe stubs that return false / -1, so the Java side falls
//     back to its input based behaviour instead of crashing with UnsatisfiedLinkError.
//   * NativeFx natives: Fullbright, Time Changer, Weather Changer.
#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>

#include <atomic>
#include <cmath>
#include <cstring>
#include <mutex>
#include <vector>

#include "pl/Hook.h"
#include "pl/Signature.h"

#include "Rtti.h"
#include "Sdk.h"
#include "Signatures.h"

#include <sys/stat.h>
#include <cstdio>
#include <string>

#define LOG_TAG "HenshinCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

using namespace henshin;

namespace {

constexpr const char* kGameLib = "libminecraftpe.so";

// ------------------------------------------------------------------ hook helpers

template <class Fn>
bool hookSig(const char* pattern, void* detour, Fn** original, const char* name) {
    auto addr = pl::signature::pl_resolve_signature(pattern, kGameLib);
    if (!addr) { LOGW("signature not found: %s", name); return false; }
    int rc = pl::hook::pl_hook(reinterpret_cast<void*>(addr), detour,
                               reinterpret_cast<void**>(original), pl::hook::Priority(200));
    if (rc != 0) { LOGW("hook failed: %s", name); return false; }
    return true;
}

uintptr_t resolve(const char* pattern) { return pl::signature::pl_resolve_signature(pattern, kGameLib); }

// ------------------------------------------------------------------ shared state

std::mutex g_installMutex;
std::atomic_bool g_coreInstalled{false};

// Snapshot produced on the game thread (inside the tick hook), consumed by JNI callers.
struct Snapshot {
    bool valid = false;
    sdk::Vec3 pos{};
    sdk::Vec2 rot{};
    std::vector<float> entities;   // 8 floats each: id, type, x, y, z, dist, health, yawToEntity
};
std::mutex g_snapMutex;
Snapshot g_snap;
std::atomic<float> g_entityRange{0.f};
std::atomic<int> g_entityTicksLeft{0};

using TickFn = void (*)(void*);
using ActorListFn = std::vector<void*> (*)(void*);
using IsPlayerFn = bool (*)(void*);
using ClientUpdateFn = void* (*)(void*, bool);

TickFn g_tickOrig = nullptr;
ClientUpdateFn g_clientUpdateOrig = nullptr;
ActorListFn g_actorList = nullptr;
IsPlayerFn g_isPlayer = nullptr;

// Visual features
std::atomic_bool g_fullbright{false};
std::atomic_bool g_timeOn{false};
std::atomic_int g_timeValue{18000};
std::atomic_bool g_weatherOn{false};
std::atomic_int g_weatherMode{0};   // 0 clear, 1 rain, 2 thunder, 3 snow
std::atomic<void*> g_lastWeather{nullptr};

// ------------------------------------------------------------------ tick hook

void buildSnapshot(void* player) {
    Snapshot s;
    s.valid = sdk::readPosition(player, s.pos) && sdk::readRotation(player, s.rot);

    const float range = g_entityRange.load();
    if (s.valid && range > 0.f && g_entityTicksLeft.load() > 0 && g_actorList) {
        g_entityTicksLeft.fetch_sub(1);
        void* level = sdk::field<void*>(player, sdk::off::ActorLevel);
        void* manager = level ? sdk::field<void*>(level, sdk::off::LevelActorManager) : nullptr;
        if (manager) {
            std::vector<void*> actors = g_actorList(manager);
            float index = 0.f;
            for (void* a : actors) {
                if (!a || a == player) continue;
                sdk::Vec3 p;
                if (!sdk::readPosition(a, p)) continue;
                float dx = p.x - s.pos.x, dy = p.y - s.pos.y, dz = p.z - s.pos.z;
                float dist = std::sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > range) continue;
                float type;
                if (g_isPlayer && g_isPlayer(a)) type = 0.f;                       // TYPE_PLAYER
                else if (sdk::field<std::uint32_t>(a, sdk::off::ActorCategories) & 2u) type = 1.f; // TYPE_MOB
                else continue;
                float yawTo = std::atan2(-dx, dz) * 57.2957795f;
                // No runtime id offset in the SDK: id is just a per-snapshot index.
                s.entities.insert(s.entities.end(), {index++, type, p.x, p.y, p.z, dist, -1.f, yawTo});
            }
        }
    }
    std::lock_guard<std::mutex> lock(g_snapMutex);
    g_snap = std::move(s);
}

void applyWeather(void* weather) {
    if (!weather) return;
    g_lastWeather.store(weather);
    if (!g_weatherOn.load()) return;
    int mode = g_weatherMode.load();
    float rain = mode == 0 ? 0.f : 1.f;
    float lightning = mode == 2 ? 1.f : 0.f;
    sdk::field<float>(weather, sdk::off::WeatherOldRain) = rain;
    sdk::field<float>(weather, sdk::off::WeatherRain) = rain;
    sdk::field<float>(weather, sdk::off::WeatherTargetRain) = rain;
    sdk::field<float>(weather, sdk::off::WeatherOldLightning) = lightning;
    sdk::field<float>(weather, sdk::off::WeatherLightning) = lightning;
    sdk::field<float>(weather, sdk::off::WeatherTargetLightning) = lightning;
}

void tickDetour(void* actor) {
    if (actor) {
        buildSnapshot(actor);
        if (g_weatherOn.load()) {
            void* dim = sdk::field<void*>(actor, sdk::off::ActorDimension);
            if (dim) applyWeather(sdk::field<void*>(dim, sdk::off::DimensionWeather));
        }
    }
    if (g_tickOrig) g_tickOrig(actor);
}

void* clientUpdateDetour(void* client, bool v) {
    return g_clientUpdateOrig ? g_clientUpdateOrig(client, v) : nullptr;
}

// ------------------------------------------------------------------ fullbright

constexpr std::size_t kFullbrightPatchSize = 12;
void* g_fullbrightTarget = nullptr;
std::uint8_t g_fullbrightOriginal[kFullbrightPatchSize];
bool g_fullbrightPatched = false;

void setFullbright(bool on) {
    g_fullbright = on;
    if (!g_fullbrightTarget) {
        auto addr = resolve(sig::Fullbright);
        if (!addr) { LOGW("Fullbright signature not found"); return; }
        g_fullbrightTarget = reinterpret_cast<void*>(addr);
    }
    if (on && !g_fullbrightPatched) {
        // fmov s0, #<max light> ; ret   (same patch BedrockTools uses)
        static const std::uint8_t patch[kFullbrightPatchSize] = {
            0x40, 0x8F, 0xA8, 0x52, 0x00, 0x00, 0x27, 0x1E, 0xC0, 0x03, 0x5F, 0xD6};
        std::memcpy(g_fullbrightOriginal, g_fullbrightTarget, kFullbrightPatchSize);
        g_fullbrightPatched = sdk::patchMemory(g_fullbrightTarget, patch, kFullbrightPatchSize);
    } else if (!on && g_fullbrightPatched) {
        if (sdk::patchMemory(g_fullbrightTarget, g_fullbrightOriginal, kFullbrightPatchSize))
            g_fullbrightPatched = false;
    }
}

// ------------------------------------------------------------------ time changer

int (*g_getTimeOrig)(void*) = nullptr;
void (*g_setTimeOrig)(void*, int) = nullptr;
bool g_timeHooked = false;

int getTimeDetour(void* level) {
    if (g_timeOn.load()) return g_timeValue.load();
    return g_getTimeOrig ? g_getTimeOrig(level) : 0;
}
void setTimeDetour(void* level, int t) {
    if (g_setTimeOrig) g_setTimeOrig(level, g_timeOn.load() ? g_timeValue.load() : t);
}

void ensureTimeHooks() {
    if (g_timeHooked) return;
    bool a = hookSig(sig::Time, reinterpret_cast<void*>(getTimeDetour), &g_getTimeOrig, "Time");
    bool b = hookSig(sig::SetTime, reinterpret_cast<void*>(setTimeDetour), &g_setTimeOrig, "SetTime");
    g_timeHooked = a && b;
}

// ------------------------------------------------------------------ weather changer

void (*g_dimTickOrig)(void*) = nullptr;
void (*g_weatherTickOrig)(void*) = nullptr;
float (*g_rainOrig)(void*, float) = nullptr;
float (*g_lightningOrig)(void*, float) = nullptr;
bool (*g_isRainingOrig)(void*) = nullptr;
bool (*g_isLightningOrig)(void*) = nullptr;
float (*g_biomeTempOrig)(void*, void*, void*) = nullptr;
bool g_weatherHooked = false;

float rainLevel() { return g_weatherMode.load() == 0 ? 0.f : 1.f; }
float lightningLevel() { return g_weatherMode.load() == 2 ? 1.f : 0.f; }

void dimTickDetour(void* dim) {
    if (dim) applyWeather(sdk::field<void*>(dim, sdk::off::DimensionWeather));
    if (g_dimTickOrig) g_dimTickOrig(dim);
    if (dim) applyWeather(sdk::field<void*>(dim, sdk::off::DimensionWeather));
}
void weatherTickDetour(void* w) {
    applyWeather(w);
    if (g_weatherTickOrig) g_weatherTickOrig(w);
    applyWeather(w);
}
float rainDetour(void* w, float d) {
    if (g_weatherOn.load()) { applyWeather(w); return rainLevel(); }
    return g_rainOrig ? g_rainOrig(w, d) : 0.f;
}
float lightningDetour(void* w, float d) {
    if (g_weatherOn.load()) { applyWeather(w); return lightningLevel(); }
    return g_lightningOrig ? g_lightningOrig(w, d) : 0.f;
}
bool isRainingDetour(void* w) {
    if (g_weatherOn.load()) { applyWeather(w); return rainLevel() > 0.2f; }
    return g_isRainingOrig ? g_isRainingOrig(w) : false;
}
bool isLightningDetour(void* w) {
    if (g_weatherOn.load()) { applyWeather(w); return lightningLevel() > 0.2f; }
    return g_isLightningOrig ? g_isLightningOrig(w) : false;
}
float biomeTempDetour(void* biome, void* region, void* pos) {
    if (g_weatherOn.load() && g_weatherMode.load() == 3) return 0.f;   // snow
    return g_biomeTempOrig ? g_biomeTempOrig(biome, region, pos) : 0.5f;
}

void ensureWeatherHooks() {
    if (g_weatherHooked) return;
    bool any = false;
    any |= hookSig(sig::DimensionTick, reinterpret_cast<void*>(dimTickDetour), &g_dimTickOrig, "DimensionTick");
    any |= hookSig(sig::WeatherTick, reinterpret_cast<void*>(weatherTickDetour), &g_weatherTickOrig, "WeatherTick");
    any |= hookSig(sig::WeatherGetRainLevel, reinterpret_cast<void*>(rainDetour), &g_rainOrig, "WeatherGetRainLevel");
    any |= hookSig(sig::WeatherGetLightningLevel, reinterpret_cast<void*>(lightningDetour), &g_lightningOrig, "WeatherGetLightningLevel");
    any |= hookSig(sig::WeatherIsRaining, reinterpret_cast<void*>(isRainingDetour), &g_isRainingOrig, "WeatherIsRaining");
    any |= hookSig(sig::WeatherIsLightning, reinterpret_cast<void*>(isLightningDetour), &g_isLightningOrig, "WeatherIsLightning");
    hookSig(sig::BiomeGetTemperature, reinterpret_cast<void*>(biomeTempDetour), &g_biomeTempOrig, "BiomeGetTemperature");
    g_weatherHooked = any;
}

// ------------------------------------------------------------------ install

bool gameLoaded() {
    void* h = dlopen(kGameLib, RTLD_NOW | RTLD_NOLOAD);
    if (!h) return false;
    dlclose(h);
    return true;
}

/** Installs the tick hook the first time the game library is available. Safe to call repeatedly. */
bool ensureCore() {
    if (g_coreInstalled.load(std::memory_order_acquire)) return true;
    std::lock_guard<std::mutex> lock(g_installMutex);
    if (g_coreInstalled.load()) return true;
    if (!gameLoaded()) return false;
    g_actorList = reinterpret_cast<ActorListFn>(resolve(sig::ActorManagerList));
    g_isPlayer = reinterpret_cast<IsPlayerFn>(resolve(sig::ActorIsPlayer));
    bool ok = hookSig(sig::NormalTick, reinterpret_cast<void*>(tickDetour), &g_tickOrig, "NormalTick");
    hookSig(sig::ClientInstanceUpdate, reinterpret_cast<void*>(clientUpdateDetour), &g_clientUpdateOrig, "ClientInstanceUpdate");
    if (ok) LOGI("henshin_core hooks installed");
    g_coreInstalled.store(ok, std::memory_order_release);
    return ok;
}

bool ready() {
    if (!ensureCore()) return false;
    std::lock_guard<std::mutex> lock(g_snapMutex);
    return g_snap.valid;
}

// ------------------------------------------------------------------ RTTI probe

std::mutex g_probeMutex;
std::atomic_bool g_probeDone{false};

void logLines(const std::string& text) {
    std::size_t pos = 0;
    while (pos < text.size()) {
        std::size_t nl = text.find('\n', pos);
        if (nl == std::string::npos) nl = text.size();
        __android_log_print(ANDROID_LOG_INFO, "HenshinRtti", "%.*s", static_cast<int>(nl - pos), text.c_str() + pos);
        pos = nl + 1;
    }
}

bool writeReport(const std::string& text) {
    ::mkdir("/storage/emulated/0/games", 0777);
    ::mkdir("/storage/emulated/0/games/henshin_app", 0777);
    FILE* f = std::fopen("/storage/emulated/0/games/henshin_app/rtti_probe.txt", "w");
    if (!f) return false;
    std::fwrite(text.data(), 1, text.size(), f);
    std::fclose(f);
    return true;
}

/**
 * Finds the vtables of the main game classes through RTTI and writes where they are (as RVAs) to
 * logcat (tag HenshinRtti) and to games/henshin_app/rtti_probe.txt. Runs once, when the game
 * library is loaded. Hooks nothing: it only reads memory.
 */
bool runProbe() {
    if (g_probeDone.load()) return true;
    if (!gameLoaded()) return false;
    std::lock_guard<std::mutex> lock(g_probeMutex);
    if (g_probeDone.load()) return true;
    rtti::Image img;
    if (!rtti::loadImage(kGameLib, img)) return false;
    const std::string report = rtti::buildReport(
        img, {"11LocalPlayer", "6Player", "3Mob", "5Actor", "8GameMode", "14ClientInstance"}, 300);
    logLines(report);
    if (!writeReport(report)) LOGW("could not write rtti_probe.txt (storage permission?)");
    g_probeDone.store(true);
    return true;
}

}  // namespace

// ====================================================================== JNI: NativeGameBridge

#define BRIDGE(ret, name, ...) \
    extern "C" JNIEXPORT ret JNICALL Java_com_origin_launcher_Launcher_henshin_bridge_NativeGameBridge_##name(JNIEnv* env, jclass, ##__VA_ARGS__)

BRIDGE(jboolean, nReady) { return ready() ? JNI_TRUE : JNI_FALSE; }

// Runs the RTTI probe once the game library is loaded. @return true when the report was produced.
BRIDGE(jboolean, nProbe) { return runProbe() ? JNI_TRUE : JNI_FALSE; }

// What really works natively: bit 0 = attack(), bit 1 = setMotion() / collidingHorizontally().
// Nothing is implemented for this game version yet, so the Java modules use their input fallbacks.
BRIDGE(jint, nCaps) { return 0; }

BRIDGE(jfloatArray, nPlayerState) {
    Snapshot copy;
    { std::lock_guard<std::mutex> lock(g_snapMutex); copy.valid = g_snap.valid; copy.pos = g_snap.pos; copy.rot = g_snap.rot; }
    if (!copy.valid) return nullptr;
    jfloat v[5] = {copy.pos.x, copy.pos.y, copy.pos.z, copy.rot.y /*yaw*/, copy.rot.x /*pitch*/};
    jfloatArray arr = env->NewFloatArray(5);
    env->SetFloatArrayRegion(arr, 0, 5, v);
    return arr;
}

BRIDGE(jfloatArray, nEntities, jfloat range) {
    g_entityRange.store(range);
    g_entityTicksLeft.store(40);   // keep scanning for ~2 s after the last request
    std::vector<float> copy;
    { std::lock_guard<std::mutex> lock(g_snapMutex); copy = g_snap.entities; }
    jfloatArray arr = env->NewFloatArray(static_cast<jsize>(copy.size()));
    if (!copy.empty()) env->SetFloatArrayRegion(arr, 0, static_cast<jsize>(copy.size()), copy.data());
    return arr;
}

// Not available from BedrockTools' offsets: stubs keep the Java fallbacks working.
BRIDGE(jboolean, nAttack, jlong) { return JNI_FALSE; }
BRIDGE(jboolean, nWallCollide) { return JNI_FALSE; }
BRIDGE(void, nSetMotion, jfloat, jfloat, jfloat) {}
BRIDGE(jint, nHunger) { return -1; }
BRIDGE(jboolean, nSelectSlot, jint) { return JNI_FALSE; }
BRIDGE(jboolean, nUseItem, jboolean) { return JNI_FALSE; }
BRIDGE(jboolean, nItemEsp, jboolean, jint, jfloat, jboolean, jint, jstring) { return JNI_FALSE; }
BRIDGE(jboolean, nDesync, jboolean, jint, jint) { return JNI_FALSE; }
BRIDGE(jboolean, nSetRotation, jfloat, jfloat) { return JNI_FALSE; }
BRIDGE(jboolean, nSwing) { return JNI_FALSE; }
BRIDGE(jboolean, nHitbox, jboolean, jfloat, jfloat, jboolean) { return JNI_FALSE; }
BRIDGE(jboolean, nKnockback, jboolean, jfloat, jfloat) { return JNI_FALSE; }
BRIDGE(jboolean, nCanPlace, jint, jint, jint) { return JNI_FALSE; }
BRIDGE(jboolean, nPlace, jint, jint, jint) { return JNI_FALSE; }
BRIDGE(jint, nFindHotbar, jint) { return -1; }
BRIDGE(jint, nSelectedSlot) { return -1; }

// ====================================================================== JNI: NativeFx

#define FX(ret, name, ...) \
    extern "C" JNIEXPORT ret JNICALL Java_com_origin_launcher_Launcher_henshin_bridge_NativeFx_##name(JNIEnv*, jclass, ##__VA_ARGS__)

FX(jboolean, nAvailable) { return ensureCore() ? JNI_TRUE : JNI_FALSE; }

FX(jboolean, nFullbright, jboolean on) {
    if (!gameLoaded()) return JNI_FALSE;
    setFullbright(on);
    return (!on || g_fullbrightPatched) ? JNI_TRUE : JNI_FALSE;
}

FX(jboolean, nTime, jboolean on, jint time) {
    if (!gameLoaded()) return JNI_FALSE;
    g_timeValue = time;
    g_timeOn = on;
    ensureTimeHooks();
    return g_timeHooked ? JNI_TRUE : JNI_FALSE;
}

FX(jboolean, nWeather, jboolean on, jint mode) {
    if (!gameLoaded()) return JNI_FALSE;
    g_weatherMode = mode;
    g_weatherOn = on;
    ensureCore();
    ensureWeatherHooks();
    if (on) applyWeather(g_lastWeather.load());
    return g_weatherHooked ? JNI_TRUE : JNI_FALSE;
}
