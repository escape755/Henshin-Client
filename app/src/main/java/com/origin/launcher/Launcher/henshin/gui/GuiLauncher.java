package com.origin.launcher.Launcher.henshin.gui;

import android.app.Activity;

import com.origin.launcher.Launcher.henshin.core.HenshinSettings;

/** Opens the ClickGUI selected in Settings: "Panel" (tabbed window) or "TBUI" (OpenTBUI Toolbox-style). */
public final class GuiLauncher {
    private GuiLauncher() {}

    public static void toggle(Activity activity) {
        if (isOpen()) { closeAll(); return; }
        if (HenshinSettings.GUI_TBUI.equals(HenshinSettings.getGuiStyle(activity))) {
            try {
                TbuiGuiWindow.toggle(activity);
                return;
            } catch (Throwable t) {
                // Never leave the player without a GUI: fall back to the Panel and remember it.
                android.util.Log.e("Henshin", "TBUI ClickGUI failed, using Panel", t);
                HenshinSettings.setGuiStyle(activity, HenshinSettings.GUI_PANEL);
            }
        }
        PanelGuiWindow.toggle(activity);
    }

    public static boolean isOpen() { return PanelGuiWindow.isOpen() || TbuiGuiWindow.isOpen(); }

    public static void closeAll() {
        PanelGuiWindow.closeIfOpen();
        TbuiGuiWindow.closeIfOpen();
    }
}
