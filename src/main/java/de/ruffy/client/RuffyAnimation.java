package de.ruffy.client;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Ruffy's native animation math.
 *
 * Uses a frame-rate-independent critically damped spring for UI state changes,
 * a cubic Hermite easing curve for timeline animations, shortest-path angular
 * interpolation and reusable 3D vector rotation helpers for custom meshes.
 */
public final class RuffyAnimation {
    private static final Map<String, SpringTrack> SPRINGS = new HashMap<>();
    private static final Map<String, Long> LAST_ACCESS = new HashMap<>();

    private RuffyAnimation() {}

    /**
     * Critical spring interpolation. Unlike a fixed lerp it preserves velocity,
     * remains stable at variable frame rates and settles without overshoot.
     */
    public static synchronized float animate(String key, float target) {
        long now = System.nanoTime();
        SpringTrack track = SPRINGS.get(key);

        if (track == null) {
            track = new SpringTrack(0.0F, 0.0F, now);
            SPRINGS.put(key, track);
        }

        LAST_ACCESS.put(key, now);
        if (!RuffyConfig.get("smooth_animations")) {
            track.value = target;
            track.velocity = 0.0F;
            track.lastNanos = now;
            return target;
        }

        int strength = RuffyConfig.getInt("animation_smoothness");
        if (strength <= 0) {
            track.value = target;
            track.velocity = 0.0F;
            track.lastNanos = now;
            return target;
        }

        double dt = Math.max(0.0, Math.min(0.05,
                (now - track.lastNanos) / 1_000_000_000.0));
        track.lastNanos = now;
        if (dt <= 0.0) return track.value;

        // 70–330 ms characteristic settle time, controlled by one global setting.
        double duration = 0.070 + (strength / 100.0) * 0.260;
        double omega = 4.6 / duration;
        double displacement = track.value - target;
        double acceleration = -(omega * omega) * displacement
                - (2.0 * omega) * track.velocity;

        track.velocity += acceleration * dt;
        float previous = track.value;
        track.value += (float) (track.velocity * dt);

        // Snap cleanly at the target once the spring crosses it or settles.
        if ((previous - target) * (track.value - target) <= 0.0F
                || Math.abs(track.value - target) < 0.0012F
                && Math.abs(track.velocity) < 0.008F) {
            track.value = target;
            track.velocity = 0.0F;
        }

        cleanup(now);
        return track.value;
    }

    /** Starts/restarts a key from an exact value, useful for screen transitions. */
    public static synchronized void set(String key, float value) {
        long now = System.nanoTime();
        SpringTrack track = SPRINGS.computeIfAbsent(key,
                ignored -> new SpringTrack(value, 0.0F, now));
        track.value = value;
        track.velocity = 0.0F;
        track.lastNanos = now;
        LAST_ACCESS.put(key, now);
    }

    /** A timeline easing curve with zero velocity at both ends. */
    public static float easeInOut(float progress) {
        float t = clamp01(progress);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    /** Fast start, slow finish; useful for expanding rings and pop-in motion. */
    public static float easeOut(float progress) {
        float t = clamp01(progress);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
    }

    /** Smooth damped oscillation for breathing/pulsing 3D accents. */
    public static float breathe(double timeSeconds, double frequency, float phase) {
        double wave = Math.sin(timeSeconds * frequency * Math.PI * 2.0 + phase);
        return (float) (0.5 + 0.5 * wave);
    }

    /** Interpolates angles along the shortest circular route. */
    public static float angleLerp(float from, float to, float amount) {
        float delta = (to - from) % 360.0F;
        if (delta > 180.0F) delta -= 360.0F;
        if (delta < -180.0F) delta += 360.0F;
        return from + delta * clamp01(amount);
    }

    /** Rotates a point around yaw (Y), pitch (X), then roll (Z), in radians. */
    public static Vec3 rotate3D(Vec3 point, double yaw, double pitch, double roll) {
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double cr = Math.cos(roll), sr = Math.sin(roll);

        double x1 = point.x * cy - point.z * sy;
        double z1 = point.x * sy + point.z * cy;
        double y1 = point.y;

        double y2 = y1 * cp - z1 * sp;
        double z2 = y1 * sp + z1 * cp;
        double x2 = x1;

        double x3 = x2 * cr - y2 * sr;
        double y3 = x2 * sr + y2 * cr;
        return new Vec3(x3, y3, z2);
    }

    /** Blend ARGB channels without depending on a shader or external animation library. */
    public static int blendColor(int from, int to, float amount) {
        float t = clamp01(amount);
        int a = blendChannel((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, t);
        int r = blendChannel((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, t);
        int g = blendChannel((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, t);
        int b = blendChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int multiplyAlpha(int color, float opacity) {
        int alpha = (color >>> 24) & 0xFF;
        int result = Math.max(0, Math.min(255, Math.round(alpha * clamp01(opacity))));
        return (color & 0x00FFFFFF) | (result << 24);
    }

    public static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static int blendChannel(int from, int to, float amount) {
        return Math.max(0, Math.min(255, Math.round(from + (to - from) * amount)));
    }

    /** Prevents old one-shot interaction tracks from growing forever. */
    private static void cleanup(long now) {
        if (SPRINGS.size() < 256) return;
        long cutoff = now - 30_000_000_000L;
        Iterator<Map.Entry<String, Long>> iterator = LAST_ACCESS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < cutoff) {
                SPRINGS.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    private static final class SpringTrack {
        private float value;
        private float velocity;
        private long lastNanos;

        private SpringTrack(float value, float velocity, long lastNanos) {
            this.value = value;
            this.velocity = velocity;
            this.lastNanos = lastNanos;
        }
    }
}
