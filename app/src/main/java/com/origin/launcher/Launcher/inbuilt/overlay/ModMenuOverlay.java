package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.widget.ImageButton;
import com.origin.launcher.Launcher.inbuilt.gui.ClickGuiWindow;
import com.origin.launcher.R;

/**
 * Floating button that opens / closes the Henshin ClickGUI (toolbox style panel).
 * The old ModMenuDialog is still in the project but is no longer used from here.
 */
public class ModMenuOverlay extends BaseOverlayButton {

    public ModMenuOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return "mod_menu";
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_modmenu;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {
        btn.setBackgroundResource(R.drawable.round_button_bg);
    }

    @Override
    protected void onButtonClick() {
        com.origin.launcher.Launcher.henshin.gui.GuiLauncher.toggle(activity);
    }
}
