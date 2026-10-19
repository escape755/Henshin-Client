package com.abysm.client.game.module.motion

import android.view.KeyEvent
import com.abysm.client.game.Module
import com.abysm.client.game.ModuleCategory
import com.origin.launcher.Launcher.henshin.bridge.GameBridge
import com.origin.launcher.Launcher.henshin.bridge.InputInjector
import com.origin.launcher.Launcher.henshin.core.ModuleManager

/** Port of AbySm's SpeedModule: horizontal speed boost through velocity. */
class SpeedModule : Module("Speed", ModuleCategory.Motion) {
    private var speed by floatValue("speed", 0.6f, 0.2f..2.5f)
    private var onlyGround by boolValue("onlyForward", true)

    override fun requiresBridge(): Boolean = true

    override fun onTick(game: GameBridge, input: InputInjector?) {
        if (!game.isAvailable) return
        val st = game.playerState() ?: return
        if (st.size < 5) return
        val mm = ModuleManager.get()
        if (mm.guiOpen) return

        val w = mm.isKeyDown(KeyEvent.KEYCODE_W); val s = mm.isKeyDown(KeyEvent.KEYCODE_S)
        val a = mm.isKeyDown(KeyEvent.KEYCODE_A); val d = mm.isKeyDown(KeyEvent.KEYCODE_D)
        if (onlyGround && !w) return
        val fwd = (if (w) 1 else 0) - (if (s) 1 else 0)
        val str = (if (d) 1 else 0) - (if (a) 1 else 0)
        if (fwd == 0 && str == 0) return

        val rad = Math.toRadians(st[3].toDouble())
        val sin = Math.sin(rad).toFloat(); val cos = Math.cos(rad).toFloat()
        // Bedrock convention: yaw 0 faces +Z.
        val vx = (-sin * fwd + cos * str) * speed
        val vz = (cos * fwd + sin * str) * speed
        game.setMotion(vx, 0f, vz) // Y is not exposed by the bridge; horizontal only
    }
}
