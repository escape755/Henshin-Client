package com.origin.launcher.Launcher.henshin.value;

import java.util.Arrays;
import java.util.List;

/** One choice out of a fixed list of strings. */
public class EnumValue extends Value<String> {
    public final List<String> options;
    public EnumValue(String name, String def, String... options) {
        super(name, def);
        this.options = Arrays.asList(options);
        if (!this.options.contains(def)) value = this.options.get(0);
    }
    @Override protected String sanitize(String v) {
        if (v == null) return null;
        for (String o : options) if (o.equalsIgnoreCase(v)) return o;
        return null;
    }
    public int index() { return options.indexOf(value); }
    public void next() { set(options.get((index() + 1) % options.size())); }
    public boolean is(String o) { return value.equalsIgnoreCase(o); }
    @Override public String serialize() { return value; }
    @Override public boolean deserialize(String s) {
        String n = sanitize(s);
        if (n == null) return false;
        set(n);
        return true;
    }
}
