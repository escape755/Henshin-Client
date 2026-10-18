package com.origin.launcher.Launcher.henshin.command;

import android.app.Activity;

import com.mojang.minecraftpe.ChatCommandHook;
import com.origin.launcher.Launcher.henshin.core.HenshinSettings;
import com.origin.launcher.Launcher.henshin.core.Module;
import com.origin.launcher.Launcher.henshin.core.ModuleManager;
import com.origin.launcher.Launcher.henshin.gui.GuiLauncher;
import com.origin.launcher.Launcher.henshin.shortcut.ShortcutManager;
import com.origin.launcher.Launcher.henshin.value.Value;

import java.util.Arrays;

/**
 * In-game chat commands (type them in the Minecraft chat, they are never sent to the server):
 *   .bind <module> <key>     .unbind <module>     .binds
 *   .toggle <module>         .set <module> <value> <new value>
 *   .shortcut add|remove <module> | .shortcut edit | .shortcut clear
 *   .gui                      .modules             .help
 */
public final class CommandManager extends ChatCommandHook {
    private final Activity activity;

    public CommandManager(Activity a) { this.activity = a; }

    private void say(String s) { ModuleManager.get().toast(s); }

    @Override
    public boolean onChatCommand(String line) {
        String[] t = line.substring(1).trim().split("\\s+");
        if (t.length == 0 || t[0].isEmpty()) return false;
        String cmd = t[0].toLowerCase();
        ModuleManager mm = ModuleManager.get();
        try {
            switch (cmd) {
                case "bind": {
                    if (t.length < 3) { say("Usage: .bind <module> <key>  (e.g. .bind Killaura Z)"); return true; }
                    Module m = findModule(t, 1, t.length - 1);
                    if (m == null) { say("Unknown module: " + join(t, 1, t.length - 1)); return true; }
                    String key = t[t.length - 1];
                    if (key.equalsIgnoreCase("none")) { mm.setBind(m, 0); say(m.name + " unbound"); return true; }
                    int code = ModuleManager.parseKey(key);
                    if (code == 0) { say("Unknown key: " + key); return true; }
                    mm.setBind(m, code);
                    say(m.name + " bound to " + ModuleManager.keyName(code));
                    return true;
                }
                case "unbind": {
                    Module m = findModule(t, 1, t.length);
                    if (m == null) { say("Usage: .unbind <module>"); return true; }
                    mm.setBind(m, 0);
                    say(m.name + " unbound");
                    return true;
                }
                case "binds": {
                    StringBuilder sb = new StringBuilder();
                    for (Module m : mm.getModules())
                        if (m.getKeyCode() != 0) sb.append(m.name).append(": ").append(ModuleManager.keyName(m.getKeyCode())).append("  ");
                    say(sb.length() == 0 ? "No binds set" : sb.toString().trim());
                    return true;
                }
                case "t": case "toggle": {
                    Module m = findModule(t, 1, t.length);
                    if (m == null) { say("Usage: .toggle <module>"); return true; }
                    m.toggle();
                    say(m.name + (m.isEnabled() ? " ON" : " OFF"));
                    return true;
                }
                case "set": {
                    if (t.length < 4) { say("Usage: .set <module> <value> <new value>"); return true; }
                    Module m = mm.find(t[1]);
                    if (m == null) { say("Unknown module: " + t[1]); return true; }
                    Value<?> v = m.findValue(t[2]);
                    if (v == null) { say("Unknown value: " + t[2]); return true; }
                    String val = join(t, 3, t.length);
                    say(v.deserialize(val) ? m.name + "." + v.name + " = " + v.serialize() : "Invalid value: " + val);
                    return true;
                }
                case "shortcut": case "sc": {
                    ShortcutManager sm = ShortcutManager.get();
                    String sub = t.length > 1 ? t[1].toLowerCase() : "";
                    if (sub.equals("clear")) {
                        sm.clear();
                        say("All shortcuts removed");
                    } else if ((sub.equals("add") || sub.equals("remove")) && t.length > 2) {
                        Module m = findModule(t, 2, t.length);
                        if (m == null) { say("Unknown module"); return true; }
                        if (sub.equals("add")) { sm.add(m); say("Shortcut added: " + m.name); }
                        else { sm.remove(m); say("Shortcut removed: " + m.name); }
                    } else {
                        say("Usage: .shortcut add|remove <module> | clear");
                    }
                    return true;
                }
                case "gui": {
                    GuiLauncher.toggle(activity);
                    return true;
                }
                case "modules": {
                    StringBuilder sb = new StringBuilder();
                    for (Module m : mm.getModules()) sb.append(m.name).append(m.isEnabled() ? "*" : "").append("  ");
                    say(sb.toString().trim());
                    return true;
                }
                case "help": {
                    say(".bind <mod> <key> | .unbind | .binds | .toggle | .set <mod> <value> <x> | .shortcut add|remove|clear | .gui | .modules");
                    return true;
                }
                default:
                    return false; // not ours: let it go to the server as a normal message
            }
        } catch (Throwable e) {
            say("Command error: " + e.getMessage());
            return true;
        }
    }

    /** Module names may contain spaces in the future, so try to join the words first. */
    private Module findModule(String[] t, int from, int to) {
        if (to <= from) return null;
        return ModuleManager.get().find(join(t, from, to));
    }

    private static String join(String[] t, int from, int to) {
        return String.join(" ", Arrays.copyOfRange(t, from, to));
    }
}
