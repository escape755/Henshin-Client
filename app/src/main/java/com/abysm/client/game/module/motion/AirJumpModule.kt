package com.abysm.client.game.module.motion

import android.view.KeyEvent
import com.abysm.client.game.Module
import com.abysm.client.game.ModuleCategory
import com.origin.launcher.Launcher.henshin.bridge.GameBridge
import com.origin.launcher.Launcher.henshin.bridge.InputInjector
import com.origin.launcher.Launcher.henshin.core.ModuleManager

/** Port of AbySm's AirJumpModule: double-jump in mid air. */
class AirJumpModule : Module("AirJump", ModuleCategory.Motion) {
    private var wasDown = false
    private var lastJump = 0L

    override fun onTick(game: GameBridge, input: InputInjector?) {
        if (input == null) return
        val mm = ModuleManager.get()
        val down = mm.isKeyDown(KeyEvent.KEYCODE_SPACE)
        val now = System.currentTimeMillis()
        if (down && !wasDown && !mm.guiOpen && now - lastJump > 250) {
            input.doubleKey(KeyEvent.KEYCODE_SPACE)
            lastJump = now
        }
        wasDown = down
    }
}
