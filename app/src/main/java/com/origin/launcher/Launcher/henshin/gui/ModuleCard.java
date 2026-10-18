package com.origin.launcher.Launcher.henshin.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.materialswitch.MaterialSwitch;
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

import java.util.Locale;

/**
 * One expandable module row (name + switch + gear). Used by both ClickGUI styles so a module
 * looks and behaves the same everywhere. Expanded it shows every Value plus Keybind and Shortcut.
 */
public final class ModuleCard {
    private ModuleCard() {}

    public static View build(Activity act, Module m, int accent) {
        float d = act.getResources().getDisplayMetrics().density;
        LinearLayout card = new LinearLayout(act);
        card.setOrientation(LinearLayout.VERTICAL);

        LinearLayout main = new LinearLayout(act);
        main.setOrientation(LinearLayout.HORIZONTAL);
        main.setGravity(Gravity.CENTER_VERTICAL);
        main.setPadding(dp(d, 14), dp(d, 2), dp(d, 10), dp(d, 2));
        main.setMinimumHeight(dp(d, 48));
        card.addView(main, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout names = new LinearLayout(act);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(act, m.name, 15, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        names.addView(title);
        final TextView sub = text(act, m.description, 10, 0xFFAAAAAA);
        names.addView(sub);
        main.addView(names, new LinearLayout.LayoutParams(0, -2, 1f));

        final LinearLayout settings = new LinearLayout(act);
        settings.setOrientation(LinearLayout.VERTICAL);
        settings.setBackgroundColor(0x40000000);
        settings.setPadding(dp(d, 14), dp(d, 6), dp(d, 14), dp(d, 10));
        settings.setVisibility(View.GONE);
        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> fill(act, settings, m, accent, rebuild[0]);
        rebuild[0].run();

        final TextView gear = text(act, "⚙", 20, Color.WHITE); // gear glyph, no drawable needed
        gear.setGravity(Gravity.CENTER);
        gear.setAlpha(0.75f);
        gear.setOnClickListener(v -> {
            boolean open = settings.getVisibility() == View.VISIBLE;
            settings.setVisibility(open ? View.GONE : View.VISIBLE);
            gear.setTextColor(open ? Color.WHITE : accent);
            gear.setAlpha(open ? 0.75f : 1f);
        });
        main.addView(gear, new LinearLayout.LayoutParams(dp(d, 36), dp(d, 36)));

        MaterialSwitch sw = newSwitch(act);
        sw.setChecked(m.isEnabled());
        style(sw, m.isEnabled(), accent);
        // Keep this switch in sync when the module is toggled elsewhere (key bind, shortcut, command).
        Runnable sync = () -> {
            if (sw.isChecked() != m.isEnabled()) sw.setChecked(m.isEnabled());
            String info = m.getInfo();
            sub.setText(info.isEmpty() ? m.description : m.description + "  •  " + info);
        };
        sw.setOnCheckedChangeListener((b, c) -> {
            if (m.isEnabled() != c) m.setEnabled(c);
            style(sw, m.isEnabled(), accent);
        });
        ModuleManager.get().addToggleListener(sync);
        card.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) { ModuleManager.get().removeToggleListener(sync); }
        });
        main.addView(sw, new LinearLayout.LayoutParams(-2, -2));
        main.setOnClickListener(v -> sw.toggle());

        card.addView(settings, new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    // ------------------------------------------------------------------ settings body

    private static void fill(Activity act, LinearLayout box, Module m, int accent, Runnable rebuild) {
        box.removeAllViews();
        float d = act.getResources().getDisplayMetrics().density;
        for (Value<?> v : m.getValues()) {
            if (!v.isVisible()) continue;
            View row = null;
            if (v instanceof BoolValue) row = boolRow(act, (BoolValue) v, accent, rebuild);
            else if (v instanceof IntValue) row = intRow(act, (IntValue) v, accent);
            else if (v instanceof FloatValue) row = floatRow(act, (FloatValue) v, accent);
            else if (v instanceof EnumValue) row = enumRow(act, (EnumValue) v, accent, rebuild);
            else if (v instanceof ColorValue) row = colorRow(act, (ColorValue) v, accent, rebuild);
            else if (v instanceof TextValue) row = textRow(act, (TextValue) v, rebuild);
            if (row != null) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.topMargin = dp(d, 6);
                box.addView(row, lp);
            }
        }
        // Keybind + Shortcut
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(d, 10);
        box.addView(buttonRow(act, "Keybind", ModuleManager.keyName(m.getKeyCode()), accent, v -> {
            input(act, "Keybind for " + m.name, "Z, F5, RSHIFT...  (none = remove)",
                    ModuleManager.keyName(m.getKeyCode()), InputType.TYPE_CLASS_TEXT, s -> {
                        if (s.equalsIgnoreCase("none") || s.isEmpty()) { ModuleManager.get().setBind(m, 0); }
                        else {
                            int k = ModuleManager.parseKey(s);
                            if (k == 0) ModuleManager.get().toast("Unknown key: " + s);
                            else ModuleManager.get().setBind(m, k);
                        }
                        rebuild.run();
                    });
        }), lp);
        ShortcutManager sc = ShortcutManager.get();
        box.addView(buttonRow(act, "Floating shortcut", sc.has(m) ? "Remove" : "Add", accent, v -> {
            if (sc.has(m)) sc.remove(m); else sc.add(m);
            rebuild.run();
        }), lp);
    }

