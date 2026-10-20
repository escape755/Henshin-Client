package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.bridge.NativeFx;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.value.IntValue;

/** Forces a client-side time of day (ported from BedrockTools). */
public class TimeChanger extends NativeFxModule {
    private final IntValue time = add(new IntValue("Time", 18000, 0, 24000, 500));

    public TimeChanger() { super("Time Changer", "Sets a custom time of day (0 sunrise, 6000 noon, 12000 sunset, 18000 midnight)", Category.PLAYER); }
    @Override protected boolean apply(boolean on) { return NativeFx.time(on, time.get()); }
    @Override protected String state() { return Integer.toString(time.get()); }
    @Override public String getInfo() { return super.getInfo().isEmpty() ? Integer.toString(time.get()) : super.getInfo(); }
}
