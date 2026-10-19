package com.abysm.client.game.module.motion

import android.view.KeyEvent
import com.abysm.client.game.Module
import com.abysm.client.game.ModuleCategory
import com.origin.launcher.Launcher.henshin.bridge.GameBridge
import com.origin.launcher.Launcher.henshin.bridge.InputInjector
import com.origin.launcher.Launcher.henshin.core.ModuleManager

/** Port of AbySm's HighJumpModule: bigger jumps on space press. */
class HighJumpModule : Module("HighJump", ModuleCategory.Motion) {
    private var height by floatValue("height", 0.8f, 0.4f..2.5f, 0.05f)
    private var wasDown = false

    override fun requiresBridge(): Boolean = true

    override fun onTick(game: GameBridge, input: InputInjector?) {
        if (!game.isAvailable) return
        val mm = ModuleManager.get()
        val down = mm.isKeyDown(KeyEvent.KEYCODE_SPACE)
        if (down && !wasDown && !mm.guiOpen) game.setMotion(0f, height, 0f)
        wasDown = down
    }
}
