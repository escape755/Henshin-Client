package com.abysm.client.game

import com.origin.launcher.Launcher.henshin.bridge.GameBridge
import com.origin.launcher.Launcher.henshin.bridge.InputInjector
import com.origin.launcher.Launcher.henshin.core.Category as HCategory
import com.origin.launcher.Launcher.henshin.core.Module as HModule
import com.origin.launcher.Launcher.henshin.core.ModuleManager
import com.origin.launcher.Launcher.henshin.value.BoolValue
import com.origin.launcher.Launcher.henshin.value.FloatValue
import com.origin.launcher.Launcher.henshin.value.IntValue
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * AbySm-compatible module base, backed by Henshin's native bridge.
 * Keeps AbySm's value-delegate API (`private var v by floatValue(...)`),
 * so ported modules read almost exactly like the originals.
 */
open class Module(name: String, category: ModuleCategory) : HModule(name, "AbySm port", category.henshin) {

    /** Live game access (native bridge). */
    protected val game: GameBridge get() = ModuleManager.get().bridge

    /** Synthetic input, null when the game activity is not attached. */
    protected val input: InputInjector? get() = ModuleManager.get().input

    protected fun floatValue(name: String, def: Float, range: ClosedFloatingPointRange<Float>, step: Float = 0.05f) =
        Prop(add(FloatValue(name, def, range.start, range.endInclusive, step)))

    protected fun intValue(name: String, def: Int, range: IntRange, step: Int = 1) =
        Prop(add(IntValue(name, def, range.first, range.last, step)))

    protected fun boolValue(name: String, def: Boolean) =
        Prop(add(BoolValue(name, def)))

    protected class Prop<V>(private val v: com.origin.launcher.Launcher.henshin.value.Value<V>) : ReadWriteProperty<Any?, V> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): V = v.get()
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) { v.set(value) }
    }
}
