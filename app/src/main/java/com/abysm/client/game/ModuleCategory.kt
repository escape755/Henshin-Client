package com.abysm.client.game

import com.origin.launcher.Launcher.henshin.core.Category as HCategory

/** AbySm module categories mapped onto Henshin's GUI categories. */
enum class ModuleCategory(val henshin: HCategory) {
    Combat(HCategory.COMBAT),
    Motion(HCategory.MOVEMENT),
    Visual(HCategory.RENDER),
    Misc(HCategory.MISC),
    World(HCategory.MISC)
}
