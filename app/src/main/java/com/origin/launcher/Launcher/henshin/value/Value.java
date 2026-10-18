package com.origin.launcher.Launcher.henshin.value;

import java.util.function.BooleanSupplier;

/** Base class of every configurable setting of a module. Serialised as a plain string. */
public abstract class Value<T> {
    public final String name;
    protected T value;
    private BooleanSupplier visible = () -> true;
    private Runnable onChange;

    protected Value(String name, T def) {
        this.name = name;
        this.value = def;
    }

    public T get() { return value; }

    public void set(T v) {
        T n = sanitize(v);
        if (n == null || n.equals(value)) return;
        value = n;
        if (onChange != null) onChange.run();
    }

    /** Clamp / validate. Return null to reject the change. */
    protected T sanitize(T v) { return v; }

    public abstract String serialize();

    /** @return true when the string was valid. */
    public abstract boolean deserialize(String s);

    public Value<T> visibleWhen(BooleanSupplier s) { this.visible = s; return this; }
    public boolean isVisible() { return visible.getAsBoolean(); }
    public void setOnChange(Runnable r) { this.onChange = r; }
}
