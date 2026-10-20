package com.origin.launcher.Launcher.inbuilt.gui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;
import com.origin.launcher.Launcher.inbuilt.overlay.InbuiltOverlayManager;
import com.origin.launcher.R;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.HenshinSettings;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.gui.ModuleCard;
import com.origin.launcher.dialogs.InbuiltModsCustomizeDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Henshin ClickGUI (Toolbox "ambient" style).
 * Two full-height translucent panels: categories on the left (selected one is a red pill),
 * modules with switches on the right. The centre stays free so you can keep seeing the game.
 * Categories: Combat, Movement, Render, World, Player, Misc.
 */
public class ClickGuiWindow {

    // Change this one colour to re-skin the whole GUI.
    private static final int ACCENT = Color.parseColor("#FF1A00");

    private static ClickGuiWindow current;

    public static boolean isOpen() {
        return current != null && current.showing;
    }

    public static void toggle(Activity activity) {
        if (isOpen()) {
            current.close(true);
        } else {
            current = new ClickGuiWindow(activity);
            current.open();
        }
    }

    public static void closeIfOpen() {
        if (current != null) current.close(false);
    }

    // ---------------------------------------------------------------------------------------

    private static final int KIND_MODULE = 0;
    private static final int KIND_OPACITY = 1;
    private static final int KIND_EDIT_LAYOUT = 2;
    private static final int KIND_HENSHIN = 3;
    private static final int KIND_GUI_STYLE = 4;

    private static final class Row {
        final int kind;
        final String id;
        final String name;

        Row(int kind, String id, String name) {
            this.kind = kind;
            this.id = id;
            this.name = name;
        }
    }

    private static final String[] CATEGORY_NAMES = {"Combat", "Movement", "Render", "World", "Player", "Misc"};
    private static final int[] CATEGORY_ICONS = {
            R.drawable.ic_autoclicker, R.drawable.ic_sprint, R.drawable.ic_camera,
            R.drawable.ic_cat_world, R.drawable.ic_cat_player, R.drawable.ic_cat_misc
    };

    private static int lastCategory = 0;

    private final Activity activity;
    private final InbuiltModManager modManager;
    private final WindowManager windowManager;
    private final float density;

    private LinearLayout leftPanel;
    private LinearLayout rightPanel;
    private LinearLayout moduleList;
    private ScrollView moduleScroll;
    private LinearLayout[] tabViews;
    private boolean showing = false;

    private ClickGuiWindow(Activity activity) {
        this.activity = activity;
        this.modManager = InbuiltModManager.getInstance(activity);
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        this.density = activity.getResources().getDisplayMetrics().density;
    }

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    // ---------------------------------------------------------------------------------------
    // Open / close
    // ---------------------------------------------------------------------------------------

    private void open() {
        buildUi();

        int screenW = activity.getResources().getDisplayMetrics().widthPixels;
        int leftW = Math.max(dp(140), Math.min(dp(210), (int) (screenW * 0.24f)));
        int rightW = Math.max(dp(160), Math.min(dp(240), (int) (screenW * 0.27f)));

        boolean okLeft = addPanel(leftPanel, leftW, Gravity.START);
        boolean okRight = addPanel(rightPanel, rightW, Gravity.END);
        if (!okLeft || !okRight) {
            removeView(leftPanel);
            removeView(rightPanel);
            return;
        }
        showing = true;
        ModuleManager.get().guiOpen = true;

        float alpha = modManager.getModMenuOpacity() / 100f;
        leftPanel.setAlpha(alpha);
        rightPanel.setAlpha(alpha);
        leftPanel.setTranslationX(-leftW);
        rightPanel.setTranslationX(rightW);
        leftPanel.animate().translationX(0).setDuration(180).start();
        rightPanel.animate().translationX(0).setDuration(180).start();
    }

    private void close(boolean animate) {
        if (!showing) return;
        showing = false;
        ModuleManager.get().guiOpen = false;
        if (current == this) current = null;

        if (animate) {
            leftPanel.animate().translationX(-leftPanel.getWidth()).setDuration(150)
                    .withEndAction(() -> removeView(leftPanel)).start();
            rightPanel.animate().translationX(rightPanel.getWidth()).setDuration(150)
                    .withEndAction(() -> removeView(rightPanel)).start();
        } else {
            leftPanel.animate().cancel();
            rightPanel.animate().cancel();
            removeView(leftPanel);
            removeView(rightPanel);
        }
    }

