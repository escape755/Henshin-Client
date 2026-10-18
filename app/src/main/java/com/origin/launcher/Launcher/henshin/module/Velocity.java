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

import java.util.Locale;

/** Scales the knockback you take. 0% = none, 100% = vanilla. */
public class Velocity extends Module {
    private final FloatValue horizontal = add(new FloatValue("Horizontal", 0f, 0f, 100f, 1f));
    private final FloatValue vertical = add(new FloatValue("Vertical", 0f, 0f, 100f, 1f));
    private int ticks;

    public Velocity() { super("Velocity", "Prevent you from knockback", Category.MOVEMENT); }

    @Override public boolean requiresBridge() { return true; }

    @Override public String getInfo() {
        return String.format(Locale.US, "H%d%% V%d%%", Math.round(horizontal.get()), Math.round(vertical.get()));
    }

    private void push(GameBridge g, boolean on) { g.setKnockback(on, horizontal.get(), vertical.get()); }

    @Override protected void onEnable() {
        if (failWithoutBridge()) return;
        ticks = 0;
        GameBridge g = ModuleManager.get().getBridge();
        if (g.isAvailable()) push(g, true);
    }

    @Override protected void onDisable() { push(ModuleManager.get().getBridge(), false); }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (game.isAvailable() && ++ticks % 4 == 0) push(game, true); // apply slider changes live
    }
}
