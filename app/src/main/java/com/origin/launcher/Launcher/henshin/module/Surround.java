package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.bridge.GameBridge;
import com.origin.launcher.Launcher.henshin.bridge.InputInjector;
import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.value.BoolValue;
import com.origin.launcher.Launcher.henshin.value.EnumValue;
import com.origin.launcher.Launcher.henshin.value.FloatValue;
import com.origin.launcher.Launcher.henshin.value.IntValue;

import java.util.ArrayList;
import java.util.List;

import android.view.KeyEvent;

/**
 * Surrounds your feet with obsidian: the four side blocks plus the one under you.
 * Simplified port of the desktop Surround (1x1 only: no Dynamic, Center, Crystal or E-Chest logic).
 */
public class Surround extends Module {
    /** Item id of obsidian, resolved by the native side. */
    private static final int OBSIDIAN = 49;

    private final IntValue placeDelay = add(new IntValue("Place Delay", 1, 0, 20));
    private final IntValue perTick = add(new IntValue("Blocks Per Tick", 1, 1, 8));
    private final BoolValue autoDisable = add(new BoolValue("Auto Disable", false));
    private final BoolValue jumpDisable = add(new BoolValue("Jump Disable", true));
    private final EnumValue switchMode = add(new EnumValue("Switch", "SwitchBack", "None", "Normal", "SwitchBack"));

    private int delayTicks;
    private boolean placedSomething;
    private String info = "";

    public Surround() { super("Surround", "Surrounds you in blocks to prevent massive crystal damage", Category.COMBAT); }

    @Override public boolean requiresBridge() { return true; }
    @Override public String getInfo() { return info; }
    @Override protected void onEnable() {
        if (failWithoutBridge()) return;
        delayTicks = 0; placedSomething = false; info = "";
    }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        ModuleManager mm = ModuleManager.get();
        if (!game.isAvailable() || mm.guiOpen) return;
        if (jumpDisable.get() && mm.isKeyDown(KeyEvent.KEYCODE_SPACE)) { setEnabled(false); return; }

        float[] st = game.playerState();
        if (st == null || st.length < 3) return;
        int px = (int) Math.floor(st[0]), py = (int) Math.floor(st[1] + 0.5f), pz = (int) Math.floor(st[2]);

        // Blocks to fill: 4 sides at feet level (or the block below when the side can't be built on) + under feet.
        List<int[]> todo = new ArrayList<>();
        int[][] sides = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] s : sides) {
            int x = px + s[0], z = pz + s[1];
            if (game.canPlaceBlock(x, py, z)) todo.add(new int[]{x, py, z});
            else if (game.canPlaceBlock(x, py - 1, z)) todo.add(new int[]{x, py - 1, z});
        }
        if (game.canPlaceBlock(px, py - 1, pz)) todo.add(new int[]{px, py - 1, pz});

        if (todo.isEmpty()) {
            info = "done";
            if (placedSomething && autoDisable.get()) setEnabled(false);
            placedSomething = false;
            return;
        }
        info = todo.size() + " left";

        int slot = game.findHotbarItem(OBSIDIAN);
        if (slot < 0) {
            mm.toast("Surround: no obsidian in your hotbar");
            setEnabled(false);
            return;
        }
        if (delayTicks > 0) { delayTicks--; return; }

        int old = game.selectedSlot();
        if (!switchMode.is("None") && old != slot) game.selectHotbar(slot);
        if (switchMode.is("None") && old != slot) return; // holding something else and not allowed to switch

        int placed = 0;
        for (int[] p : todo) {
            if (game.placeBlock(p[0], p[1], p[2])) { placed++; placedSomething = true; }
            if (placed >= perTick.get()) break;
        }
        if (switchMode.is("SwitchBack") && old >= 0 && old != slot) game.selectHotbar(old);
        delayTicks = placeDelay.get();
    }
}
