package com.origin.launcher.Launcher.henshin.bridge;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/** Synthetic touches / keys sent to the game activity (same technique as the Auto Clicker). */
public class InputInjector {
    private static final int POINTER_ID = 14; // high id so it never clashes with real fingers
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean holding;

    public InputInjector(Activity activity) { this.activity = activity; }

    private float cx() {
        View d = activity.getWindow().getDecorView();
        return d.getWidth() > 0 ? d.getWidth() / 2f : activity.getResources().getDisplayMetrics().widthPixels / 2f;
    }
    private float cy() {
        View d = activity.getWindow().getDecorView();
        return d.getHeight() > 0 ? d.getHeight() / 2f : activity.getResources().getDisplayMetrics().heightPixels / 2f;
    }

    /** Quick tap on the crosshair = attack / break. */
    public void tapCenter() {
        final long t = SystemClock.uptimeMillis();
        touch(MotionEvent.ACTION_DOWN, t, t);
        handler.postDelayed(() -> touch(MotionEvent.ACTION_UP, t, SystemClock.uptimeMillis()), 20);
    }

    /** Long press on the crosshair = use / eat item. */
    public void holdCenter(long ms, Runnable onDone) {
        if (holding) return;
        holding = true;
        final long t = SystemClock.uptimeMillis();
        touch(MotionEvent.ACTION_DOWN, t, t);
        handler.postDelayed(() -> {
            touch(MotionEvent.ACTION_UP, t, SystemClock.uptimeMillis());
            holding = false;
            if (onDone != null) onDone.run();
        }, ms);
    }

    public boolean isHolding() { return holding; }

    public void releaseAll() {
        handler.removeCallbacksAndMessages(null);
        if (holding) {
            long n = SystemClock.uptimeMillis();
            touch(MotionEvent.ACTION_UP, n, n);
            holding = false;
        }
    }

    public void key(int keyCode) {
        long t = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(t, t, KeyEvent.ACTION_DOWN, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
        KeyEvent up = new KeyEvent(t, t + 10, KeyEvent.ACTION_UP, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
        try { activity.dispatchKeyEvent(down); activity.dispatchKeyEvent(up); } catch (Exception ignored) {}
    }

    /** Selects hotbar slot 0..8 by sending the number key. */
    public void hotbar(int slot0to8) { key(KeyEvent.KEYCODE_1 + Math.max(0, Math.min(8, slot0to8))); }

    private void touch(int action, long down, long now) {
        MotionEvent.PointerProperties[] p = {new MotionEvent.PointerProperties()};
        p[0].id = POINTER_ID;
        p[0].toolType = MotionEvent.TOOL_TYPE_FINGER;
        MotionEvent.PointerCoords[] c = {new MotionEvent.PointerCoords()};
        c[0].x = cx(); c[0].y = cy(); c[0].pressure = 1f; c[0].size = 1f;
        MotionEvent e = MotionEvent.obtain(down, now, action, 1, p, c, 0, 0, 1f, 1f, 0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0);
        try { activity.dispatchTouchEvent(e); } catch (Exception ignored) { } finally { e.recycle(); }
    }
}
