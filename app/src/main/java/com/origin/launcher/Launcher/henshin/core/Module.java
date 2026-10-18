package com.origin.launcher.Launcher.henshin.core;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A Henshin module: toggleable, can own a keybind, a list of values and a tick loop (20 Hz). */
public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;

    private boolean enabled;
    private int keyCode = 0; // Android KeyEvent code, 0 = unbound
    private final List<Value<?>> values = new ArrayList<>();

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <V extends Value<?>> V add(V v) {
        values.add(v);
        v.setOnChange(() -> ModuleManager.get().saveModule(this));
        return v;
    }

    protected <V extends Value<?>> V add(V v, java.util.function.BooleanSupplier visible) {
        v.visibleWhen(visible);
        return add(v);
    }

    public List<Value<?>> getValues() { return Collections.unmodifiableList(values); }

    public Value<?> findValue(String n) {
        for (Value<?> v : values) if (v.name.equalsIgnoreCase(n.replace("_", " "))
                || v.name.replace(" ", "").equalsIgnoreCase(n)) return v;
        return null;
    }

    public boolean isEnabled() { return enabled; }
    public int getKeyCode() { return keyCode; }
    void setKeyCodeInternal(int k) { keyCode = k; }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        try {
            if (on) onEnable(); else onDisable();
        } catch (Throwable t) {
            android.util.Log.e("Henshin", name + " toggle failed", t);
        }
        ModuleManager.get().onModuleToggled(this);
    }

    /** Used while loading saved state: sets the flag and runs onEnable without notifications. */
    void restoreEnabled(boolean on) {
        enabled = on;
        if (on) { try { onEnable(); } catch (Throwable ignored) {} }
    }

    /** Sets the flag without running onEnable/onDisable or notifying anyone (state mirrored from a file). */
    void setEnabledSilently(boolean on) { enabled = on; }

    public void toggle() { setEnabled(!enabled); }

    /** Short status text shown next to the name in the GUI (optional). */
    public String getInfo() { return ""; }

    protected void onEnable() {}
    protected void onDisable() {}
    /** Called ~20 times a second while enabled, on the main thread. */
    public void onTick(GameBridge game, InputInjector input) {}

    /**
     * For modules that cannot work at all without game-memory access: tells the player and switches
     * the module off again. Call from onEnable and return when it answers true.
     */
    protected final boolean failWithoutBridge() {
        if (ModuleManager.get().getBridge().isAvailable()) return false;
        ModuleManager.get().toast(name + " needs game-memory access, which this build doesn't have.");
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> setEnabled(false));
        return true;
    }

    /** Needs the native game bridge to do anything useful. */
    public boolean requiresBridge() { return false; }
}
