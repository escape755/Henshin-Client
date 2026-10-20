package com.origin.launcher.Launcher.henshin.core;

import android.content.Context;
import android.content.SharedPreferences;

/** Small client-wide settings (GUI style, accent colour...). Works from anywhere with a Context. */
public final class HenshinSettings {
    public static final String GUI_TOOLBOX = "Toolbox";
    public static final String GUI_PANEL = "Panel";
    /** OpenTBUI (Toolbox-style) ClickGUI, see TbuiGuiWindow. */
    public static final String GUI_TBUI = "TBUI";
    private static final String PREFS = "henshin_settings";

    private HenshinSettings() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getGuiStyle(Context c) { return p(c).getString("gui_style", GUI_PANEL); }
    public static void setGuiStyle(Context c, String s) { p(c).edit().putString("gui_style", s).apply(); }

    public static int getAccent(Context c) { return p(c).getInt("accent", 0xFFFF1A00); }
    public static void setAccent(Context c, int argb) { p(c).edit().putInt("accent", argb).apply(); }

    public static boolean isShortcutEditMode(Context c) { return p(c).getBoolean("shortcut_edit", false); }
    public static void setShortcutEditMode(Context c, boolean b) { p(c).edit().putBoolean("shortcut_edit", b).apply(); }
}
