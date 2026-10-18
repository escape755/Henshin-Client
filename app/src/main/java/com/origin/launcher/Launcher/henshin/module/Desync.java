package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.value.BoolValue;
import com.origin.launcher.Launcher.henshin.value.EnumValue;
import com.origin.launcher.Launcher.henshin.value.IntValue;

/** Holds back movement packets so your client position drifts away from what the server sees. */
public class Desync extends Module {
    private final EnumValue mode = add(new EnumValue("Mode", "Lag", "Ghost", "Lag", "Freeze"));
    private final IntValue delay = add(new IntValue("Delay", 400, 50, 2000, 50));
    private final BoolValue autoOff = add(new BoolValue("Auto Off", true));
    private final IntValue maxSeconds = add(new IntValue("Max Seconds", 5, 1, 30), () -> autoOff.get());

    private long startedAt;
    private int ticks;

    public Desync() { super("Desync", "Desynchronises your position from the server", Category.MISC); }

    @Override public boolean requiresBridge() { return true; }
    @Override public String getInfo() { return mode.get(); }

    private void push(GameBridge g, boolean on) { g.setDesync(on, mode.index(), delay.get()); }

    @Override protected void onEnable() {
        startedAt = System.currentTimeMillis();
        ticks = 0;
        GameBridge g = ModuleManager.get().getBridge();
        if (!g.isAvailable()) {
            ModuleManager.get().toast("Desync needs the native bridge (not loaded)");
            // Disable on the next loop iteration, after setEnabled() has finished.
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> setEnabled(false));
            return;
        }
        push(g, true);
    }

    @Override protected void onDisable() { push(ModuleManager.get().getBridge(), false); }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (autoOff.get() && System.currentTimeMillis() - startedAt > maxSeconds.get() * 1000L) {
            ModuleManager.get().toast("Desync finished");
            setEnabled(false);
            return;
        }
        if (++ticks % 10 == 0) push(game, true); // apply GUI changes live
    }
}
