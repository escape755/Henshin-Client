package com.abysm.client.game

import com.origin.launcher.Launcher.henshin.core.ModuleManager

/** A nearby entity/player as exposed by the native bridge (8 floats per entity). */
class Player(
    val id: Long,
    val type: Int,          // 0 player, 1 mob, 2 item
    val x: Float,
    val y: Float,
    val z: Float,
    val distance: Float,
    val health: Float,
    val yawTo: Float
) {
    val isPlayer: Boolean get() = type == 0
    val isMob: Boolean get() = type == 1
    val isItem: Boolean get() = type == 2

    companion object {
        /** Entities within range, parsed from the bridge's flat float array. */
        fun nearby(range: Float): List<Player> {
            val raw = ModuleManager.get().bridge.nearbyEntities(range) ?: return emptyList()
            val out = ArrayList<Player>(raw.size / 8)
            var i = 0
            while (i + 7 < raw.size) {
                out.add(Player(
                    raw[i].toLong(), raw[i + 1].toInt(),
                    raw[i + 2], raw[i + 3], raw[i + 4],
                    raw[i + 5], raw[i + 6], raw[i + 7]
                ))
                i += 8
            }
            return out
        }
    }
}
