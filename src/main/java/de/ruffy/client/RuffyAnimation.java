package de.ruffy.client;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Ruffy's hand-written animation engine.
 *
 * All UI springs, fades, pulses, timelines and 3D transforms are calculated here.
 * It deliberately has no third-party animation dependency. Tracks use a
 * frame-rate-independent critically damped spring and are discarded after
 * they have not been used for a while.
 */
public final class RuffyAnimation {
    private static final Map<String, SpringTrack> SPRINGS = new HashMap<>();
    private static final Map<String, Long> LAST_ACCESS = new HashMap<>();
    private static final double NANO_TO_SECONDS = 1.0E-9;
    private static final long TRACK_IDLE_NANOS = 20_000_000_000L;
    private static final int CLEANUP_INTERVAL = 120;
    private static int cleanupCounter;

    private RuffyAnimation() {}

    /**
     * Spring interpolation for every animated UI state.
     * The implementation is deliberately main-thread-only: all callers are
     * Minecraft client render/tick callbacks, so a global monitor is unnecessary.
     */
    public static float animate(String key, float target) {
        long now = System.nanoTime();
        SpringTrack track = SPRINGS.get(key);
        if (track == null) {
            track = new SpringTrack(0.0F, 0.0F, now);
            SPRINGS.put(key, track);
            LAST_ACCESS.put(key, now);
            maybeCleanup(now);
            return 0.0F;
        }

        LAST_ACCESS.put(key, now);
        maybeCleanup(now);
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
                (now - track.lastNanos) * NANO_TO_SECONDS));
        track.lastNanos = now;
        if (dt <= 0.0) return track.value;

        // Closed-form critically damped spring: no frame-rate-dependent lerp,
        // overshoot, or unstable integration after a dropped frame.
        double response = 0.075 + strength * 0.00235;
        double omega = 4.8 / response;
        double displacement = track.value - target;
        double velocityTerm = track.velocity + omega * displacement;
        double decay = Math.exp(-omega * dt);
        double nextDisplacement = (displacement + velocityTerm * dt) * decay;
        double nextVelocity = (track.velocity - omega * velocityTerm * dt) * decay;

        track.value = (float) (target + nextDisplacement);
        track.velocity = (float) nextVelocity;

        if (Math.abs(nextDisplacement) < 0.0008
                && Math.abs(nextVelocity) < 0.006) {
            track.value = target;
            track.velocity = 0.0F;
        }

        return track.value;
    }

    private static void maybeCleanup(long now) {
        if (++cleanupCounter >= CLEANUP_INTERVAL) {
            cleanupCounter = 0;
            cleanup(now);
        }
    }

    /** Immediately places an animation at a known value. */
    public static void set(String key, float value) {
        long now = System.nanoTime();
        SpringTrack track = SPRINGS.get(key);
        if (track == null) {
            SPRINGS.put(key, new SpringTrack(value, 0.0F, now));
        } else {
            track.value = value;
            track.velocity = 0.0F;
            track.lastNanos = now;
        }
        LAST_ACCESS.put(key, now);
    }

    /** Starts a timed animation with a smooth quintic curve. */
    public static float timeline(float progress) {
        return smoothStep(progress);
    }

    /** Quintic smoothstep: smooth start and finish with no sharp velocity jump. */
    public static float smoothStep(float progress) {
        float t = clamp01(progress);
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    /** Cubic ease-out used by expanding effects and quick transitions. */
    public static float easeOut(float progress) {
        float inverse = 1.0F - clamp01(progress);
        return 1.0F - inverse * inverse * inverse;
    }

    /** Cubic ease-in used by staggered fades and exits. */
    public static float easeIn(float progress) {
        float t = clamp01(progress);
        return t * t * t;
    }

    /** Smooth periodic pulse, with caller-controlled frequency and phase. */
    public static float breathe(double timeSeconds, double frequency, float phase) {
        double wave = Math.sin(timeSeconds * frequency * Math.PI * 2.0 + phase);
        return (float) (0.5 + 0.5 * wave);
    }

    /** Shared monotonic clock so every Ruffy effect uses one time source. */
    public static double timeSeconds() {
        return System.nanoTime() * NANO_TO_SECONDS;
    }

    /** Interpolates between two values using a clamped amount. */
    public static float lerp(float from, float to, float amount) {
        float t = clamp01(amount);
        return from + (to - from) * t;
    }

    /** Smooths an arbitrary changing value with an exponential, frame-rate independent response. */
    public static float damp(float current, float target, float response, float deltaSeconds) {
        double dt = Math.max(0.0, Math.min(0.1, deltaSeconds));
        double speed = Math.max(0.001, response);
        float amount = (float) (1.0 - Math.exp(-speed * dt));
        return lerp(current, target, amount);
    }

    /** Interpolates angles along the shortest circular route. */
    public static float angleLerp(float from, float to, float amount) {
        float delta = (to - from) % 360.0F;
        if (delta > 180.0F) delta -= 360.0F;
        if (delta < -180.0F) delta += 360.0F;
        return from + delta * clamp01(amount);
    }

    /** Creates one reusable 3D rotation instead of recalculating trig per vertex. */
    public static Rotation3 rotation3D(double yaw, double pitch, double roll) {
        return new Rotation3(yaw, pitch, roll);
    }

    /** Rotates a single 3D point around yaw (Y), pitch (X), then roll (Z), in radians. */
    public static Vec3 rotate3D(Vec3 point, double yaw, double pitch, double roll) {
        return rotation3D(yaw, pitch, roll).transform(point);
    }

    /** Blend ARGB channels without shaders or an external animation library. */
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

    /** Remove dormant one-shot tracks without walking the maps on every animation call. */
    private static void cleanup(long now) {
        if (SPRINGS.size() < 128) return;
        long cutoff = now - TRACK_IDLE_NANOS;
        Iterator<Map.Entry<String, Long>> iterator = LAST_ACCESS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < cutoff) {
                SPRINGS.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    /** Precomputed rotation matrix for reusing the same transform across a mesh. */
    public static final class Rotation3 {
        private final double cy;
        private final double sy;
        private final double cp;
        private final double sp;
        private final double cr;
        private final double sr;

        private Rotation3(double yaw, double pitch, double roll) {
            cy = Math.cos(yaw);
            sy = Math.sin(yaw);
            cp = Math.cos(pitch);
            sp = Math.sin(pitch);
            cr = Math.cos(roll);
            sr = Math.sin(roll);
        }

        public Vec3 transform(Vec3 point) {
            double x1 = point.x * cy - point.z * sy;
            double z1 = point.x * sy + point.z * cy;
            double y1 = point.y;

            double y2 = y1 * cp - z1 * sp;
            double z2 = y1 * sp + z1 * cp;

            double x3 = x1 * cr - y2 * sr;
            double y3 = x1 * sr + y2 * cr;
            return new Vec3(x3, y3, z2);
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
