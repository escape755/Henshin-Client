package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.bridge.NativeFx;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.value.EnumValue;

/** Forces clear / rain / thunder / snow locally (ported from BedrockTools). */
public class WeatherChanger extends NativeFxModule {
    private final EnumValue mode = add(new EnumValue("Mode", "Clear", "Clear", "Rain", "Thunder", "Snow"));

    public WeatherChanger() { super("Weather Changer", "Forces the weather on your screen only", Category.PLAYER); }
    @Override protected boolean apply(boolean on) { return NativeFx.weather(on, mode.index()); }
    @Override protected String state() { return mode.get(); }
    @Override public String getInfo() { return super.getInfo().isEmpty() ? mode.get() : super.getInfo(); }
}
