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
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Attacks nearby entities automatically.
 * With the native bridge: picks real targets inside Range. Without it: falls back to tapping the
 * crosshair at the configured CPS (like the Auto Clicker).
 */
public class KillAura extends Module {
    private final EnumValue mode = add(new EnumValue("Mode", "Single", "Single", "Switch", "Multi"));
    private final EnumValue targets = add(new EnumValue("Targets", "Players", "Players", "Mobs", "All"));
    private final EnumValue priority = add(new EnumValue("Priority", "Distance", "Distance", "Angle", "Health"));
    private final FloatValue range = add(new FloatValue("Range", 3.5f, 1f, 8f, 0.1f));
    private final IntValue cps = add(new IntValue("CPS", 10, 1, 20));
    private final IntValue jitter = add(new IntValue("Jitter", 15, 0, 50, 5));
    private final IntValue maxTargets = add(new IntValue("Max Targets", 3, 1, 6), () -> mode.is("Multi"));
    private final BoolValue onlyInFov = add(new BoolValue("Only In FOV", false));

    private final Random rng = new Random();
    private long nextAttack;
    private int switchIndex;
    private String info = "";

    public KillAura() {
        super("Killaura", "Automatically attacks nearby targets", Category.COMBAT);
    }

    @Override public String getInfo() { return info; }

    @Override protected void onEnable() { nextAttack = 0; switchIndex = 0; }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (ModuleManager.get().guiOpen) return;
        long now = System.currentTimeMillis();
        if (now < nextAttack) return;
        double base = 1000.0 / cps.get();
        double j = base * (jitter.get() / 100.0) * (rng.nextDouble() * 2 - 1);
        nextAttack = now + Math.max(20L, (long) (base + j));

        if (!game.isAvailable()) {
            info = "tap";
            input.tapCenter();
            return;
        }

        float[] raw = game.nearbyEntities(range.get());
        List<float[]> list = new ArrayList<>();
        for (int i = 0; i + 8 <= raw.length; i += 8) {
            float[] e = java.util.Arrays.copyOfRange(raw, i, i + 8);
            int type = (int) e[1];
            if (type == GameBridge.TYPE_ITEM) continue;
            if (targets.is("Players") && type != GameBridge.TYPE_PLAYER) continue;
            if (targets.is("Mobs") && type != GameBridge.TYPE_MOB) continue;
            if (e[5] > range.get()) continue;
            if (onlyInFov.get() && Math.abs(e[7]) > 60f) continue;
            list.add(e);
        }
        if (list.isEmpty()) { info = ""; return; }

        Collections.sort(list, (a, b) -> {
            if (priority.is("Angle")) return Float.compare(Math.abs(a[7]), Math.abs(b[7]));
            if (priority.is("Health")) return Float.compare(a[6], b[6]);
            return Float.compare(a[5], b[5]);
        });

        if (mode.is("Multi")) {
            int n = Math.min(maxTargets.get(), list.size());
            for (int i = 0; i < n; i++) game.attack((long) list.get(i)[0]);
            info = n + " targets";
        } else if (mode.is("Switch")) {
            switchIndex = (switchIndex + 1) % list.size();
            game.attack((long) list.get(switchIndex)[0]);
            info = "switch";
        } else {
            game.attack((long) list.get(0)[0]);
            info = String.format(java.util.Locale.US, "%.1fm", list.get(0)[5]);
        }
    }
}
