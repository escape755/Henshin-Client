package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.value.BoolValue;
import com.origin.launcher.Launcher.henshin.value.EnumValue;
import com.origin.launcher.Launcher.henshin.value.FloatValue;
import com.origin.launcher.Launcher.henshin.value.IntValue;

import android.view.KeyEvent;

/**
 * Creative-style flight: velocity is overwritten every tick, so you hover in place.
 * W/A/S/D move relative to where you look, Space goes up, Shift goes down.
 * Those keys are read from a hardware keyboard / gamepad (the on-screen joystick cannot be read
 * from here); with touch controls only, Fly hovers and Jetpack is the better choice.
 */
public class Fly extends Module {
    private final FloatValue hSpeed = add(new FloatValue("HSpeed", 1.0f, 0.2f, 3.0f, 0.1f));
    private final FloatValue vSpeed = add(new FloatValue("VSpeed", 0.5f, 0.2f, 3.0f, 0.1f));
    private final FloatValue glide = add(new FloatValue("Glide", 0.0f, -0.15f, 0.0f, 0.01f));
    private final FloatValue shiftHSpeed = add(new FloatValue("Shift HSpeed", 0.5f, 0.1f, 3.0f, 0.1f));

    public Fly() { super("Fly", "Fly like superman", Category.MOVEMENT); }

    @Override public boolean requiresBridge() { return true; }

    private boolean fallbackToggled;

    /** Without game access the only option is the game's own flight: double-tap jump (creative / mayfly only). */
    @Override protected void onEnable() {
        fallbackToggled = false;
        ModuleManager mm = ModuleManager.get();
        if (!mm.getBridge().isAvailable() && mm.getInput() != null) {
            mm.getInput().doubleKey(KeyEvent.KEYCODE_SPACE);
            fallbackToggled = true;
        }
    }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (!game.isAvailable()) return;
        float[] st = game.playerState();
        if (st == null || st.length < 5) return;

        ModuleManager mm = ModuleManager.get();
        float y = glide.get();
        float mx = 0f, mz = 0f;

        if (!mm.guiOpen) {
            boolean w = mm.isKeyDown(KeyEvent.KEYCODE_W), a = mm.isKeyDown(KeyEvent.KEYCODE_A);
            boolean s = mm.isKeyDown(KeyEvent.KEYCODE_S), d = mm.isKeyDown(KeyEvent.KEYCODE_D);
            boolean up = mm.isKeyDown(KeyEvent.KEYCODE_SPACE);
            boolean down = mm.isKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT) || mm.isKeyDown(KeyEvent.KEYCODE_SHIFT_RIGHT);

            if (up) y += vSpeed.get();
            if (down) y -= vSpeed.get();

            int fwd = (w ? 1 : 0) - (s ? 1 : 0);
            int str = (d ? 1 : 0) - (a ? 1 : 0);
            if (fwd != 0 || str != 0) {
                // Same maths as the desktop module: yaw + atan2(strafe, forward), Bedrock convention.
                double yaw = st[3] + Math.toDegrees(Math.atan2(str, fwd));
                double rad = Math.toRadians(yaw + 90.0);
                float speed = down ? shiftHSpeed.get() : hSpeed.get();
                mx = (float) Math.cos(rad) * speed;
                mz = (float) Math.sin(rad) * speed;
            }
        }
        game.setMotion(mx, y, mz);
    }

    @Override public String getInfo() { return ModuleManager.get().getBridge().isAvailable()
            ? String.format(java.util.Locale.US, "H%.1f V%.1f", hSpeed.get(), vSpeed.get()) : "creative"; }

    @Override protected void onDisable() {
        ModuleManager mm = ModuleManager.get();
        if (fallbackToggled && mm.getInput() != null) { mm.getInput().doubleKey(KeyEvent.KEYCODE_SPACE); fallbackToggled = false; }
        GameBridge g = mm.getBridge();
        if (g.isAvailable()) g.setMotion(0f, 0f, 0f);
    }
}
