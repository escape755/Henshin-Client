package com.origin.launcher.Launcher.henshin.shortcut;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.origin.launcher.Launcher.henshin.core.HenshinSettings;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Floating on-screen buttons that toggle a module. They can be placed anywhere on the screen:
 * turn on edit mode (Settings or ".shortcut edit"), drag them where you want, long-press to delete.
 */
public final class ShortcutManager {
    private static final String PREFS = "henshin_shortcuts";
    private static volatile ShortcutManager instance;

    public static ShortcutManager get() {
        if (instance == null) {
            synchronized (ShortcutManager.class) {
                if (instance == null) instance = new ShortcutManager();
            }
        }
        return instance;
    }

    private final Map<String, ShortcutButton> buttons = new LinkedHashMap<>();
    private final Map<String, float[]> positions = new LinkedHashMap<>();
    private Activity activity;
    private SharedPreferences prefs;
    private Runnable stateListener;

    private ShortcutManager() {}

    public void attach(Activity a) {
        activity = a;
        prefs = a.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        loadPositions();
        stateListener = this::refreshStates;
        ModuleManager.get().addToggleListener(stateListener);
        showAll();
    }

    public void detach() {
        if (stateListener != null) ModuleManager.get().removeToggleListener(stateListener);
        for (ShortcutButton b : buttons.values()) b.hide();
        buttons.clear();
        activity = null;
    }

    public boolean has(Module m) { return positions.containsKey(m.name); }

    public void add(Module m) {
        if (activity == null || has(m)) return;
        float density = activity.getResources().getDisplayMetrics().density;
        float x = 24 * density;
        float y = (90 + 60 * positions.size()) * density;
        positions.put(m.name, new float[]{x, y});
        savePositions();
        showOne(m.name);
    }

    public void remove(Module m) {
        positions.remove(m.name);
        ShortcutButton b = buttons.remove(m.name);
        if (b != null) b.hide();
        savePositions();
    }

    public void clear() {
        for (ShortcutButton b : buttons.values()) b.hide();
        buttons.clear();
        positions.clear();
        savePositions();
    }

    public void setEditMode(boolean on) {
        if (activity != null) HenshinSettings.setShortcutEditMode(activity, on);
        for (ShortcutButton b : buttons.values()) b.refresh();
    }

    public int getSizeDp() { return prefs == null ? 52 : prefs.getInt("size_dp", 52); }
    public void setSizeDp(int dp) {
        if (prefs != null) prefs.edit().putInt("size_dp", Math.max(36, Math.min(96, dp))).apply();
        reshow();
    }
    public int getOpacity() { return prefs == null ? 90 : prefs.getInt("opacity", 90); }
    public void setOpacity(int pct) {
        if (prefs != null) prefs.edit().putInt("opacity", Math.max(20, Math.min(100, pct))).apply();
        for (ShortcutButton b : buttons.values()) b.refresh();
    }

    void onMoved(String name, float x, float y) {
        positions.put(name, new float[]{x, y});
        savePositions();
    }

    void requestRemove(String name) {
        Module m = ModuleManager.get().find(name);
        if (m != null) remove(m);
    }

    // ------------------------------------------------------------------

    private void reshow() {
        for (ShortcutButton b : buttons.values()) b.hide();
        buttons.clear();
        showAll();
    }

    private void showAll() {
        for (String n : positions.keySet()) showOne(n);
    }

    private void showOne(String name) {
        Module m = ModuleManager.get().find(name);
        float[] pos = positions.get(name);
        if (m == null || pos == null || activity == null || buttons.containsKey(name)) return;
        ShortcutButton b = new ShortcutButton(activity, m, this);
        buttons.put(name, b);
        b.show((int) pos[0], (int) pos[1]);
    }

    private void refreshStates() {
        for (ShortcutButton b : buttons.values()) b.refresh();
    }

    private void loadPositions() {
        positions.clear();
        String raw = prefs.getString("list", "");
        for (String e : raw.split(";")) {
            String[] p = e.split(",");
            if (p.length != 3) continue;
            try { positions.put(p[0], new float[]{Float.parseFloat(p[1]), Float.parseFloat(p[2])}); }
            catch (NumberFormatException ignored) {}
        }
    }

    private void savePositions() {
        if (prefs == null) return;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, float[]> e : positions.entrySet())
            sb.append(e.getKey()).append(',').append(e.getValue()[0]).append(',').append(e.getValue()[1]).append(';');
        prefs.edit().putString("list", sb.toString()).apply();
    }
}
