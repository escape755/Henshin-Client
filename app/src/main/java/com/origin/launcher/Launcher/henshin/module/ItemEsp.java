package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.value.BoolValue;
import com.origin.launcher.Launcher.henshin.value.ColorValue;
import com.origin.launcher.Launcher.henshin.value.EnumValue;
import com.origin.launcher.Launcher.henshin.value.FloatValue;
import com.origin.launcher.Launcher.henshin.value.TextValue;

/** Highlights dropped items through walls. Rendering is done by the native bridge. */
public class ItemEsp extends Module {
    private final EnumValue style = add(new EnumValue("Style", "Box", "Box", "Outline", "Glow"));
    private final ColorValue color = add(new ColorValue("Color", 0xFFFFD400));
    private final FloatValue range = add(new FloatValue("Range", 24f, 4f, 64f, 1f));
    private final BoolValue tracers = add(new BoolValue("Tracers", false));
    private final TextValue filter = add(new TextValue("Filter", "", 64));

    private int ticks;

    public ItemEsp() { super("ItemEsp", "Shows dropped items through walls", Category.RENDER); }

    @Override public boolean requiresBridge() { return true; }
    @Override public String getInfo() { return style.get(); }

    private void push(GameBridge g, boolean on) {
        g.setItemEsp(on, color.get(), range.get(), tracers.get(), style.index(), filter.get());
    }

    @Override protected void onEnable() {
        ticks = 0;
        com.origin.launcher.Launcher.henshin.core.ModuleManager m = com.origin.launcher.Launcher.henshin.core.ModuleManager.get();
        push(m.getBridge(), true);
    }

    @Override protected void onDisable() {
        push(com.origin.launcher.Launcher.henshin.core.ModuleManager.get().getBridge(), false);
    }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        // Re-send the settings twice a second so GUI changes apply live.
        if (++ticks % 10 == 0) push(game, true);
    }
}