    private boolean addPanel(View view, int width, int horizontalGravity) {
        try {
            WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                    width,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
            );
            p.gravity = Gravity.TOP | horizontalGravity;
            p.token = activity.getWindow().getDecorView().getWindowToken();
            windowManager.addView(view, p);
            return true;
        } catch (Exception e) {
            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content == null) return false;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    width, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.TOP | horizontalGravity);
            content.addView(view, lp);
            return true;
        }
    }

    private void removeView(View view) {
        if (view == null) return;
        try {
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            } else if (view.isAttachedToWindow()) {
                windowManager.removeView(view);
            }
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------------------------------
    // UI construction
    // ---------------------------------------------------------------------------------------

    private void buildUi() {
        // ---- Left: header + categories ----
        leftPanel = new LinearLayout(activity);
        leftPanel.setOrientation(LinearLayout.VERTICAL);
        leftPanel.setBackgroundColor(Color.argb(0x99, 0, 0, 0));
        leftPanel.setClickable(true);

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(4), dp(6), dp(0));

        TextView title = new TextView(activity);
        title.setText("HENSHIN");
        title.setTextColor(Color.WHITE);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.15f);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView closeBtn = new ImageView(activity);
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setColorFilter(Color.WHITE);
        closeBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        closeBtn.setOnClickListener(v -> close(true));
        header.addView(closeBtn, new LinearLayout.LayoutParams(dp(36), dp(36)));

        leftPanel.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        leftPanel.addView(buildCategoryList(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // ---- Right: module list ----
        rightPanel = new LinearLayout(activity);
        rightPanel.setOrientation(LinearLayout.VERTICAL);
        rightPanel.setBackgroundColor(Color.argb(0x73, 0, 0, 0));
        rightPanel.setClickable(true);

        moduleScroll = new ScrollView(activity);
        moduleScroll.setVerticalScrollBarEnabled(false);
        moduleScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        moduleList = new LinearLayout(activity);
        moduleList.setOrientation(LinearLayout.VERTICAL);
        moduleList.setPadding(0, dp(6), 0, dp(10));
        moduleScroll.addView(moduleList, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rightPanel.addView(moduleScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        selectCategory(lastCategory);
    }

    private View buildCategoryList() {
        ScrollView sv = new ScrollView(activity);
        sv.setVerticalScrollBarEnabled(false);
        sv.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(4), 0, dp(8));
        sv.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        tabViews = new LinearLayout[CATEGORY_NAMES.length];
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            final int index = i;
            LinearLayout tab = new LinearLayout(activity);
            tab.setOrientation(LinearLayout.HORIZONTAL);
            tab.setGravity(Gravity.CENTER_VERTICAL);
            tab.setPadding(dp(14), 0, dp(10), 0);

            ImageView icon = new ImageView(activity);
            icon.setImageResource(CATEGORY_ICONS[i]);
            icon.setColorFilter(Color.WHITE);
            tab.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

            TextView label = new TextView(activity);
            label.setText(CATEGORY_NAMES[i]);
            label.setTextColor(Color.WHITE);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            label.setSingleLine(true);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.leftMargin = dp(14);
            tab.addView(label, llp);

            tab.setOnClickListener(v -> selectCategory(index));

            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
            tlp.rightMargin = dp(12);
            tlp.topMargin = dp(2);
            tlp.bottomMargin = dp(2);
            list.addView(tab, tlp);
            tabViews[i] = tab;
        }
        return sv;
    }

    private GradientDrawable pill() {
        float r = dp(26);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(ACCENT);
        d.setCornerRadii(new float[]{0, 0, r, r, r, r, 0, 0});
        return d;
    }

    private void selectCategory(int index) {
        lastCategory = index;
        for (int i = 0; i < tabViews.length; i++) {
            tabViews[i].setBackground(i == index ? pill() : null);
        }
        moduleList.removeAllViews();
        for (Row row : rowsFor(index)) {
            moduleList.addView(buildRow(row), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        moduleScroll.scrollTo(0, 0);
    }

    // ---------------------------------------------------------------------------------------
    // Rows per category
    // ---------------------------------------------------------------------------------------

    private String s(int res) {
        return activity.getString(res);
    }

    /** Toolbox tabs: Combat, Movement, Render, World, Player, Misc -> Henshin module categories. */
    private static Category henshinCategory(int tab) {
        switch (tab) {
            case 0: return Category.COMBAT;
            case 1: return Category.MOVEMENT;
            case 2: return Category.RENDER;
            case 4: return Category.PLAYER;
            case 5: return Category.MISC;
            default: return null;
        }
    }

    private static Row mod(String id, String name) {
        return new Row(KIND_MODULE, id, name);
    }

    private List<Row> rowsFor(int category) {
        List<Row> l = new ArrayList<>();
        Category hc = henshinCategory(category);
        if (hc != null) {
            for (Module m : ModuleManager.get().getModules(hc)) l.add(new Row(KIND_HENSHIN, m.name, m.name));
        }
        switch (category) {
            case 0: // Combat
                l.add(mod(ModIds.AUTO_CLICKER, s(R.string.inbuilt_mod_autoclicker)));
                l.add(mod(ModIds.CPS_DISPLAY, s(R.string.inbuilt_mod_cps_display)));
                l.add(mod(ModIds.THIRD_PERSON_NAMETAG, s(R.string.inbuilt_mod_nametag)));
                break;
            case 1: // Movement
                l.add(mod(ModIds.AUTO_SPRINT, s(R.string.inbuilt_mod_autosprint)));
                l.add(mod(ModIds.CAMERA_PERSPECTIVE, s(R.string.inbuilt_mod_camera)));
                l.add(mod("double_tppview", "2x TPP View"));
                break;
            case 2: // Render
                l.add(mod(ModIds.ZOOM, s(R.string.inbuilt_mod_zoom)));
                l.add(mod("better_brightness", "Better Brightness"));
                l.add(mod("Nohurtcam", "No Hurt Cam"));
                l.add(mod("Nofog", "No Fog"));
                l.add(mod("particles_disabler", "Particles Disabler"));
                l.add(mod("no_shadows", "No Shadows"));
                l.add(mod("no_pumpkin_overlay", "No Pumpkin Overlay"));
                l.add(mod("no_spyglass_overlay", "No Spyglass Overlay"));
                l.add(mod("custom_cross_hair", "Custom CrossHair"));
                l.add(mod("white_block_outline", "White Block Outline"));
                l.add(mod(ModIds.TOGGLE_HUD, s(R.string.inbuilt_mod_hud)));
                break;
            case 3: // World
                l.add(mod("java_clouds", "Java Fancy Clouds"));
                l.add(mod("java_cubemap", "Java Cubemap"));
                l.add(mod("no_flipbook_animations", "No Flipbook Animations"));
                l.add(mod("portal_optimizer", "Portal Optimizer"));
                l.add(mod("psm", "Particle Single Mapping"));
                break;
            case 4: // Player
                l.add(mod(ModIds.QUICK_DROP, s(R.string.inbuilt_mod_quick_drop)));
                l.add(mod("no_eating_animation", "No Eating Animation"));
                l.add(mod("no_bow_animation", "No Bow Animation"));
                l.add(mod("classic_skins", "Classic Vanilla Skins"));
                l.add(mod(ModIds.HOTBAR_ONE, s(R.string.inbuilt_mod_hotbarone)));
                l.add(mod(ModIds.HOTBAR_TWO, s(R.string.inbuilt_mod_hotbartwo)));
                l.add(mod(ModIds.HOTBAR_THREE, s(R.string.inbuilt_mod_hotbarthree)));
                l.add(mod(ModIds.HOTBAR_FOUR, s(R.string.inbuilt_mod_hotbarfour)));
                l.add(mod(ModIds.HOTBAR_FIVE, s(R.string.inbuilt_mod_hotbarfive)));
                l.add(mod(ModIds.HOTBAR_SIX, s(R.string.inbuilt_mod_hotbarsix)));
                l.add(mod(ModIds.HOTBAR_SEVEN, s(R.string.inbuilt_mod_hotbarseven)));
                l.add(mod(ModIds.HOTBAR_EIGHT, s(R.string.inbuilt_mod_hotbareight)));
                l.add(mod(ModIds.HOTBAR_NINE, s(R.string.inbuilt_mod_hotbarnine)));
                break;
            default: // Misc
                l.add(mod(ModIds.FPS_DISPLAY, s(R.string.inbuilt_mod_fps_display)));
                l.add(mod("henshin_title", "Henshin Title"));
                l.add(new Row(KIND_GUI_STYLE, null, "GUI style"));
                l.add(new Row(KIND_OPACITY, null, "GUI opacity"));
                l.add(new Row(KIND_EDIT_LAYOUT, null, "Edit button layout"));
                break;
        }
        return l;
    }

    // ---------------------------------------------------------------------------------------
    // Row views
    // ---------------------------------------------------------------------------------------

    private View buildRow(Row r) {
        switch (r.kind) {
            case KIND_OPACITY:
                return buildOpacityRow(r);
            case KIND_EDIT_LAYOUT:
                return buildEditLayoutRow(r);
            case KIND_GUI_STYLE:
                return buildGuiStyleRow(r);
            case KIND_HENSHIN: {
                Module hm = ModuleManager.get().find(r.id);
                return hm == null ? new View(activity) : ModuleCard.build(activity, hm, HenshinSettings.getAccent(activity));
            }
            default:
                return buildModuleRow(r);
        }
    }

    private LinearLayout newMainRow() {
        LinearLayout main = new LinearLayout(activity);
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setGravity(Gravity.CENTER_VERTICAL);
        main.setPadding(dp(14), dp(2), dp(12), dp(2));
        main.setMinimumHeight(dp(46));
        return main;
    }

    private TextView newRowLabel(String text) {
        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        return t;
    }

    private View buildModuleRow(Row r) {
        final LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout main = newMainRow();
        card.addView(main, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        main.addView(newRowLabel(r.name), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final View settings = buildSettings(r);
        if (settings != null) {
            settings.setVisibility(View.GONE);
            final ImageView gear = new ImageView(activity);
            gear.setImageResource(R.drawable.ic_settings);
            gear.setColorFilter(Color.WHITE);
            gear.setAlpha(0.7f);
            gear.setPadding(dp(7), dp(7), dp(7), dp(7));
            gear.setOnClickListener(v -> {
                boolean open = settings.getVisibility() == View.VISIBLE;
                settings.setVisibility(open ? View.GONE : View.VISIBLE);
                gear.setColorFilter(open ? Color.WHITE : ACCENT);
                gear.setAlpha(open ? 0.7f : 1f);
            });
            main.addView(gear, new LinearLayout.LayoutParams(dp(32), dp(32)));
        }

        final MaterialSwitch sw = newSwitch();
        main.addView(sw, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        if (settings != null) {
            card.addView(settings, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        boolean on = modManager.isModAdded(r.id);
        sw.setChecked(on);
        styleSwitch(sw, on);

        sw.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                modManager.addMod(r.id);
                modManager.applyAllPatches();
            } else {
                modManager.removeAllPatches();
                modManager.removeMod(r.id);
            }
            InbuiltOverlayManager om = InbuiltOverlayManager.getInstance();
            if (om != null) om.showEnabledOverlays();
            styleSwitch(sw, modManager.isModAdded(r.id));
        });

        main.setOnClickListener(v -> sw.toggle());
        return card;
    }

    private View buildOpacityRow(Row r) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(8), dp(14), dp(8));
        box.addView(sliderRow(r.name, 20, 100, modManager.getModMenuOpacity(), "%", v -> {
            modManager.setModMenuOpacity(v);
            float a = v / 100f;
            leftPanel.setAlpha(a);
            rightPanel.setAlpha(a);
        }));
        return box;
    }

    private View buildGuiStyleRow(Row r) {
        LinearLayout main = newMainRow();
        main.addView(newRowLabel(r.name), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView value = newRowLabel(HenshinSettings.getGuiStyle(activity));
        value.setTextColor(ACCENT);
        main.addView(value);
        main.setOnClickListener(v -> {
            boolean tbui = HenshinSettings.GUI_TBUI.equals(HenshinSettings.getGuiStyle(activity));
            String next = tbui ? HenshinSettings.GUI_PANEL : HenshinSettings.GUI_TBUI;
            HenshinSettings.setGuiStyle(activity, next);
            value.setText(next);
            ModuleManager.get().toast("Next time the ClickGUI opens it will use: " + next);
        });
        return main;
    }

    private View buildEditLayoutRow(Row r) {
        LinearLayout main = newMainRow();
        main.addView(newRowLabel(r.name), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ImageView arrow = new ImageView(activity);
        arrow.setImageResource(R.drawable.ic_chevron_right);
        arrow.setColorFilter(Color.WHITE);
        main.addView(arrow, new LinearLayout.LayoutParams(dp(24), dp(24)));
        main.setOnClickListener(v -> {
            close(false);
            new InbuiltModsCustomizeDialog(activity, false).show();
        });
        return main;
    }

    private MaterialSwitch newSwitch() {
        Context themed = new ContextThemeWrapper(
                activity, com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar);
        MaterialSwitch sw = new MaterialSwitch(themed);
        sw.setShowText(false);
        return sw;
    }

    private void styleSwitch(MaterialSwitch sw, boolean on) {
        sw.setThumbTintList(ColorStateList.valueOf(on ? Color.WHITE : Color.parseColor("#E0E0E0")));
        sw.setTrackTintList(ColorStateList.valueOf(on ? ACCENT : Color.parseColor("#66FFFFFF")));
    }

    // ---------------------------------------------------------------------------------------
    // Per-module settings (gear)
    // ---------------------------------------------------------------------------------------

    private View buildSettings(Row r) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.argb(0x40, 0, 0, 0));
        box.setPadding(dp(14), dp(6), dp(14), dp(10));

        switch (r.id) {
            case ModIds.AUTO_CLICKER:
                box.addView(sliderRow("Clicks per second",
                        InbuiltModManager.MIN_AUTOCLICKER_CPS,
                        InbuiltModManager.MAX_AUTOCLICKER_CPS,
                        modManager.getAutoClickerCps(), "",
                        v -> modManager.setAutoClickerCps(v)));
                TextView hint = new TextView(activity);
                hint.setText("Tap the Auto Clicker button in-game to start / stop. It taps the crosshair (screen centre).");
                hint.setTextColor(Color.parseColor("#BBBBBB"));
                hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
                LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                hlp.topMargin = dp(4);
                box.addView(hint, hlp);
                return box;
            case ModIds.ZOOM:
                box.addView(sliderRow("Zoom level", 10, 100, modManager.getZoomLevel(), "%",
                        v -> modManager.setZoomLevel(v)));
                box.addView(switchRow("Hold to zoom", modManager.getZoomHoldMode(),
                        checked -> modManager.setZoomHoldMode(checked)));
                return box;
            default:
                return null;
        }
    }

    private View sliderRow(String label, int min, int max, int value, String unit, IntConsumer onChange) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);

        final TextView text = new TextView(activity);
        text.setTextColor(Color.WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        text.setText(label + ": " + value + unit);
        row.addView(text);

        SeekBar bar = new SeekBar(activity);
        bar.setMax(max - min);
        bar.setProgress(value - min);
        bar.setProgressTintList(ColorStateList.valueOf(ACCENT));
        bar.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#66FFFFFF")));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int v = progress + min;
                text.setText(label + ": " + v + unit);
                if (fromUser) onChange.accept(v);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        row.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View switchRow(String label, boolean checked, Consumer<Boolean> onChange) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView text = new TextView(activity);
        text.setText(label);
        text.setTextColor(Color.WHITE);
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        MaterialSwitch sw = newSwitch();
        sw.setChecked(checked);
        styleSwitch(sw, checked);
        sw.setOnCheckedChangeListener((b, c) -> {
            styleSwitch(sw, c);
            onChange.accept(c);
        });
        row.addView(sw);
        return row;
    }
}
