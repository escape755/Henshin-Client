package com.origin.launcher.Launcher.henshin.gui;

import android.app.Activity;

import com.origin.launcher.Launcher.henshin.core.HenshinSettings;
import com.origin.launcher.Launcher.inbuilt.gui.ClickGuiWindow;

/** Opens the ClickGUI style chosen in Settings (Panel = new simple GUI, Toolbox = side panels). */
public final class GuiLauncher {
    private GuiLauncher() {}

    public static void toggle(Activity activity) {
        if (PanelGuiWindow.isOpen()) { PanelGuiWindow.closeIfOpen(); return; }
        if (ClickGuiWindow.isOpen()) { ClickGuiWindow.closeIfOpen(); return; }
        if (HenshinSettings.GUI_TOOLBOX.equals(HenshinSettings.getGuiStyle(activity))) {
            ClickGuiWindow.toggle(activity);
        } else {
            PanelGuiWindow.toggle(activity);
        }
    }

    public static boolean isOpen() { return PanelGuiWindow.isOpen() || ClickGuiWindow.isOpen(); }
    public static void closeAll() { PanelGuiWindow.closeIfOpen(); ClickGuiWindow.closeIfOpen(); }
}
