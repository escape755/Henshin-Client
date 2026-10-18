package com.origin.launcher.Launcher.henshin.module;

import android.view.KeyEvent;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.value.EnumValue;
import com.origin.launcher.Launcher.henshin.value.IntValue;

/** Port of Abysm's AntiAFK: sends a harmless key press every N seconds so the server never sees you idle. */
public class AntiAFK extends Module {
    private final IntValue interval = add(new IntValue("Interval", 30, 5, 300, 5));
    private final EnumValue action = add(new EnumValue("Action", "Jump", "Jump", "Sneak"));
    private long last;

    public AntiAFK() { super("AntiAFK", "Avoids being kicked for idling (Abysm port)", Category.PLAYER); }

    @Override public String getInfo() { return interval.get() + "s"; }
    @Override protected void onEnable() { last = System.currentTimeMillis(); }

    @Override public void onTick(GameBridge game, InputInjector input) {
        if (input == null || ModuleManager.get().guiOpen) return;
        long now = System.currentTimeMillis();
        if (now - last < interval.get() * 1000L) return;
        last = now;
        input.key(action.is("Jump") ? KeyEvent.KEYCODE_SPACE : KeyEvent.KEYCODE_SHIFT_LEFT);
    }
}
