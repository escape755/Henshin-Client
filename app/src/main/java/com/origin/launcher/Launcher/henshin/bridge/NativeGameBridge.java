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
}
