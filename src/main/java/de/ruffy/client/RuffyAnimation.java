package de.ruffy.client;

import java.util.HashMap;
import java.util.Map;

/**
 * Frame-rate independent animation helper shared by the ClickGUI, HUD and
 * first-person rendering. The single user setting controls all transitions.
 */
public final class RuffyAnimation {
    private static final Map<String, Track> TRACKS = new HashMap<>();

    private RuffyAnimation() {}

    /**
     * Smoothly approaches a target using an exponential time-based curve.
     * Smoothness 0 snaps immediately; 100 uses the longest, softest transition.
     */
    public static synchronized float animate(String key, float target) {
        long now = System.nanoTime();
        Track track = TRACKS.get(key);

        if (track == null) {
            track = new Track(0.0F, now);
            TRACKS.put(key, track);
        }

        if (!RuffyConfig.get("smooth_animations")) {
            track.value = target;
            track.lastNanos = now;
            return target;
        }

        int smoothness = RuffyConfig.getInt("animation_smoothness");
        if (smoothness <= 0) {
            track.value = target;
            track.lastNanos = now;
            return target;
        }

        double elapsed = Math.max(0.0,
                Math.min(0.1, (now - track.lastNanos) / 1_000_000_000.0));
        track.lastNanos = now;

        // 45 ms at low smoothness, up to 245 ms at maximum smoothness.
        double duration = 0.045 + (smoothness / 100.0) * 0.20;
        float blend = (float) (1.0 - Math.exp(-elapsed / duration));
        track.value += (target - track.value) * blend;

        if (Math.abs(target - track.value) < 0.0015F) {
            track.value = target;
        }
        return track.value;
    }

    /** Set a starting value for an animation, for example when opening a screen. */
    public static synchronized void set(String key, float value) {
        Track track = TRACKS.computeIfAbsent(key, ignored -> new Track(value, System.nanoTime()));
        track.value = value;
        track.lastNanos = System.nanoTime();
    }

    /** Blend all ARGB channels between two colors. */
    public static int blendColor(int from, int to, float amount) {
        float t = Math.max(0.0F, Math.min(1.0F, amount));
        int a = blendChannel((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, t);
        int r = blendChannel((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, t);
        int g = blendChannel((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, t);
        int b = blendChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int multiplyAlpha(int color, float opacity) {
        int alpha = (color >>> 24) & 0xFF;
        int result = Math.max(0, Math.min(255, Math.round(alpha * opacity)));
        return (color & 0x00FFFFFF) | (result << 24);
    }

    private static int blendChannel(int from, int to, float amount) {
        return Math.max(0, Math.min(255, Math.round(from + (to - from) * amount)));
    }

    private static final class Track {
        private float value;
        private long lastNanos;

        private Track(float value, long lastNanos) {
            this.value = value;
            this.lastNanos = lastNanos;
        }
    }
}
