package com.origin.launcher.Launcher.henshin.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.bridge.NativeGameBridge;
import com.origin.launcher.Launcher.henshin.module.AutoEat;
import com.origin.launcher.Launcher.henshin.module.Desync;
import com.origin.launcher.Launcher.henshin.module.ItemEsp;
import com.origin.launcher.Launcher.henshin.module.KillAura;
import com.origin.launcher.Launcher.henshin.module.Spider;
import com.origin.launcher.Launcher.henshin.value.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry + persistence + 20 Hz tick loop + keybinds for all Henshin modules.
 * The tick loop only runs while at least one module is enabled, so an idle client costs nothing.
 */
public final class ModuleManager {
    private static final String PREFS = "henshin_modules";
    private static final long TICK_MS = 50;
    private static volatile ModuleManager instance;

    public static ModuleManager get() {
        if (instance == null) {
            synchronized (ModuleManager.class) {
                if (instance == null) instance = new ModuleManager();
            }
        }
        return instance;
    }

    private final List<Module> modules = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final GameBridge bridge = new NativeGameBridge();
    private SharedPreferences prefs;
    private Activity activity;
    private InputInjector input;
    private boolean ticking;
    private boolean warnedNoBridge;
    /** True while any Henshin GUI is on screen (modules must not tap the game behind it). */
    public volatile boolean guiOpen;
    private final List<Runnable> toggleListeners = new ArrayList<>();

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            ticking = false;
            if (activity == null) return;
            boolean any = false;
            for (Module m : modules) {
                if (!m.isEnabled()) continue;
                any = true;
                try { m.onTick(bridge, input); } catch (Throwable t) {
                    android.util.Log.e("Henshin", m.name + " tick error", t);
                }
            }
            if (any) schedule();
        }
    };

    private ModuleManager() {
        modules.add(new KillAura());
        modules.add(new Spider());
        modules.add(new ItemEsp());
        modules.add(new AutoEat());
        modules.add(new Desync());
    }

    // ------------------------------------------------------------------ lifecycle

    public void attach(Activity a) {
        activity = a;
        input = new InputInjector(a);
        prefs = a.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (Module m : modules) load(m);
        schedule();
    }

    public void detach() {
        handler.removeCallbacks(tick);
        ticking = false;
        for (Module m : modules) {
            if (m.isEnabled()) { try { m.setEnabled(false); } catch (Throwable ignored) {} }
        }
        if (input != null) input.releaseAll();
        activity = null;
        input = null;
        warnedNoBridge = false;
    }

    private void schedule() {
        if (ticking || activity == null) return;
        ticking = true;
        handler.postDelayed(tick, TICK_MS);
    }

    // ------------------------------------------------------------------ access

    public List<Module> getModules() { return Collections.unmodifiableList(modules); }

    public List<Module> getModules(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.category == c) out.add(m);
        return out;
    }

    /** Case-insensitive, ignores spaces and underscores ("kill aura" == "Killaura"). */
    public Module find(String n) {
        if (n == null) return null;
        String k = n.replace(" ", "").replace("_", "");
        for (Module m : modules) if (m.name.replace(" ", "").equalsIgnoreCase(k)) return m;
        return null;
    }

    public GameBridge getBridge() { return bridge; }
    public Activity getActivity() { return activity; }
    public void addToggleListener(Runnable r) { toggleListeners.add(r); }
    public void removeToggleListener(Runnable r) { toggleListeners.remove(r); }

    void onModuleToggled(Module m) {
        saveModule(m);
        if (m.isEnabled()) {
            schedule();
            if (m.requiresBridge() && !bridge.isAvailable() && !warnedNoBridge) {
                warnedNoBridge = true;
                toast("Henshin: native game bridge not loaded - modules that read game memory will use input fallbacks or do nothing.");
            }
        }
        for (Runnable r : new ArrayList<>(toggleListeners)) r.run();
    }

    public void toast(String msg) {
        if (activity == null) return;
        activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show());
    }

    // ------------------------------------------------------------------ keybinds

    public void setBind(Module m, int keyCode) {
        m.setKeyCodeInternal(keyCode);
        saveModule(m);
        for (Runnable r : new ArrayList<>(toggleListeners)) r.run();
    }

    /** @return true when the key event was consumed as a module bind. */
    public boolean onKeyEvent(KeyEvent e) {
        if (activity == null || e.getKeyCode() == 0) return false;
        Module target = null;
        for (Module m : modules) if (m.getKeyCode() == e.getKeyCode()) { target = m; break; }
        if (target == null) return false;
        // Never steal keys while the player is typing in chat / any text box.
        if (activity.getCurrentFocus() instanceof EditText) return false;
        // Toggle once per physical press (ignore auto-repeat and key-up).
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) target.toggle();
        return true;
    }

    // ------------------------------------------------------------------ persistence

    private void load(Module m) {
        if (prefs == null) return;
        String p = m.name + ".";
        m.setKeyCodeInternal(prefs.getInt(p + "bind", 0));
        for (Value<?> v : m.getValues()) {
            String s = prefs.getString(p + "v." + v.name, null);
            if (s != null) v.deserialize(s);
        }
        m.restoreEnabled(prefs.getBoolean(p + "enabled", false));
    }

    void saveModule(Module m) {
        if (prefs == null) return;
        SharedPreferences.Editor e = prefs.edit();
        String p = m.name + ".";
        e.putBoolean(p + "enabled", m.isEnabled());
        e.putInt(p + "bind", m.getKeyCode());
        for (Value<?> v : m.getValues()) e.putString(p + "v." + v.name, v.serialize());
        e.apply();
    }

    // ------------------------------------------------------------------ key names

    public static String keyName(int code) {
        if (code == 0) return "None";
        String n = KeyEvent.keyCodeToString(code);
        return n.startsWith("KEYCODE_") ? n.substring(8) : n;
    }

    /** "Z", "f5", "rshift", "space", "7" ... -> Android key code, or 0 when unknown. */
    public static int parseKey(String s) {
        if (s == null) return 0;
        String k = s.trim().toUpperCase(java.util.Locale.ROOT);
        switch (k) {
            case "RSHIFT": return KeyEvent.KEYCODE_SHIFT_RIGHT;
            case "LSHIFT": case "SHIFT": return KeyEvent.KEYCODE_SHIFT_LEFT;
            case "RCTRL": return KeyEvent.KEYCODE_CTRL_RIGHT;
            case "LCTRL": case "CTRL": return KeyEvent.KEYCODE_CTRL_LEFT;
            case "RALT": return KeyEvent.KEYCODE_ALT_RIGHT;
            case "LALT": case "ALT": return KeyEvent.KEYCODE_ALT_LEFT;
            case "ESC": return KeyEvent.KEYCODE_ESCAPE;
            case "BACKSPACE": return KeyEvent.KEYCODE_DEL;
            case "ENTER": return KeyEvent.KEYCODE_ENTER;
            case "GRAVE": case "`": return KeyEvent.KEYCODE_GRAVE;
            default:
        }
        int c = KeyEvent.keyCodeFromString("KEYCODE_" + k);
        return c == KeyEvent.KEYCODE_UNKNOWN ? 0 : c;
    }
}
