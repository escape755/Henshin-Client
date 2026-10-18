package com.origin.launcher.Launcher.henshin.value;

public class IntValue extends Value<Integer> {
    public final int min, max, step;
    public IntValue(String name, int def, int min, int max) { this(name, def, min, max, 1); }
    public IntValue(String name, int def, int min, int max, int step) {
        super(name, Math.max(min, Math.min(max, def)));
        this.min = min; this.max = max; this.step = Math.max(1, step);
    }
    @Override protected Integer sanitize(Integer v) {
        return v == null ? null : Math.max(min, Math.min(max, v));
    }
    @Override public String serialize() { return Integer.toString(value); }
    @Override public boolean deserialize(String s) {
        try { set(Integer.parseInt(s.trim())); return true; } catch (Exception e) { return false; }
    }
}
