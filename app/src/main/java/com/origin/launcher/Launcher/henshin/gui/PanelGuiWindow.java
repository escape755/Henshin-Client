package com.origin.launcher.Launcher.henshin.gui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.HenshinSettings;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.shortcut.ShortcutManager;

/**
 * "Panel" ClickGUI: one small floating window with category chips on top and the modules below.
 * The last chip, "Shortcuts", controls the floating buttons. Everything else stays visible.
 */
public final class PanelGuiWindow {
    private static PanelGuiWindow current;
    private static int lastTab = 0;

    public static boolean isOpen() { return current != null && current.showing; }
    public static void closeIfOpen() { if (current != null) current.close(); }
    public static void toggle(Activity a) {
        if (isOpen()) { current.close(); return; }
        current = new PanelGuiWindow(a);
        current.open();
    }

    private final Activity act;
    private final WindowManager wm;
    private final float d;
    private final int accent;
    private LinearLayout root;
    private LinearLayout content;
    private ScrollView scroll;
    private TextView[] chips;
    private boolean showing;

    private PanelGuiWindow(Activity a) {
        act = a;
        wm = (WindowManager) a.getSystemService(Context.WINDOW_SERVICE);
        d = a.getResources().getDisplayMetrics().density;
        accent = HenshinSettings.getAccent(a);
    }

    private int dp(float v) { return (int) (v * d + 0.5f); }

    private void open() {
        build();
        int sw = act.getResources().getDisplayMetrics().widthPixels;
        int sh = act.getResources().getDisplayMetrics().heightPixels;
        int w = Math.min(dp(520), (int) (sw * 0.78f));
        int h = (int) (sh * 0.82f);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(w, h,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.CENTER;
        lp.token = act.getWindow().getDecorView().getWindowToken();
        try {
            wm.addView(root, lp);
        } catch (Exception e) {
            ViewGroup c = act.findViewById(android.R.id.content);
            if (c == null) return;
            c.addView(root, new FrameLayout.LayoutParams(w, h, Gravity.CENTER));
        }
        showing = true;
        ModuleManager.get().guiOpen = true;
        root.setAlpha(0f);
        root.setScaleX(0.96f);
        root.setScaleY(0.96f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(140).start();
    }

    private void close() {
        if (!showing) return;
        showing = false;
        if (current == this) current = null;
        ModuleManager.get().guiOpen = com.origin.launcher.Launcher.inbuilt.gui.ClickGuiWindow.isOpen();
        try {
            if (root.getParent() instanceof ViewGroup) ((ViewGroup) root.getParent()).removeView(root);
            else wm.removeView(root);
        } catch (Exception ignored) {}
    }

    private void build() {
        root = new LinearLayout(act);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0xE60E0E12);
        bg.setStroke(dp(1), 0x33FFFFFF);
        root.setBackground(bg);
        root.setClickable(true);

        // header
        LinearLayout header = new LinearLayout(act);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(10), dp(8), dp(4));
        TextView title = ModuleCard.text(act, "HENSHIN", 14, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.18f);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView close = ModuleCard.text(act, "✕", 18, Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> close());
        header.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        // tabs
        HorizontalScrollView hs = new HorizontalScrollView(act);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(act);
        tabs.setPadding(dp(12), dp(2), dp(12), dp(6));
        Category[] cats = Category.values();
        chips = new TextView[cats.length + 1];
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            TextView c = ModuleCard.text(act, i < cats.length ? cats[i].title : "Shortcuts", 13, Color.WHITE);
            c.setGravity(Gravity.CENTER);
            c.setPadding(dp(14), dp(6), dp(14), dp(6));
            c.setOnClickListener(v -> select(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(6);
            tabs.addView(c, lp);
            chips[i] = c;
        }
        hs.addView(tabs);
        root.addView(hs, new LinearLayout.LayoutParams(-1, -2));

        scroll = new ScrollView(act);
        scroll.setVerticalScrollBarEnabled(false);
        content = new LinearLayout(act);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(2), 0, dp(10));
        scroll.addView(content, new ViewGroup.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        select(Math.min(lastTab, chips.length - 1));
    }

    private void select(int idx) {
        lastTab = idx;
        Category[] cats = Category.values();
        for (int i = 0; i < chips.length; i++) {
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(16));
            g.setColor(i == idx ? accent : 0x26FFFFFF);
            chips[i].setBackground(g);
        }
        content.removeAllViews();
        if (idx < cats.length) {
            for (Module m : ModuleManager.get().getModules(cats[idx])) {
                content.addView(ModuleCard.build(act, m, accent), new LinearLayout.LayoutParams(-1, -2));
            }
            if (ModuleManager.get().getModules(cats[idx]).isEmpty()) {
                TextView e = ModuleCard.text(act, "No modules here yet", 13, 0xFF999999);
                e.setPadding(dp(16), dp(16), dp(16), dp(16));
                content.addView(e);
            }
        } else {
            buildShortcutTab();
        }
        scroll.scrollTo(0, 0);
    }

    private void buildShortcutTab() {
        ShortcutManager sm = ShortcutManager.get();
        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(6), dp(16), dp(6));

        TextView info = ModuleCard.text(act,
                "Floating buttons that toggle a module. Add them from a module's gear (\"Floating shortcut\") "
                        + "or with .shortcut add <module>. Turn on edit mode to drag them anywhere; long-press to delete.",
                11, 0xFFBBBBBB);
        box.addView(info);

        com.google.android.material.materialswitch.MaterialSwitch edit = ModuleCard.newSwitch(act);
        boolean on = HenshinSettings.isShortcutEditMode(act);
        edit.setChecked(on);
        ModuleCard.style(edit, on, accent);
        edit.setOnCheckedChangeListener((b, c) -> { sm.setEditMode(c); ModuleCard.style(edit, c, accent); });
        LinearLayout row = new LinearLayout(act);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(ModuleCard.text(act, "Edit mode (move / delete)", 13, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(edit);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
        rl.topMargin = dp(10);
        box.addView(row, rl);

        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1, -2);
        sl.topMargin = dp(10);
        box.addView(ModuleCard.slider(act, "Button size (dp)", 36, 96, sm.getSizeDp(), 2, accent,
                n -> String.valueOf(n), n -> sm.setSizeDp(n)), sl);
        box.addView(ModuleCard.slider(act, "Opacity (%)", 20, 100, sm.getOpacity(), 5, accent,
                n -> String.valueOf(n), n -> sm.setOpacity(n)), sl);

        TextView clear = ModuleCard.text(act, "Remove all shortcuts", 13, Color.WHITE);
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(16));
        g.setStroke(dp(1), accent);
        g.setColor(0x22FFFFFF);
        clear.setBackground(g);
        clear.setOnClickListener(v -> sm.clear());
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-1, -2);
        cl.topMargin = dp(16);
        box.addView(clear, cl);

        content.addView(box, new LinearLayout.LayoutParams(-1, -2));
    }
}
