package com.origin.launcher.Launcher.henshin.bridge;

import android.util.Log;

/**
 * JNI-backed implementation. Native library: libhenshin_core.so (see HenshinNative).
 * If the library is not packaged or its hooks are not installed, isAvailable() is false.
 */
public final class NativeGameBridge implements GameBridge {
    private static final String TAG = "HenshinBridge";
    private static boolean loaded;

    static {
        try {
            System.loadLibrary("henshin_core");
            loaded = true;
        } catch (Throwable t) {
            loaded = false;
            Log.w(TAG, "henshin_core not available: " + t.getMessage());
        }
    }

    private static native boolean nReady();
    private static native float[] nPlayerState();
    private static native float[] nEntities(float range);
    private static native boolean nAttack(long id);
    private static native boolean nWallCollide();
    private static native void nSetMotion(float x, float y, float z);
    private static native int nHunger();
    private static native boolean nSelectSlot(int slot);
    private static native boolean nUseItem(boolean hold);
    private static native boolean nItemEsp(boolean on, int argb, float range, boolean tracers, int style, String filter);
    private static native boolean nDesync(boolean on, int mode, int delayMs);

    // Optional natives: an older libhenshin_core.so may not export them, so every call is guarded.
    private static native boolean nSetRotation(float yaw, float pitch);
    private static native boolean nSwing();
    private static native boolean nHitbox(boolean on, float w, float h, boolean mobs);
    private static native boolean nKnockback(boolean on, float h, float v);
    private static native boolean nCanPlace(int x, int y, int z);
    private static native boolean nPlace(int x, int y, int z);
    private static native int nFindHotbar(int itemId);
    private static native int nSelectedSlot();

    private boolean safeReady() {
        if (!loaded) return false;
        try { return nReady(); } catch (Throwable t) { loaded = false; return false; }
    }

    @Override public boolean isAvailable() { return safeReady(); }
    @Override public float[] playerState() { return safeReady() ? nPlayerState() : null; }
    @Override public float[] nearbyEntities(float r) { return safeReady() ? nEntities(r) : new float[0]; }
    @Override public boolean attack(long id) { return safeReady() && nAttack(id); }
    @Override public boolean collidingHorizontally() { return safeReady() && nWallCollide(); }
    @Override public void setMotion(float x, float y, float z) { if (safeReady()) nSetMotion(x, y, z); }
    @Override public int hunger() { return safeReady() ? nHunger() : -1; }
    @Override public boolean selectHotbar(int s) { return safeReady() && nSelectSlot(s); }
    @Override public boolean useItem(boolean h) { return safeReady() && nUseItem(h); }
    @Override public boolean setItemEsp(boolean on, int c, float r, boolean t, int st, String f) { return safeReady() && nItemEsp(on, c, r, t, st, f == null ? "" : f); }
    @Override public boolean setDesync(boolean on, int m, int d) { return safeReady() && nDesync(on, m, d); }

    @Override public boolean setRotation(float yaw, float pitch) {
        if (!safeReady()) return false;
        try { return nSetRotation(yaw, pitch); } catch (UnsatisfiedLinkError e) { return false; }
    }
    @Override public boolean swing() {
        if (!safeReady()) return false;
        try { return nSwing(); } catch (UnsatisfiedLinkError e) { return false; }
    }
    @Override public boolean setHitbox(boolean on, float w, float h, boolean mobs) {
        if (!safeReady()) return false;
        try { return nHitbox(on, w, h, mobs); } catch (UnsatisfiedLinkError e) { return false; }
    }
    @Override public boolean setKnockback(boolean on, float h, float v) {
        if (!safeReady()) return false;
        try { return nKnockback(on, h, v); } catch (UnsatisfiedLinkError e) { return false; }
    }
    @Override public boolean canPlaceBlock(int x, int y, int z) {
        if (!safeReady()) return false;
        try { return nCanPlace(x, y, z); } catch (UnsatisfiedLinkError e) { return false; }
    }
    @Override public boolean placeBlock(int x, int y, int z) {
        if (!safeReady()) return false;
        try { return nPlace(x, y, z); } catch (UnsatisfiedLinkError e) { return false; }
    }
    @Override public int findHotbarItem(int itemId) {
        if (!safeReady()) return -1;
        try { return nFindHotbar(itemId); } catch (UnsatisfiedLinkError e) { return -1; }
    }
    @Override public int selectedSlot() {
        if (!safeReady()) return -1;
        try { return nSelectedSlot(); } catch (UnsatisfiedLinkError e) { return -1; }
    }
}
