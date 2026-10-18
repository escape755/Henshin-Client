package com.origin.launcher.Launcher.henshin.value;

public class BoolValue extends Value<Boolean> {
    public BoolValue(String name, boolean def) { super(name, def); }
    @Override public String serialize() { return value ? "1" : "0"; }
    @Override public boolean deserialize(String s) {
        if ("1".equals(s) || "true".equalsIgnoreCase(s)) { set(true); return true; }
        if ("0".equals(s) || "false".equalsIgnoreCase(s)) { set(false); return true; }
        return false;
    }
}
