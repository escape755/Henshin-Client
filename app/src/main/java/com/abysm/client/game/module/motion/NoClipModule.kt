package com.abysm.client.game.module.motion

import com.abysm.client.game.Module
import com.abysm.client.game.ModuleCategory
import com.origin.launcher.Launcher.henshin.bridge.GameBridge
import com.origin.launcher.Launcher.henshin.bridge.InputInjector

/**
 * Port of AbySm's NoClipModule.
 * AbySm does this with an abilities packet; Henshin's bridge cannot toggle noclip server-side,
 * so this is a best-effort "phase push": constant forward pressure with a small vertical jitter
 * to squeeze through thin walls. Works on some blocks, not a true noclip.
 */
class NoClipModule : Module("NoClip", ModuleCategory.Motion) {
    private var push by floatValue("push", 0.3f, 0.1f..1.0f, 0.05f)
    private var tick = 0

    override fun requiresBridge(): Boolean = true

    override fun onTick(game: GameBridge, input: InputInjector?) {
        if (!game.isAvailable) return
        val st = game.playerState() ?: return
        if (st.size < 5) return
        if (!game.collidingHorizontally()) return

        val rad = Math.toRadians(st[3].toDouble())
        val sin = Math.sin(rad).toFloat(); val cos = Math.cos(rad).toFloat()
        tick++
        val jitter = if (tick % 2 == 0) 0.08f else -0.02f
        game.setMotion(-sin * push, jitter, cos * push)
    }
}
