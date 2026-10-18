package com.mojang.minecraftpe;

/**
 * Henshin: bridge that lets the launcher app intercept chat lines that start with "."
 * (e.g. ".bind Killaura Z") before they are sent to the server.
 * The :minecraft module cannot depend on :app, so the app registers its handler here.
 */
public abstract class ChatCommandHook {
    /** Set by the app. Null when no client is active. */
    public static volatile ChatCommandHook handler;

    /** @return true when the line was a Henshin command and must NOT be sent as chat. */
    public abstract boolean onChatCommand(String line);
}
