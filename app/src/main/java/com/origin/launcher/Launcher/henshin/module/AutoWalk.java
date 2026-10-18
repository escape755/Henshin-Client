package com.origin.launcher.Launcher.henshin.module;

import android.view.KeyEvent;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.value.BoolValue;

/** Port of Abysm's AutoWalk: keeps W held. Done with synthetic key events, no packets needed. */
public class AutoWalk extends Module {
    private final BoolValue sprint = add(new BoolValue("Sprint", false));

    public AutoWalk() { super("AutoWalk", "Walks forward by itself (Abysm port)", Category.MOVEMENT); }

    @Override public void onTick(GameBridge game, InputInjector input) {
        if (input == null) return;
        if (ModuleManager.get().guiOpen) { release(input); return; }
        input.keyDown(KeyEvent.KEYCODE_W);
        if (sprint.get()) input.keyDown(KeyEvent.KEYCODE_CTRL_LEFT);
        else input.keyUp(KeyEvent.KEYCODE_CTRL_LEFT);
    }

    @Override protected void onDisable() {
        InputInjector in = ModuleManager.get().getInput();
        if (in != null) release(in);
    }

    private void release(InputInjector in) {
        in.keyUp(KeyEvent.KEYCODE_W);
        in.keyUp(KeyEvent.KEYCODE_CTRL_LEFT);
    }
}
