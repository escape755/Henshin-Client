package com.origin.launcher.Launcher.henshin.module;

import com.origin.launcher.Launcher.henshin.core.Module;

import java.util.ArrayList;
import java.util.List;

/** The client tweaks that used to be on the Modules page; now the "Legit" ClickGUI category. */
public final class LegitModules {
    private LegitModules() {}

    public static List<Module> create() {
        List<Module> l = new ArrayList<>();
        l.add(new ConfigModule("No hurt cam", "Turns off the in-game hurt camera shake", "Nohurtcam", false));
        l.add(new ConfigModule("No Fog", "Turns off the in-game fog (doesn't work with fullbright)", "Nofog", false));
        l.add(new ConfigModule("Better Brightness", "Lets you see in the dark", "better_brightness", false));
        l.add(new ConfigModule("Particles Disabler", "Turns off the in-game particles", "particles_disabler", false));
        l.add(new ConfigModule("Java Fancy Clouds", "Changes the clouds to Java fancy clouds", "java_clouds", false));
        l.add(new ConfigModule("Java Cubemap", "Lowers the in-game cubemap a bit, like Java", "java_cubemap", false));
        l.add(new ConfigModule("Classic Vanilla skins", "Disables the newer skins added by Mojang", "classic_skins", false));
        l.add(new ConfigModule("No flipbook animation", "More FPS by disabling block animations", "no_flipbook_animations", false));
        l.add(new ConfigModule("No Shadows", "More FPS by disabling shadows", "no_shadows", false));
        l.add(new ConfigModule("Henshin Title", "Changes the start screen title image", "henshin_title", true));
        l.add(new ConfigModule("2x tpp view", "Doubles the third person view radius", "double_tppview", false));
        l.add(new ConfigModule("White Block Outline", "Makes the block selection outline white", "white_block_outline", false));
        l.add(new ConfigModule("No pumpkin overlay", "Removes the dark overlay when wearing a pumpkin", "no_pumpkin_overlay", false));
        l.add(new ConfigModule("No spyglass overlay", "Removes the overlay when using a spyglass", "no_spyglass_overlay", false));
        l.add(new ConfigModule("No Eating animation", "Disables the eating animation of food items", "no_eating_animation", false));
        l.add(new ConfigModule("No Bow animation", "Disables the bow animation", "no_bow_animation", false));
        l.add(new ConfigModule("Portal optimizer", "Optimizes the portal effect", "portal_optimizer", false));
        l.add(new ConfigModule("Particle single mapping", "Optimizes the particles material (culling on, particle fog off)", "psm", false));
        l.add(new ConfigModule("Custom CrossHair", "Lets you use your own crosshair", "custom_cross_hair", false));
        return l;
    }
}
