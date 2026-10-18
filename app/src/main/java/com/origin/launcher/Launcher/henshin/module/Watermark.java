package com.origin.launcher.Launcher.henshin.module;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.value.BoolValue;
import com.origin.launcher.Launcher.henshin.value.EnumValue;
import com.origin.launcher.Launcher.henshin.value.IntValue;
import com.origin.launcher.Launcher.henshin.value.TextValue;

/**
 * Port of Abysm's WaterMark HUD. It is a plain View drawn by the launcher on top of the game,
 * so it needs neither the packet relay nor the native bridge.
 */
public class Watermark extends Module {
    private final TextValue text = add(new TextValue("Text", "Henshin", 24));
    private final BoolValue showVersion = add(new BoolValue("Show Version", true));
    private final EnumValue position = add(new EnumValue("Position", "Top Left",
            "Top Left", "Top Center", "Top Right",
            "Center Left", "Center", "Center Right",
            "Bottom Left", "Bottom Center", "Bottom Right"));
    private final IntValue fontSize = add(new IntValue("Font Size", 24, 12, 36));
    private final EnumValue mode = add(new EnumValue("Mode", "RGB", "RGB", "Static"));
    private final EnumValue font = add(new EnumValue("Font", "Bold", "Bold", "Normal", "Mono"));

    private TextView view;
    private String version;
    private String lastShown = "";
    private int lastHue = -1;
    private int lastGravity = -1;

    public Watermark() { super("Watermark", "Shows a custom text on screen (Abysm port)", Category.RENDER); }

    @Override public String getInfo() { return mode.get(); }

    @Override protected void onEnable() {
        Activity a = ModuleManager.get().getActivity();
        if (a == null) return;
        if (version == null) {
            try { version = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName; }
            catch (Throwable t) { version = ""; }
        }
        a.runOnUiThread(() -> {
            ViewGroup root = a.findViewById(android.R.id.content);
            if (root == null || view != null) return;
            view = new TextView(a);
            view.setClickable(false);
            view.setFocusable(false);
            view.setShadowLayer(4f, 1f, 1f, Color.BLACK);
            float d = a.getResources().getDisplayMetrics().density;
            int m = Math.round(12 * d);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(m, m, m, m);
            root.addView(view, lp);
            lastShown = ""; lastHue = -1; lastGravity = -1;
            refresh();
        });
    }

    @Override protected void onDisable() {
        Activity a = ModuleManager.get().getActivity();
        final TextView v = view;
        view = null;
        if (v == null) return;
        Runnable r = () -> {
            ViewGroup p = (ViewGroup) v.getParent();
            if (p != null) p.removeView(v);
        };
        if (a != null) a.runOnUiThread(r); else r.run();
    }

    @Override public void onTick(GameBridge game, InputInjector input) { refresh(); }

    private static int gravityOf(String p) {
        int v = p.startsWith("Top") ? Gravity.TOP : p.startsWith("Bottom") ? Gravity.BOTTOM : Gravity.CENTER_VERTICAL;
        int h = p.endsWith("Left") ? Gravity.START : p.endsWith("Right") ? Gravity.END : Gravity.CENTER_HORIZONTAL;
        if (p.equals("Center")) return Gravity.CENTER;
        return v | h;
    }

    private void refresh() {
        TextView v = view;
        if (v == null) return;
        String s = text.get();
        if (showVersion.get() && version != null && !version.isEmpty()) s += " v" + version;

        int hue = mode.is("RGB") ? (int) ((System.currentTimeMillis() / 12) % 360) : 0;
        if (!s.equals(lastShown) || hue != lastHue) {
            lastShown = s; lastHue = hue;
            SpannableString sp = new SpannableString(s);
            if (mode.is("RGB")) {
                for (int i = 0; i < s.length(); i++) {
                    int c = Color.HSVToColor(new float[]{(hue + i * 18) % 360, 0.85f, 1f});
                    sp.setSpan(new ForegroundColorSpan(c), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else {
                sp.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            v.setText(sp);
        }
        v.setTextSize(fontSize.get());
        v.setTypeface(font.is("Mono") ? Typeface.MONOSPACE : Typeface.DEFAULT,
                font.is("Bold") ? Typeface.BOLD : Typeface.NORMAL);
        int g = gravityOf(position.get());
        if (g != lastGravity) {
            lastGravity = g;
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) lp).gravity = g;
                v.setLayoutParams(lp);
            }
        }
    }
}
