package com.abysm.client.game

import com.origin.launcher.Launcher.henshin.core.ModuleManager

/** The local player, mirroring AbySm's LocalPlayer over Henshin's native bridge. */
class LocalPlayer private constructor() {

    companion object { val instance = LocalPlayer() }

    private val game get() = ModuleManager.get().bridge

    val isAvailable: Boolean get() = game.isAvailable

    private fun st(): FloatArray? = game.playerState()?.takeIf { it.size >= 5 }

    val x: Float get() = st()?.get(0) ?: 0f
    val y: Float get() = st()?.get(1) ?: 0f
    val z: Float get() = st()?.get(2) ?: 0f
    val yaw: Float get() = st()?.get(3) ?: 0f
    val pitch: Float get() = st()?.get(4) ?: 0f

    fun setMotion(x: Float, y: Float, z: Float) = game.setMotion(x, y, z)
    fun setRotation(yaw: Float, pitch: Float) = game.setRotation(yaw, pitch)
    fun swing() = game.swing()
    fun selectHotbar(slot0to8: Int) = game.selectHotbar(slot0to8)
    fun useItem(holding: Boolean) = game.useItem(holding)
    fun collidingHorizontally(): Boolean = game.collidingHorizontally()
    fun hunger(): Int = game.hunger()
}

/** Convenience accessor matching AbySm's `LocalPlayer` usage style. */
val localPlayer: LocalPlayer get() = LocalPlayer.instance
