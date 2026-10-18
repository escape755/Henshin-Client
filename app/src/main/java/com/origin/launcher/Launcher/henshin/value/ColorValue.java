package com.origin.launcher.Launcher.henshin.value;

import android.graphics.Color;

/** ARGB colour, edited in the GUI with a palette of presets or typed as #RRGGBB. */
public class ColorValue extends Value<Integer> {
    public static final int[] PRESETS = {
            0xFFFF1A00, 0xFFFF8A00, 0xFFFFD400, 0xFF3DDC84,
            0xFF00C8FF, 0xFF3D6BFF, 0xFFB44CFF, 0xFFFFFFFF
    };
    public ColorValue(String name, int argb) { super(name, argb); }
    @Override public String serialize() { return String.format("#%08X", value); }
    @Override public boolean deserialize(String s) {
        try {
            String t = s.trim();
            if (!t.startsWith("#")) t = "#" + t;
            int c = Color.parseColor(t);
            if (t.length() == 7) c |= 0xFF000000;
            set(c);
            return true;
        } catch (Exception e) { return false; }
    }
}
