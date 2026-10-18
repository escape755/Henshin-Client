package com.origin.launcher.Launcher.henshin.module;

import android.os.Handler;
import android.os.Looper;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.value.BoolValue;
import com.origin.launcher.Launcher.henshin.value.EnumValue;
import com.origin.launcher.Launcher.henshin.value.IntValue;

/**
 * Eats by itself: selects the food slot, holds "use" until done, then returns to your slot.
 * Smart mode reads hunger from the native bridge; Timer mode (and Smart without the bridge)
 * eats every N seconds.
 */
public class AutoEat extends Module {
    private final EnumValue mode = add(new EnumValue("Mode", "Smart", "Smart", "Timer"));
    private final IntValue hunger = add(new IntValue("Hunger", 14, 1, 19), () -> mode.is("Smart"));
    private final IntValue interval = add(new IntValue("Interval", 60, 10, 300, 5));
    private final IntValue foodSlot = add(new IntValue("Food Slot", 2, 1, 9));
    private final IntValue returnSlot = add(new IntValue("Return Slot", 1, 1, 9));
    private final IntValue holdMs = add(new IntValue("Hold Ms", 1700, 800, 3500, 100));
    private final BoolValue swapBack = add(new BoolValue("Swap Back", true));

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean eating;
    private long lastEat;

    public AutoEat() { super("AutoEat", "Eats automatically when hungry", Category.PLAYER); }

    @Override public String getInfo() { return mode.get(); }
    @Override protected void onEnable() { lastEat = System.currentTimeMillis(); eating = false; }
    @Override protected void onDisable() { handler.removeCallbacksAndMessages(null); eating = false; }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (eating || input.isHolding() || ModuleManager.get().guiOpen) return;
        long now = System.currentTimeMillis();
        boolean hungry;
        int h = mode.is("Smart") ? game.hunger() : -1;
        if (h >= 0) hungry = h <= hunger.get() && now - lastEat > 3000;
        else hungry = now - lastEat >= interval.get() * 1000L;
        if (!hungry) return;

        eating = true;
        lastEat = now;
        if (!game.selectHotbar(foodSlot.get() - 1)) input.hotbar(foodSlot.get() - 1);
        handler.postDelayed(() -> {
            if (!isEnabled()) { eating = false; return; }
            if (game.isAvailable() && game.useItem(true)) {
                handler.postDelayed(() -> { game.useItem(false); finish(game, input); }, holdMs.get());
            } else {
                input.holdCenter(holdMs.get(), () -> finish(game, input));
            }
        }, 150);
    }

    private void finish(GameBridge game, InputInjector input) {
        if (swapBack.get() && !(game.isAvailable() && game.selectHotbar(returnSlot.get() - 1))) {
            input.hotbar(returnSlot.get() - 1);
        }
        eating = false;
    }
}
