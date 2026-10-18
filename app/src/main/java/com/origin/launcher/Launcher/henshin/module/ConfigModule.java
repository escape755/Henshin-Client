package com.origin.launcher.Launcher.henshin.module;

import android.util.Log;

import com.origin.launcher.Launcher.henshin.core.Category;
import com.origin.launcher.Launcher.henshin.core.Module;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * A "Legit" module: a plain on/off switch stored as one boolean key in
 * /storage/emulated/0/games/henshin_app/henshin_mods/config.json, the file the native side reads.
 * (These used to live on the old Modules page of the launcher.)
 */
public class ConfigModule extends Module {
    private static final String TAG = "ConfigModule";
    private static final File CONFIG =
            new File("/storage/emulated/0/games/henshin_app/henshin_mods/config.json");
    private static final Object LOCK = new Object();

    private final String key;
    private final boolean defaultValue;

    public ConfigModule(String name, String description, String key, boolean defaultValue) {
        super(name, description, Category.LEGIT);
        this.key = key;
        this.defaultValue = defaultValue;
    }

    /** Current value in config.json (or the default when the file / key is missing). */
    public boolean readState() {
        synchronized (LOCK) {
            JSONObject json = readConfig();
            return json.has(key) ? json.optBoolean(key, defaultValue) : defaultValue;
        }
    }

    @Override protected void onEnable() { write(true); }

    @Override protected void onDisable() { write(false); }

    private void write(boolean value) {
        synchronized (LOCK) {
            try {
                JSONObject json = readConfig();
                json.put(key, value);
                File dir = CONFIG.getParentFile();
                if (dir != null && !dir.exists()) dir.mkdirs();
                try (FileWriter w = new FileWriter(CONFIG)) {
                    w.write(json.toString(2));
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not save " + key, e);
            }
        }
    }

    private static JSONObject readConfig() {
        try {
            if (!CONFIG.exists()) return new JSONObject();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new FileReader(CONFIG))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line);
            }
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "Could not read config.json", e);
            return new JSONObject();
        }
    }
}
