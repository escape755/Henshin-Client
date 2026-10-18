// JNI de com.origin.launcher.Launcher.henshin.bridge.NativeGameBridge
#include <jni.h>

#include <string>
#include <vector>

#include "Backend.h"

using henshin::backend;

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *, void *) {
    henshin::installBackend();
    return JNI_VERSION_1_6;
}

#define BRIDGE(ret, name) JNIEXPORT ret JNICALL Java_com_origin_launcher_Launcher_henshin_bridge_NativeGameBridge_##name

BRIDGE(jboolean, nReady)(JNIEnv *, jclass) {
    henshin::installBackend();
    return backend().ready() ? JNI_TRUE : JNI_FALSE;
}

BRIDGE(jfloatArray, nPlayerState)(JNIEnv *env, jclass) {
    float s[5];
    if (!backend().playerState || !backend().playerState(s)) return nullptr;
    jfloatArray arr = env->NewFloatArray(5);
    if (arr) env->SetFloatArrayRegion(arr, 0, 5, s);
    return arr;
}

BRIDGE(jfloatArray, nEntities)(JNIEnv *env, jclass, jfloat range) {
    std::vector<float> v;
    if (backend().entities) backend().entities(range, v);
    jfloatArray arr = env->NewFloatArray(static_cast<jsize>(v.size()));
    if (arr && !v.empty()) env->SetFloatArrayRegion(arr, 0, static_cast<jsize>(v.size()), v.data());
    return arr;
}

BRIDGE(jboolean, nAttack)(JNIEnv *, jclass, jlong id) {
    return backend().attack && backend().attack(id);
}
BRIDGE(jboolean, nWallCollide)(JNIEnv *, jclass) {
    return backend().wallCollide && backend().wallCollide();
}
BRIDGE(void, nSetMotion)(JNIEnv *, jclass, jfloat x, jfloat y, jfloat z) {
    if (backend().setMotion) backend().setMotion(x, y, z);
}
BRIDGE(jint, nHunger)(JNIEnv *, jclass) {
    return backend().hunger ? backend().hunger() : -1;
}
BRIDGE(jboolean, nSelectSlot)(JNIEnv *, jclass, jint slot) {
    return backend().selectSlot && backend().selectSlot(slot);
}
BRIDGE(jboolean, nUseItem)(JNIEnv *, jclass, jboolean hold) {
    return backend().useItem && backend().useItem(hold == JNI_TRUE);
}
BRIDGE(jboolean, nItemEsp)(JNIEnv *env, jclass, jboolean on, jint argb, jfloat range,
                           jboolean tracers, jint style, jstring filter) {
    if (!backend().itemEsp) return JNI_FALSE;
    std::string f;
    if (filter) {
        const char *c = env->GetStringUTFChars(filter, nullptr);
        if (c) { f = c; env->ReleaseStringUTFChars(filter, c); }
    }
    return backend().itemEsp(on == JNI_TRUE, static_cast<uint32_t>(argb), range, tracers == JNI_TRUE, style, f);
}
BRIDGE(jboolean, nDesync)(JNIEnv *, jclass, jboolean on, jint mode, jint delayMs) {
    return backend().desync && backend().desync(on == JNI_TRUE, mode, delayMs);
}
BRIDGE(jboolean, nSetRotation)(JNIEnv *, jclass, jfloat yaw, jfloat pitch) {
    return backend().setRotation && backend().setRotation(yaw, pitch);
}
BRIDGE(jboolean, nSwing)(JNIEnv *, jclass) {
    return backend().swing && backend().swing();
}
BRIDGE(jboolean, nHitbox)(JNIEnv *, jclass, jboolean on, jfloat w, jfloat h, jboolean mobs) {
    return backend().hitbox && backend().hitbox(on == JNI_TRUE, w, h, mobs == JNI_TRUE);
}
BRIDGE(jboolean, nKnockback)(JNIEnv *, jclass, jboolean on, jfloat h, jfloat v) {
    return backend().knockback && backend().knockback(on == JNI_TRUE, h, v);
}
BRIDGE(jboolean, nCanPlace)(JNIEnv *, jclass, jint x, jint y, jint z) {
    return backend().canPlace && backend().canPlace(x, y, z);
}
BRIDGE(jboolean, nPlace)(JNIEnv *, jclass, jint x, jint y, jint z) {
    return backend().place && backend().place(x, y, z);
}
BRIDGE(jint, nFindHotbar)(JNIEnv *, jclass, jint itemId) {
    return backend().findHotbar ? backend().findHotbar(itemId) : -1;
}
BRIDGE(jint, nSelectedSlot)(JNIEnv *, jclass) {
    return backend().selectedSlot ? backend().selectedSlot() : -1;
}

} // extern "C"
