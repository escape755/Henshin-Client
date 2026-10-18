package com.origin.launcher.Launcher.henshin.value;

public class TextValue extends Value<String> {
    public final int maxLength;
    public TextValue(String name, String def) { this(name, def, 64); }
    public TextValue(String name, String def, int maxLength) {
        super(name, def == null ? "" : def);
        this.maxLength = maxLength;
    }
    @Override protected String sanitize(String v) {
        if (v == null) return null;
        return v.length() > maxLength ? v.substring(0, maxLength) : v;
    }
    @Override public String serialize() { return value; }
    @Override public boolean deserialize(String s) { set(s == null ? "" : s); return true; }
}
