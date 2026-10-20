package com.origin.launcher.Launcher.henshin.bridge;

import android.util.Log;

/**
 * Client-side visual patches implemented in libhenshin_core.so (ported from BedrockTools).
 * Every call returns false when the library is missing, the game isn't loaded yet or the
 * signature for the running Minecraft version wasn't found.
 */
public final class NativeFx {
    private static final String TAG = "HenshinFx";
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

    private NativeFx() {}

    private static native boolean nAvailable();
    private static native boolean nFullbright(boolean on);
    private static native boolean nTime(boolean on, int time);
    private static native boolean nWeather(boolean on, int mode);

    public static boolean available() {
        if (!loaded) return false;
        try { return nAvailable(); } catch (Throwable t) { loaded = false; return false; }
    }

    public static boolean fullbright(boolean on) {
        if (!loaded) return false;
        try { return nFullbright(on); } catch (Throwable t) { Log.w(TAG, "fullbright", t); return false; }
    }

    /** @param time ticks of the day, 0..24000 (6000 = noon, 18000 = midnight). */
    public static boolean time(boolean on, int time) {
        if (!loaded) return false;
        try { return nTime(on, time); } catch (Throwable t) { Log.w(TAG, "time", t); return false; }
    }

    /** @param mode 0 clear, 1 rain, 2 thunder, 3 snow. */
    public static boolean weather(boolean on, int mode) {
        if (!loaded) return false;
        try { return nWeather(on, mode); } catch (Throwable t) { Log.w(TAG, "weather", t); return false; }
    }
}
