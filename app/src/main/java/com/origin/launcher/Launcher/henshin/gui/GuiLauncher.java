package com.origin.launcher.Launcher.henshin.gui;

import android.app.Activity;

/** Opens the Henshin ClickGUI (the tabbed Panel). The old Toolbox GUI is no longer reachable. */
public final class GuiLauncher {
    private GuiLauncher() {}

    public static void toggle(Activity activity) {
        if (PanelGuiWindow.isOpen()) { PanelGuiWindow.closeIfOpen(); return; }
        PanelGuiWindow.toggle(activity);
    }

    public static boolean isOpen() { return PanelGuiWindow.isOpen(); }
    public static void closeAll() { PanelGuiWindow.closeIfOpen(); }
}
