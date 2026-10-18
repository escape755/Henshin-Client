package com.origin.launcher.Launcher.henshin.value;

import java.util.Locale;

public class FloatValue extends Value<Float> {
    public final float min, max, step;
    public FloatValue(String name, float def, float min, float max) { this(name, def, min, max, 0.1f); }
    public FloatValue(String name, float def, float min, float max, float step) {
        super(name, Math.max(min, Math.min(max, def)));
        this.min = min; this.max = max; this.step = step;
    }
    @Override protected Float sanitize(Float v) {
        if (v == null || v.isNaN()) return null;
        float c = Math.max(min, Math.min(max, v));
        return step > 0 ? Math.round(c / step) * step : c;
    }
    @Override public String serialize() { return String.format(Locale.US, "%.3f", value); }
    @Override public boolean deserialize(String s) {
        try { set(Float.parseFloat(s.trim())); return true; } catch (Exception e) { return false; }
    }
}
