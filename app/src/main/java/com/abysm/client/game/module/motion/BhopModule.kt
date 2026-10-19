package com.abysm.client.game.module.motion

import android.view.KeyEvent
import com.abysm.client.game.Module
import com.abysm.client.game.ModuleCategory
import com.origin.launcher.Launcher.henshin.bridge.GameBridge
import com.origin.launcher.Launcher.henshin.bridge.InputInjector
import com.origin.launcher.Launcher.henshin.core.ModuleManager

/** Port of AbySm's BhopModule: bunny-hop with horizontal boost. */
class BhopModule : Module("Bhop", ModuleCategory.Motion) {
    private var speed by floatValue("speed", 0.55f, 0.2f..2.0f)
    private var hop by floatValue("hop", 0.42f, 0.1f..1.0f, 0.01f)

    override fun requiresBridge(): Boolean = true

    override fun onTick(game: GameBridge, input: InputInjector?) {
        if (!game.isAvailable) return
        val st = game.playerState() ?: return
        if (st.size < 5) return
        val mm = ModuleManager.get()
        if (mm.guiOpen || !mm.isKeyDown(KeyEvent.KEYCODE_W)) return

        val rad = Math.toRadians(st[3].toDouble())
        val sin = Math.sin(rad).toFloat(); val cos = Math.cos(rad).toFloat()
        game.setMotion(-sin * speed, hop, cos * speed)
    }
}
