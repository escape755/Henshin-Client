package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.value.BoolValue;
import com.origin.launcher.Launcher.henshin.value.EnumValue;
import com.origin.launcher.Launcher.henshin.value.FloatValue;

/** Climb walls: while pushing against a wall the player gets upward motion. */
public class Spider extends Module {
    private final EnumValue mode = add(new EnumValue("Mode", "Vanilla", "Vanilla", "Smooth"));
    private final FloatValue speed = add(new FloatValue("Speed", 0.20f, 0.05f, 0.60f, 0.01f));
    private final BoolValue stopAtTop = add(new BoolValue("Stop At Top", true));

    private float current;

    public Spider() { super("Spider", "Stick to walls and climb them", Category.MOVEMENT); }

    @Override public boolean requiresBridge() { return true; }
    @Override public String getInfo() { return mode.get(); }
    @Override protected void onEnable() { current = 0f; }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (!game.isAvailable()) return;
        if (game.collidingHorizontally()) {
            float target = speed.get();
            current = mode.is("Smooth") ? Math.min(target, current + target * 0.25f + 0.01f) : target;
            game.setMotion(0f, current, 0f);
        } else {
            current = 0f;
        }
    }
}
