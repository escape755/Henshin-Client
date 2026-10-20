package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.bridge.NativeFx;
import com.origin.launcher.Launcher.henshin.core.Category;

/** Maximum light level everywhere (memory patch ported from BedrockTools). */
public class Fullbright extends NativeFxModule {
    public Fullbright() { super("Fullbright", "Removes darkness by maxing the light level everywhere", Category.RENDER); }
    @Override protected boolean apply(boolean on) { return NativeFx.fullbright(on); }
    @Override protected String state() { return ""; }
}
