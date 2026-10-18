package com.origin.launcher.discord;

import android.app.Activity;

import com.origin.launcher.discord.DiscordManager;

/** One shared DiscordManager for the Account and Settings pages (avoids two RPC connections). */
public final class DiscordHolder {
    private static DiscordManager manager;
    private static boolean rpcInitialised;

    private DiscordHolder() {}

    public static synchronized DiscordManager get(Activity activity) {
        if (manager == null) {
            manager = new DiscordManager(activity);
        }
        if (!rpcInitialised) {
            DiscordRPCHelper.getInstance().initialize(manager);
            DiscordRPCHelper.getInstance().initializeRPC(manager.getDiscordRPC());
            rpcInitialised = true;
        }
        return manager;
    }

    public static synchronized void release() {
        manager = null;
        rpcInitialised = false;
    }
}
