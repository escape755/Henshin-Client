package com.origin.launcher.Launcher.henshin.bridge;

/**
 * Access to live game state (player, entities, packets). Implemented natively; when the native
 * side is missing every method is a safe no-op and {@link #isAvailable()} is false, so modules
 * can fall back to input-based behaviour.
 */
public interface GameBridge {
    int TYPE_PLAYER = 0, TYPE_MOB = 1, TYPE_ITEM = 2;

    boolean isAvailable();

    /** {x, y, z, yaw, pitch} or null. */
    float[] playerState();

    /** Flat array, 8 floats per entity: id, type, x, y, z, distance, health, yawToEntity. */
    float[] nearbyEntities(float range);

    boolean attack(long entityId);
    boolean collidingHorizontally();
    void setMotion(float x, float y, float z);
    /** @return 0..20 or -1 when unknown. */
    int hunger();
    boolean selectHotbar(int slot0to8);
    boolean useItem(boolean holding);
    /** style: 0 box, 1 outline, 2 glow. filter: comma separated item names, empty = all. */
    boolean setItemEsp(boolean on, int argb, float range, boolean tracers, int style, String filter);
    /** mode: 0 ghost, 1 lag, 2 freeze. */
    boolean setDesync(boolean on, int mode, int delayMs);
}
