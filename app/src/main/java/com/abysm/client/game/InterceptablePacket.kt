package com.abysm.client.game

/**
 * Compatibility version of AbySm's InterceptablePacket.
 *
 * AbySm runs a Bedrock packet relay, so its modules intercept real protocol packets.
 * Henshin talks to the game through a native memory bridge instead of a relay, so nothing
 * feeds this class; it exists so AbySm-style module code compiles and packet-only hooks
 * degrade gracefully (they simply never fire). Do not expect packet-based features to work.
 */
class InterceptablePacket(var packet: Any? = null) {
    var cancelled: Boolean = false
        private set

    /** Marks the packet as consumed (AbySm semantics). */
    fun cancel() { cancelled = true }

    /** AbySm alias for cancel(). */
    fun intercept() { cancelled = true }
}
