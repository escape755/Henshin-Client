package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;

/**
 * Base for modules backed by {@code NativeFx}. The game library may not be loaded yet when a saved
 * "enabled" state is restored, so applying is retried every couple of seconds before giving up.
 */
abstract class NativeFxModule extends Module {
    private static final int RETRY_TICKS = 40, MAX_TRIES = 15;
    private boolean applied;
    private int tries, wait;
    private String lastState = "";

    protected NativeFxModule(String name, String description, Category category) {
        super(name, description, category);
    }

    /** Pushes the current settings to the native side. @return true when it took effect. */
    protected abstract boolean apply(boolean on);
    /** Settings snapshot; when it changes while enabled the module re-applies. */
    protected abstract String state();

    @Override protected void onEnable() { applied = false; tries = 0; wait = 0; lastState = state(); applied = apply(true); }

    @Override protected void onDisable() {
        if (applied) apply(false);
        applied = false;
    }

    @Override public String getInfo() { return applied ? "" : "waiting"; }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (!applied) {
            if (++wait < RETRY_TICKS) return;
            wait = 0;
            applied = apply(true);
            if (!applied && ++tries >= MAX_TRIES) {
                ModuleManager.get().toast(name + " isn't supported by this Minecraft version (signature not found).");
                setEnabled(false);
            }
            return;
        }
        String s = state();
        if (!s.equals(lastState)) { lastState = s; apply(true); }
    }
}
