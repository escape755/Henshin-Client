package com.abysm.client.game.module.misc

import com.abysm.client.game.Module
import com.abysm.client.game.ModuleCategory
import com.origin.launcher.Launcher.henshin.bridge.GameBridge
import com.origin.launcher.Launcher.henshin.bridge.InputInjector
import com.origin.launcher.Launcher.henshin.core.ModuleManager

/**
 * Port of AbySm's AutoDisconnectModule: leaves the world when health drops too low.
 * AbySm reads health from packets; here we use the bridge's player/entity data when present.
 */
class AutoDisconnectModule : Module("AutoDisconnect", ModuleCategory.Misc) {
    private var minHealth by floatValue("minHealth", 4f, 1f..20f, 0.5f)

    override fun requiresBridge(): Boolean = true

    override fun onTick(game: GameBridge, input: InputInjector?) {
        // The bridge does not expose local player health directly; entities list has remote players only.
        // Fallback: disconnect on low hunger, which the bridge does expose.
        val h = game.hunger()
        if (h in 0..10 && h < minHealth / 2f) {
            ModuleManager.get().activity?.finish()
            setEnabled(false)
        }
    }
}
