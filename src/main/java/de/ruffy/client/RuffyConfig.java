package de.ruffy.client;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Persistent client-only settings for Ruffy Visuals. */
public final class RuffyConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ruffyclient/config");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ruffyclient.properties");
    private static final Properties VALUES = new Properties();
    private static final Map<String, Boolean> DEFAULTS = Map.ofEntries(
            Map.entry("watermark", true),
            Map.entry("fps", true),
            Map.entry("coordinates", false),
            Map.entry("keystrokes", true),
            Map.entry("nebula_particles", true),
            Map.entry("movement_trail", false),
            Map.entry("jump_burst", true),
            Map.entry("swing_animation", true),
            Map.entry("hit_sparks", true),
            Map.entry("critical_sparks", true),
            Map.entry("landing_ring", true),
            Map.entry("sprint_dust", false)
    );

    private RuffyConfig() {}

    public static synchronized void load() {
        VALUES.clear();
        if (Files.isRegularFile(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                VALUES.load(reader);
            } catch (IOException exception) {
                LOGGER.warn("Could not read config {}; using defaults", FILE, exception);
            }
        }
    }

    public static synchronized boolean get(String key) {
        return Boolean.parseBoolean(
                VALUES.getProperty(key, String.valueOf(DEFAULTS.getOrDefault(key, false)))
        );
    }

    public static synchronized void set(String key, boolean enabled) {
        if (!DEFAULTS.containsKey(key)) return;
        VALUES.setProperty(key, Boolean.toString(enabled));
        save();
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                VALUES.store(writer, "Ruffy Visuals client settings");
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save config {}", FILE, exception);
        }
    }
}
