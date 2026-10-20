package com.origin.launcher.Launcher.henshin.bridge;

/**
 * Access to live game state (player, entities, packets). Implemented natively; when the native
 * side is missing every method is a safe no-op and {@link #isAvailable()} is false, so modules
 * can fall back to input-based behaviour.
 */
public interface GameBridge {
    int TYPE_PLAYER = 0, TYPE_MOB = 1, TYPE_ITEM = 2;

    boolean isAvailable();

    /** Bit flags for {@link #caps()}. */
    int CAP_ATTACK = 1, CAP_MOVE = 2;

    /**
     * What the native side really implements. {@link #isAvailable()} only says that the game state
     * can be read; attack() / setMotion() may still be stubs, and then modules must use their
     * input fallbacks.
     */
    default int caps() { return 0; }
    default boolean canAttack() { return (caps() & CAP_ATTACK) != 0; }
    default boolean canMove() { return (caps() & CAP_MOVE) != 0; }

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

    // ---- extra calls used by Fly / Aura / Hitbox / Velocity / Surround ------------------------
    // Default to "not supported" so older native builds keep working.

    /** Sets the local player's rotation (degrees, Bedrock convention: yaw 0 = +Z, pitch + = down). */
    default boolean setRotation(float yaw, float pitch) { return false; }
    /** Plays the arm swing animation. */
    default boolean swing() { return false; }
    /** Enlarges the hitbox of other players (and mobs when {@code mobs}); {@code on=false} restores it. */
    default boolean setHitbox(boolean on, float width, float height, boolean mobs) { return false; }
    /** Scales incoming knockback, in percent (100 = vanilla, 0 = none); {@code on=false} restores it. */
    default boolean setKnockback(boolean on, float horizontalPct, float verticalPct) { return false; }
    /** @return true when a block can be placed at that position. */
    default boolean canPlaceBlock(int x, int y, int z) { return false; }
    default boolean placeBlock(int x, int y, int z) { return false; }
    /** @return hotbar slot 0..8 holding the item/block with that id, or -1. */
    default int findHotbarItem(int itemId) { return -1; }
    default int selectedSlot() { return -1; }
}
