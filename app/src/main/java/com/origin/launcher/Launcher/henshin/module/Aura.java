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
 * Port of the desktop KillAuraX. Attacks every target in range on a tick interval.
 * Single = closest target only, Multi = every target in range.
 */
public class Aura extends Module {
    private final FloatValue range = add(new FloatValue("Range", 5f, 3f, 20f, 0.1f));
    private final IntValue interval = add(new IntValue("Interval", 1, 0, 20));
    private final EnumValue rotations = add(new EnumValue("Rotations", "Normal", "None", "Normal", "Smooth"));
    private final IntValue rotSpeed = add(new IntValue("Rot Speed", 90, 10, 180, 5), () -> rotations.is("Smooth"));
    private final EnumValue hitType = add(new EnumValue("HitType", "Single", "Single", "Multi"));
    private final IntValue hitChance = add(new IntValue("HitChance", 100, 0, 100, 5));
    private final IntValue hitAttempts = add(new IntValue("Hit Attempts", 1, 1, 10));
    private final BoolValue mobs = add(new BoolValue("Mobs", false));
    private final BoolValue swingSync = add(new BoolValue("Swing Sync", true));

    private static final float EYE = 1.62f;
    private final Random rng = new Random();
    private int sinceAttack;
    private float smoothYaw;
    private boolean hadTarget;
    private String info = "";

    public Aura() { super("Aura", "Attacks entities around you", Category.COMBAT); }

    @Override public boolean requiresBridge() { return true; }
    @Override public String getInfo() { return hitType.get() + (info.isEmpty() ? "" : " " + info); }

    @Override protected void onEnable() { sinceAttack = 99; hadTarget = false; info = ""; }

    private static float wrap(float a) {
        while (a < -180f) a += 360f;
        while (a > 180f) a -= 360f;
        return a;
    }

    @Override
    public void onTick(GameBridge game, InputInjector input) {
        if (ModuleManager.get().guiOpen) return;
        if (!game.isAvailable()) {
            // No entity list: all we can do is hit whatever is under the crosshair, on the same interval.
            info = "tap";
            if (++sinceAttack >= Math.max(1, interval.get())) {
                sinceAttack = 0;
                if (rng.nextInt(100) < hitChance.get()) input.tapCenter();
            }
            return;
        }
        float[] st = game.playerState();
        if (st == null || st.length < 5) return;

        float[] raw = game.nearbyEntities(range.get());
        List<float[]> list = new ArrayList<>();
        for (int i = 0; i + 8 <= raw.length; i += 8) {
            int type = (int) raw[i + 1];
            if (type == GameBridge.TYPE_ITEM) continue;
            if (type == GameBridge.TYPE_MOB && !mobs.get()) continue;
            if (raw[i + 5] > range.get()) continue;
            list.add(java.util.Arrays.copyOfRange(raw, i, i + 8));
        }
        if (list.isEmpty()) { hadTarget = false; info = ""; return; }
        Collections.sort(list, (a, b) -> Float.compare(a[5], b[5]));
        float[] t = list.get(0);

        // ---- rotation towards the closest target
        if (!rotations.is("None")) {
            double dx = t[2] - st[0], dz = t[4] - st[2];
            double dy = (t[3] + 0.9) - (st[1] + EYE); // aim at the middle of the body
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) Math.toDegrees(Math.atan2(-dy, Math.hypot(dx, dz)));
            if (rotations.is("Smooth")) {
                if (!hadTarget) smoothYaw = st[3];
                smoothYaw += wrap(yaw - smoothYaw) * (0.01f * rotSpeed.get());
                yaw = smoothYaw;
            }
            game.setRotation(yaw, pitch);
        }
        hadTarget = true;

        // ---- attack on interval
        if (++sinceAttack < Math.max(1, interval.get())) return;
        sinceAttack = 0;

        int n = hitType.is("Multi") ? list.size() : 1;
        for (int i = 0; i < n; i++) {
            if (rng.nextInt(100) >= hitChance.get()) continue;
            for (int k = 0; k < hitAttempts.get(); k++) game.attack((long) list.get(i)[0]);
            if (swingSync.get()) game.swing();
        }
        info = n + (n == 1 ? " target" : " targets");
    }
}
