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

/** Constantly pushes you in the direction you are looking (horizontal and vertical). */
public class Jetpack extends Module {
    private final FloatValue speed = add(new FloatValue("Speed", 1.0f, 0.2f, 3.0f, 0.1f));
    private final BoolValue fastStop = add(new BoolValue("Fast Stop", false));

    public Jetpack() { super("Jetpack", "It JetPack :D", Category.MOVEMENT); }

    @Override public boolean requiresBridge() { return true; }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (!game.isAvailable()) {
            // Fallback: keep W held (forward). Combined with the game's own flight it moves you where you look.
            info = "forward";
            if (input == null) return;
            if (ModuleManager.get().guiOpen) input.keyUp(android.view.KeyEvent.KEYCODE_W);
            else input.keyDown(android.view.KeyEvent.KEYCODE_W);
            return;
        }
        if (ModuleManager.get().guiOpen) return;
        float[] st = game.playerState();
        if (st == null || st.length < 5) return;

        double yaw = Math.toRadians(st[3] + 90.0);
        double pitch = Math.toRadians(-st[4]); // Bedrock: positive pitch looks down
        float s = speed.get();
        game.setMotion(
                (float) (Math.cos(yaw) * Math.cos(pitch) * s),
                (float) (Math.sin(pitch) * s),
                (float) (Math.sin(yaw) * Math.cos(pitch) * s));
    }

    private String info = "";
    /** Text shown next to the name in the GUI: the speed, or "forward" while using the fallback. */
    @Override public String getInfo() {
        return !info.isEmpty() ? info : String.format(java.util.Locale.US, "%.1f", speed.get());
    }

    @Override protected void onDisable() {
        info = "";
        InputInjector in = ModuleManager.get().getInput();
        if (in != null) in.keyUp(android.view.KeyEvent.KEYCODE_W);
        GameBridge g = ModuleManager.get().getBridge();
        if (g.isAvailable()) g.setMotion(0f, 0f, 0f); // stop dead instead of drifting
    }
}
