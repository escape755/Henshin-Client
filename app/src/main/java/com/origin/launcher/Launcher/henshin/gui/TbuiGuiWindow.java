package com.origin.launcher.Launcher.henshin.gui;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;

import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.HenshinSettings;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.shortcut.ShortcutManager;
import com.origin.launcher.Launcher.henshin.value.BoolValue;
import com.origin.launcher.Launcher.henshin.value.ColorValue;
import com.origin.launcher.Launcher.henshin.value.EnumValue;
import com.origin.launcher.Launcher.henshin.value.FloatValue;
import com.origin.launcher.Launcher.henshin.value.IntValue;
import com.origin.launcher.Launcher.henshin.value.TextValue;
import com.origin.launcher.Launcher.henshin.value.Value;
import com.origin.launcher.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev1503.opentbui.Icon;
import dev1503.opentbui.OpenTBUI;
import dev1503.opentbui.StatusManager;
import dev1503.opentbui.TBTheme;
import dev1503.opentbui.widgets.TBAction;
import dev1503.opentbui.widgets.TBEditText;
import dev1503.opentbui.widgets.TBToggle;
import dev1503.opentbui.widgets.TBWidget;

/**
 * "TBUI" ClickGUI: the Toolbox-style interface from OpenTBUI (categories on the left, toggles with
 * their settings on the right) driven by the Henshin {@link ModuleManager}.
 *
 * Everything is kept in sync through OpenTBUI's {@link StatusManager}:
 *   m/&lt;Module&gt;/enabled     -> module on/off (also follows keybinds / shortcuts / chat commands)
 *   m/&lt;Module&gt;/v/&lt;Value&gt;  -> numeric, enum and colour settings
 *   ui/...                       -> GUI options (accent colour, panel widths, shortcuts)
 */
public final class TbuiGuiWindow {
    private static final String P_ENABLED = "/enabled";
    private static final String P_VALUE = "/v/";
    private static final String P_ACCENT = "ui/accent";
    private static final String P_CAT_W = "ui/categories_width";
    private static final String P_FEAT_W = "ui/features_width";

    private static TbuiGuiWindow current;

    public static boolean isOpen() { return current != null && current.ui != null && current.ui.isShowing(); }
    public static void closeIfOpen() { if (current != null) current.close(); }

    public static void toggle(Activity a) {
        if (isOpen()) { current.close(); return; }
        if (current == null || current.act != a) {
            if (current != null) current.destroy();
            current = new TbuiGuiWindow(a);
        }
        current.open();
    }

    private final Activity act;
    private final ModuleManager mm = ModuleManager.get();
    private final StatusManager sm = new StatusManager();
    private final Map<String, Module> modulesByName = new HashMap<>();
    private final Map<String, Value<?>> valuesByPath = new HashMap<>();
    private final Map<Value<?>, TBWidget> widgetsByValue = new HashMap<>();
    private final List<Runnable> textRefreshers = new ArrayList<>();
    private final Map<Module, TBToggle> toggles = new HashMap<>();
    private final Map<Module, TBAction> shortcutActions = new HashMap<>();
    private final Runnable moduleListener = this::syncFromModules;
    private OpenTBUI ui;
    private boolean building;

    private TbuiGuiWindow(Activity a) {
        act = a;
        // The game activity may not use an AppCompat / Material theme; OpenTBUI's widgets need those
        // attributes. force=false only fills in what's missing, the game theme is left untouched.
        try {
            a.getTheme().applyStyle(com.google.android.material.R.style.Theme_MaterialComponents_DayNight_NoActionBar, false);
        } catch (Throwable ignored) {}
        build();
        mm.addToggleListener(moduleListener);
    }

    private void destroy() {
        mm.removeToggleListener(moduleListener);
        if (ui != null && ui.isShowing()) ui.hide();
    }

    private int dp(int v) { return (int) (v * act.getResources().getDisplayMetrics().density + 0.5f); }

    private static TBTheme themeFor(int accent) {
        float[] hsv = new float[3];
        Color.colorToHSV(accent, hsv);
        hsv[2] *= 0.8f;
        return new TBTheme(accent | 0xFF000000, Color.HSVToColor(hsv));
    }

    // ------------------------------------------------------------------ open / close

    private void open() {
        syncFromModules();
        mm.guiOpen = true;
        ui.show();
    }

    private void close() {
        if (ui != null) ui.hide();
        // hide() also fires the hide listener, which clears guiOpen
    }

    private void onHidden() {
        mm.guiOpen = com.origin.launcher.Launcher.inbuilt.gui.ClickGuiWindow.isOpen() || PanelGuiWindow.isOpen();
    }

