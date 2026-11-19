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

/** Persistent, client-only options for the Ruffy Visuals control center. */
public final class RuffyConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ruffyclient/config");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ruffyclient.properties");
    private static final Properties VALUES = new Properties();

    private static final Map<String, Boolean> DEFAULTS = Map.ofEntries(
            Map.entry("watermark", true),
            Map.entry("fps", true),
            Map.entry("coordinates", false),
            Map.entry("keystrokes", true),
            Map.entry("direction", true),
            Map.entry("speed", false),
            Map.entry("world_clock", false),
            Map.entry("nebula_particles", true),
            Map.entry("orbit_particles", false),
            Map.entry("movement_trail", false),
            Map.entry("jump_burst", true),
            Map.entry("sprint_dust", false),
            Map.entry("landing_ring", true),
            Map.entry("step_sparks", false),
            Map.entry("hit_sparks", true),
            Map.entry("swing_animation", true),
            Map.entry("critical_sparks", true),
            Map.entry("low_health_aura", false),
            Map.entry("potion_glow", false),
            Map.entry("damage_pulse", true),
            Map.entry("underwater_bubbles", true),
            Map.entry("weather_fx", false),
            Map.entry("cave_dust", false),
            Map.entry("starfall", false),
            Map.entry("palette_sparks", true),
            Map.entry("health_hud", true),
            Map.entry("armor_hud", false),
            Map.entry("smooth_animations", true),
            Map.entry("prism_core", true),
            Map.entry("orbit_drones", false),
            Map.entry("three_d_slash", true)
    );

    private static final Map<String, Integer> INTEGER_DEFAULTS = Map.of(
            "palette", 0,
            "hud_opacity", 44,
            "menu_opacity", 94,
            "effect_quality", 1,
            "animation_smoothness", 78,
            "hud_scale", 96
    );

    private static final int[] PRIMARY = {
            0xFFB45CFF, // Nebula violet
            0xFFFF4D8D, // Rose
            0xFFEC4CFF, // Magenta
            0xFF995CFF, // Violet
            0xFF38BDF8, // Azure
            0xFF7DD3FC, // Ice
            0xFF41D9B0, // Mint
            0xFF45C779, // Emerald
            0xFFFFB547, // Amber
            0xFFFFE16A  // Solar
    };
    private static final int[] SECONDARY = {
            0xFF6034AE, 0xFFA92254, 0xFF7A2FA3, 0xFF4E2A9C,
            0xFF17678C, 0xFF326A90, 0xFF1D826A, 0xFF21764B,
            0xFFA7651E, 0xFFA98A2E
    };

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
        if (!DEFAULTS.containsKey(key) && !key.startsWith("favorite_")) return;
        VALUES.setProperty(key, Boolean.toString(enabled));
        save();
    }

    public static synchronized int getInt(String key) {
        int fallback = INTEGER_DEFAULTS.getOrDefault(key, 0);
        try {
            return Integer.parseInt(VALUES.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static synchronized void setInt(String key, int value) {
        if (!INTEGER_DEFAULTS.containsKey(key)) return;
        int clamped = switch (key) {
            case "palette" -> Math.max(0, Math.min(PRIMARY.length - 1, value));
            case "hud_opacity" -> Math.max(12, Math.min(90, value));
            case "menu_opacity" -> Math.max(60, Math.min(100, value));
            case "effect_quality" -> Math.max(0, Math.min(2, value));
            case "animation_smoothness" -> Math.max(0, Math.min(100, value));
            case "hud_scale" -> Math.max(75, Math.min(120, value));
            default -> value;
        };
        VALUES.setProperty(key, Integer.toString(clamped));
        save();
    }

    public static int primaryColor() {
        return PRIMARY[Math.max(0, Math.min(PRIMARY.length - 1, getInt("palette")))];
    }

    public static int secondaryColor() {
        return SECONDARY[Math.max(0, Math.min(SECONDARY.length - 1, getInt("palette")))];
    }

    public static int hudPanelColor() {
        int alpha = 12 + (getInt("hud_opacity") * 150 / 100);
        return (alpha << 24) | 0x000B0A19;
    }

    public static int hudBorderColor() {
        int alpha = 26 + (getInt("hud_opacity") * 82 / 100);
        return (alpha << 24) | (primaryColor() & 0x00FFFFFF);
    }

    public static synchronized void reset() {
        VALUES.clear();
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
