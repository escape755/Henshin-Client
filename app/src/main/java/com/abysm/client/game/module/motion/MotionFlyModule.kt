package com.abysm.client.game.module.motion

import android.view.KeyEvent
import com.abysm.client.game.Module
import com.abysm.client.game.ModuleCategory
import com.origin.launcher.Launcher.henshin.bridge.GameBridge
import com.origin.launcher.Launcher.henshin.bridge.InputInjector
import com.origin.launcher.Launcher.henshin.core.ModuleManager

/** Port of AbySm's MotionFlyModule: velocity-based fly (no abilities packet needed). */
class MotionFlyModule : Module("MotionFly", ModuleCategory.Motion) {
    private var hSpeed by floatValue("hSpeed", 0.8f, 0.2f..3.0f, 0.1f)
    private var vSpeed by floatValue("vSpeed", 0.5f, 0.2f..3.0f, 0.1f)

    override fun requiresBridge(): Boolean = true

    override fun onTick(game: GameBridge, input: InputInjector?) {
        if (!game.isAvailable) return
        val st = game.playerState() ?: return
        if (st.size < 5) return
        val mm = ModuleManager.get()
        if (mm.guiOpen) return

        var mx = 0f; var mz = 0f; var my = 0f
        val w = mm.isKeyDown(KeyEvent.KEYCODE_W); val s = mm.isKeyDown(KeyEvent.KEYCODE_S)
        val a = mm.isKeyDown(KeyEvent.KEYCODE_A); val d = mm.isKeyDown(KeyEvent.KEYCODE_D)
        val up = mm.isKeyDown(KeyEvent.KEYCODE_SPACE)
        val down = mm.isKeyDown(KeyEvent.KEYCODE_SHIFT_LEFT) || mm.isKeyDown(KeyEvent.KEYCODE_SHIFT_RIGHT)

        val fwd = (if (w) 1 else 0) - (if (s) 1 else 0)
        val str = (if (d) 1 else 0) - (if (a) 1 else 0)
        val rad = Math.toRadians(st[3].toDouble())
        val sin = Math.sin(rad).toFloat(); val cos = Math.cos(rad).toFloat()
        mx += (-sin * fwd + cos * str) * hSpeed
        mz += (cos * fwd + sin * str) * hSpeed
        if (up) my += vSpeed
        if (down) my -= vSpeed

        if (mx != 0f || mz != 0f || my != 0f) game.setMotion(mx, my, mz)
    }
}
