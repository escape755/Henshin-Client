package com.origin.launcher.Launcher.henshin.core;

public enum Category {
    COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), PLAYER("Player"), MISC("Misc");

    public final String title;
    Category(String title) { this.title = title; }
}
