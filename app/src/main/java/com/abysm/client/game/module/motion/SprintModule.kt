package com.abysm.client.game.module.motion

import android.view.KeyEvent
import com.abysm.client.game.Module
import com.abysm.client.game.ModuleCategory
import com.origin.launcher.Launcher.henshin.bridge.GameBridge
import com.origin.launcher.Launcher.henshin.bridge.InputInjector
import com.origin.launcher.Launcher.henshin.core.ModuleManager

/** Port of AbySm's SprintModule: sprint automatically while walking forward. */
class SprintModule : Module("Sprint", ModuleCategory.Motion) {
    private var lastSprint = 0L

    override fun onTick(game: GameBridge, input: InputInjector?) {
        val mm = ModuleManager.get()
        if (mm.guiOpen || input == null) return
        val now = System.currentTimeMillis()
        if (mm.isKeyDown(KeyEvent.KEYCODE_W) && now - lastSprint > 900) {
            input.doubleKey(KeyEvent.KEYCODE_W) // double-tap forward = sprint
            lastSprint = now
        }
    }
}