    private static View boolRow(Activity act, BoolValue v, int accent, Runnable rebuild) {
        LinearLayout row = new LinearLayout(act);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(text(act, v.name, 12, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1f));
        MaterialSwitch sw = newSwitch(act);
        sw.setChecked(v.get());
        style(sw, v.get(), accent);
        sw.setOnCheckedChangeListener((b, c) -> { v.set(c); style(sw, c, accent); rebuild.run(); });
        row.addView(sw);
        return row;
    }

    private static View intRow(Activity act, IntValue v, int accent) {
        return slider(act, v.name, v.min, v.max, v.get(), v.step, accent,
                n -> String.valueOf(n), n -> v.set(n));
    }

    private static View floatRow(Activity act, FloatValue v, int accent) {
        // Slider works on integer "ticks" of size step.
        float step = v.step > 0 ? v.step : 0.1f;
        int min = Math.round(v.min / step), max = Math.round(v.max / step), cur = Math.round(v.get() / step);
        return slider(act, v.name, min, max, cur, 1, accent,
                n -> String.format(Locale.US, "%.2f", n * step), n -> v.set(n * step));
    }

    interface IntFmt { String f(int n); }
    interface IntSink { void put(int n); }

    static View slider(Activity act, String label, int min, int max, int value, int step,
                               int accent, IntFmt fmt, IntSink sink) {
        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(act, label + ": " + fmt.f(value), 12, Color.WHITE);
        row.addView(t);
        SeekBar bar = new SeekBar(act);
        int steps = Math.max(1, (max - min) / step);
        bar.setMax(steps);
        bar.setProgress((value - min) / step);
        bar.setProgressTintList(ColorStateList.valueOf(accent));
        bar.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(0x66FFFFFF));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                int n = min + p * step;
                t.setText(label + ": " + fmt.f(n));
                if (user) sink.put(n);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        row.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private static View enumRow(Activity act, EnumValue v, int accent, Runnable rebuild) {
        return buttonRow(act, v.name, v.get(), accent, x -> { v.next(); rebuild.run(); });
    }

    private static View textRow(Activity act, TextValue v, Runnable rebuild) {
        return buttonRow(act, v.name, v.get().isEmpty() ? "(empty)" : v.get(), 0xFFFFFFFF, x ->
                input(act, v.name, "", v.get(), InputType.TYPE_CLASS_TEXT, s -> { v.set(s); rebuild.run(); }));
    }

    private static View colorRow(Activity act, ColorValue v, int accent, Runnable rebuild) {
        float d = act.getResources().getDisplayMetrics().density;
        LinearLayout col = new LinearLayout(act);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(text(act, v.name + ": " + v.serialize(), 12, Color.WHITE));
        LinearLayout sw = new LinearLayout(act);
        sw.setGravity(Gravity.CENTER_VERTICAL);
        for (int c : ColorValue.PRESETS) {
            View dot = new View(act);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(c);
            g.setStroke(dp(d, c == v.get() ? 3 : 1), c == v.get() ? Color.WHITE : 0x55FFFFFF);
            dot.setBackground(g);
            dot.setOnClickListener(x -> { v.set(c); rebuild.run(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(d, 24), dp(d, 24));
            lp.rightMargin = dp(d, 6);
            lp.topMargin = dp(d, 4);
            sw.addView(dot, lp);
        }
        TextView hex = text(act, "#", 16, Color.WHITE);
        hex.setGravity(Gravity.CENTER);
        hex.setOnClickListener(x -> input(act, v.name + " (#RRGGBB)", "#FFD400", v.serialize(),
                InputType.TYPE_CLASS_TEXT, s -> {
                    if (!v.deserialize(s)) ModuleManager.get().toast("Invalid colour: " + s);
                    rebuild.run();
                }));
        sw.addView(hex, new LinearLayout.LayoutParams(dp(d, 24), dp(d, 24)));
        col.addView(sw);
        return col;
    }

    private static View buttonRow(Activity act, String label, String valueText, int accent, View.OnClickListener l) {
        float d = act.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(act);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(d, 34));
        row.addView(text(act, label, 12, Color.WHITE), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView b = text(act, valueText, 12, Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(d, 12), dp(d, 5), dp(d, 12), dp(d, 5));
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(d, 14));
        g.setColor(0x33FFFFFF);
        g.setStroke(dp(d, 1), accent);
        b.setBackground(g);
        b.setOnClickListener(l);
        row.addView(b, new LinearLayout.LayoutParams(-2, -2));
        row.setOnClickListener(l);
        return row;
    }

    // ------------------------------------------------------------------ helpers

    private interface Sink { void put(String s); }

    private static void input(Activity act, String title, String hint, String current, int type, Sink sink) {
        EditText et = new EditText(act);
        et.setInputType(type);
        et.setHint(hint);
        et.setText(current);
        et.setSelection(et.getText().length());
        et.setSingleLine(true);
        new AlertDialog.Builder(act)
                .setTitle(title)
                .setView(et)
                .setPositiveButton("OK", (dlg, w) -> sink.put(et.getText().toString().trim()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        return t;
    }

    static MaterialSwitch newSwitch(Activity act) {
        Context themed = new ContextThemeWrapper(act, com.google.android.material.R.style.Theme_Material3_Dark_NoActionBar);
        MaterialSwitch sw = new MaterialSwitch(themed);
        sw.setShowText(false);
        return sw;
    }

    static void style(MaterialSwitch sw, boolean on, int accent) {
        sw.setThumbTintList(ColorStateList.valueOf(on ? Color.WHITE : 0xFFE0E0E0));
        sw.setTrackTintList(ColorStateList.valueOf(on ? accent : 0x66FFFFFF));
    }

    static int dp(float d, float v) { return (int) (v * d + 0.5f); }
}
