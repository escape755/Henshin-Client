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

/** Makes other players' hitboxes bigger so they are easier to hit. */
public class Hitbox extends Module {
    private final FloatValue height = add(new FloatValue("Height", 2.0f, 1.8f, 5.0f, 0.1f));
    private final FloatValue width = add(new FloatValue("Width", 4.0f, 0.6f, 5.0f, 0.1f));
    private final BoolValue mobs = add(new BoolValue("Mobs", false));

    public Hitbox() { super("Hitbox", "Increase hitbox size of entities", Category.COMBAT); }

    @Override public boolean requiresBridge() { return true; }
    @Override public String getInfo() { return String.format(java.util.Locale.US, "%.1f x %.1f", width.get(), height.get()); }

    private void push(GameBridge g, boolean on) { g.setHitbox(on, width.get(), height.get(), mobs.get()); }

    @Override protected void onEnable() {
        if (failWithoutBridge()) return;
        GameBridge g = ModuleManager.get().getBridge();
        if (g.isAvailable()) push(g, true);
    }

    @Override protected void onDisable() { push(ModuleManager.get().getBridge(), false); }

    // Re-applied every tick: new entities spawn all the time and the game resets their size.
    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (game.isAvailable()) push(game, true);
    }
}