    // ------------------------------------------------------------------ build

    private void build() {
        building = true;
        int accent = HenshinSettings.getAccent(act);
        ui = OpenTBUI.fromPopup(act, sm);
        ui.setTheme(themeFor(accent));
        ui.setCategoriesViewWidth(dp(150));
        ui.setFeaturesViewWidth(dp(260));
        ui.setOnHideListener(this::onHidden);
        ui.getTipBar().setText("Henshin Client");

        sm.setListener(new StatusManager.Listener() {
            @Override public void onValueChange(String path, double value) { handleValue(path, value); }
            @Override public void onActionTrigger(String path) { handleAction(path); }
        });

        for (Category c : Category.values()) {
            List<Module> list = mm.getModules(c);
            if (list.isEmpty()) continue;
            dev1503.opentbui.Category tb = ui.addCategory(c.title, iconFor(c));
            for (Module m : list) addModule(tb, m);
        }
        buildSettingsCategory();
        building = false;
    }

    private Icon iconFor(Category c) {
        int res;
        switch (c) {
            case COMBAT: res = R.drawable.tbui_swords_24px; break;
            case MOVEMENT: res = R.drawable.tbui_directions_run_24px; break;
            case RENDER: res = R.drawable.tbui_photo_camera_24px; break;
            case PLAYER: res = R.drawable.tbui_deployed_code_24px; break;
            case LEGIT: res = R.drawable.tbui_inbox_customize_24px; break;
            default: res = R.drawable.tbui_more_horiz_24px;
        }
        return new Icon(res);
    }

    private void addModule(dev1503.opentbui.Category tb, Module m) {
        modulesByName.put(m.name, m);
        String path = "m/" + m.name + P_ENABLED;
        TBToggle t = tb.addToggle(m.name, path, m.isEnabled());
        toggles.put(m, t);
        if (m.description != null && !m.description.isEmpty()) t.addLabel(m.description);

        for (Value<?> v : m.getValues()) {
            String vp = "m/" + m.name + P_VALUE + v.name;
            TBWidget w = null;
            if (v instanceof BoolValue) {
                w = t.addToggle(v.name, vp, ((BoolValue) v).get());
            } else if (v instanceof IntValue) {
                IntValue iv = (IntValue) v;
                w = t.addRangeSlider(v.name, vp, iv.min, iv.max).setDecimalScale(0);
                ((dev1503.opentbui.widgets.TBRangeSlider) w).setValueWithoutNotify(iv.get());
            } else if (v instanceof FloatValue) {
                FloatValue fv = (FloatValue) v;
                int scale = fv.step >= 1f ? 0 : fv.step >= 0.1f ? 1 : 2;
                w = t.addRangeSlider(v.name, vp, fv.min, fv.max).setDecimalScale(scale);
                ((dev1503.opentbui.widgets.TBRangeSlider) w).setValueWithoutNotify(fv.get());
            } else if (v instanceof EnumValue) {
                EnumValue ev = (EnumValue) v;
                w = t.addDropDown(v.name, vp, ev.options.toArray(new String[0]), Math.max(0, ev.index()));
            } else if (v instanceof ColorValue) {
                w = t.addColor(v.name, vp, ((ColorValue) v).get());
            } else if (v instanceof TextValue) {
                final TextValue tv = (TextValue) v;
                final TBEditText et = t.addEditText(v.name, tv.get(), text -> tv.set(text));
                textRefreshers.add(() -> et.setTextWithoutNotify(tv.get()));
                w = et;
            }
            if (w == null) continue;
            valuesByPath.put(vp, v);
            widgetsByValue.put(v, w);
        }

        TBAction sc = t.addAction(shortcutLabel(m), "act/shortcut/" + m.name);
        shortcutActions.put(m, sc);
        refreshVisibility();
    }

    private static String shortcutLabel(Module m) {
        return "Shortcut: " + (ShortcutManager.get().has(m) ? "remove" : "add");
    }

    private void buildSettingsCategory() {
        dev1503.opentbui.Category tb = ui.addCategory("Settings", new Icon(R.drawable.tbui_ic_settings_black_24dp));
        TBToggle theme = tb.addToggle("Theme", true);
        theme.addColor("Accent color", P_ACCENT, HenshinSettings.getAccent(act));
        TBToggle layout = tb.addToggle("Layout", true);
        layout.addRangeSlider("Categories width", P_CAT_W, 80, 300).setValueWithoutNotify(150);
        layout.addRangeSlider("Features width", P_FEAT_W, 160, 420).setValueWithoutNotify(260);
        layout.addAction("Reset layout", "act/reset_layout");
        tb.addAction("Remove all shortcuts", "act/clear_shortcuts");
        tb.addAction("Switch to Panel ClickGUI", "act/use_panel");
    }

