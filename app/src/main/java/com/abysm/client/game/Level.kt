package com.abysm.client.game

/** AbySm-style world access. The bridge only exposes nearby entities, so that's all we get. */
object Level {
    fun players(range: Float = 64f): List<Player> = Player.nearby(range).filter { it.isPlayer }
    fun mobs(range: Float = 64f): List<Player> = Player.nearby(range).filter { it.isMob }
    fun items(range: Float = 32f): List<Player> = Player.nearby(range).filter { it.isItem }
}