    // ------------------------------------------------------------------ StatusManager -> Henshin

    private void handleValue(String path, double value) {
        if (path == null || building) return;
        if (path.startsWith("m/") && path.endsWith(P_ENABLED)) {
            Module m = modulesByName.get(path.substring(2, path.length() - P_ENABLED.length()));
            if (m != null) m.setEnabled(value >= 1);
            return;
        }
        Value<?> v = valuesByPath.get(path);
        if (v != null) {
            applyValue(v, value);
            refreshVisibility();
            return;
        }
        switch (path) {
            case P_ACCENT:
                HenshinSettings.setAccent(act, (int) value);
                ui.setTheme(themeFor((int) value));
                break;
            case P_CAT_W: ui.setCategoriesViewWidth(dp((int) value)); break;
            case P_FEAT_W: ui.setFeaturesViewWidth(dp((int) value)); break;
            default:
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyValue(Value<?> v, double d) {
        if (v instanceof BoolValue) ((BoolValue) v).set(d >= 1);
        else if (v instanceof IntValue) ((IntValue) v).set((int) Math.round(d));
        else if (v instanceof FloatValue) ((FloatValue) v).set((float) d);
        else if (v instanceof EnumValue) {
            EnumValue ev = (EnumValue) v;
            int i = (int) d;
            if (i >= 0 && i < ev.options.size()) ev.set(ev.options.get(i));
        } else if (v instanceof ColorValue) ((ColorValue) v).set((int) d);
    }

    private void handleAction(String path) {
        if (path == null) return;
        if (path.startsWith("act/shortcut/")) {
            Module m = modulesByName.get(path.substring("act/shortcut/".length()));
            if (m == null) return;
            ShortcutManager s = ShortcutManager.get();
            if (s.has(m)) { s.remove(m); mm.toast("Shortcut removed: " + m.name); }
            else { s.add(m); mm.toast("Shortcut added: " + m.name); }
            TBAction a = shortcutActions.get(m);
            if (a != null) a.setName(shortcutLabel(m));
            return;
        }
        switch (path) {
            case "act/reset_layout":
                sm.setValue(P_CAT_W, 150);
                sm.setValue(P_FEAT_W, 260);
                ui.setCategoriesViewWidth(dp(150));
                ui.setFeaturesViewWidth(dp(260));
                break;
            case "act/clear_shortcuts":
                ShortcutManager.get().clear();
                for (Map.Entry<Module, TBAction> e : shortcutActions.entrySet()) e.getValue().setName(shortcutLabel(e.getKey()));
                mm.toast("All shortcuts removed");
                break;
            case "act/use_panel":
                HenshinSettings.setGuiStyle(act, HenshinSettings.GUI_PANEL);
                close();
                GuiLauncher.toggle(act);
                break;
            default:
        }
    }

    // ------------------------------------------------------------------ Henshin -> StatusManager / widgets

    /** Pulls module state (enabled flag + every value) into the widgets. Safe to call any time. */
    private void syncFromModules() {
        building = true; // StatusManager callbacks would only write the same values back
        try {
            for (Map.Entry<Module, TBToggle> e : toggles.entrySet()) {
                sm.setValue("m/" + e.getKey().name + P_ENABLED, e.getKey().isEnabled() ? 1 : 0);
            }
            for (Map.Entry<String, Value<?>> e : valuesByPath.entrySet()) {
                Double d = toDouble(e.getValue());
                if (d != null) sm.setValue(e.getKey(), d);
            }
            for (Runnable r : textRefreshers) r.run();
        } finally {
            building = false;
        }
        refreshVisibility();
    }

    private static Double toDouble(Value<?> v) {
        if (v instanceof BoolValue) return ((BoolValue) v).get() ? 1.0 : 0.0;
        if (v instanceof IntValue) return ((IntValue) v).get().doubleValue();
        if (v instanceof FloatValue) return ((FloatValue) v).get().doubleValue();
        if (v instanceof EnumValue) return (double) Math.max(0, ((EnumValue) v).index());
        if (v instanceof ColorValue) return ((ColorValue) v).get().doubleValue();
        return null;
    }

    /** Hides settings whose {@code visibleWhen} condition is currently false. */
    private void refreshVisibility() {
        for (Map.Entry<Value<?>, TBWidget> e : widgetsByValue.entrySet()) {
            View view = e.getValue().getView();
            if (view != null) view.setVisibility(e.getKey().isVisible() ? View.VISIBLE : View.GONE);
        }
    }
}
