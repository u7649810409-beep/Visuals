package de.ruffy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Fully custom Ruffy visual-effects renderer.
 *
 * Nothing in this class uses Minecraft ParticleTypes or ClientLevel.addParticle.
 * Effects are simulated in Java and drawn as custom translucent line geometry
 * through Fabric's world-render event.
 */
public final class RuffyVisualEffects {
    private static final Random RANDOM = new Random();
    private static final List<GlowPoint> POINTS = new ArrayList<>();
    private static final List<PulseRing> RINGS = new ArrayList<>();
    private static final List<SlashArc> SLASHES = new ArrayList<>();

    // Hard caps keep burst spam and long sessions from growing render work.
    private static final int MAX_POINTS = 180;
    private static final int MAX_RINGS = 12;
    private static final int MAX_SLASHES = 4;
    private static final double TAU = Math.PI * 2.0;
    private static final long CLIENT_TICK_NANOS = 50_000_000L;
    private static final int CIRCLE_SAMPLES = 48;
    private static final double[] CIRCLE_COS = new double[CIRCLE_SAMPLES];
    private static final double[] CIRCLE_SIN = new double[CIRCLE_SAMPLES];

    static {
        for (int i = 0; i < CIRCLE_SAMPLES; i++) {
            double angle = TAU * i / CIRCLE_SAMPLES;
            CIRCLE_COS[i] = Math.cos(angle);
            CIRCLE_SIN[i] = Math.sin(angle);
        }
    }

    private static long tick;
    private static boolean wasGrounded = true;
    private static boolean wasSwinging;
    private static float previousHealth = 20.0F;
    private static int ambientCounter;

    private RuffyVisualEffects() {}

    /** Called once per client tick to simulate and spawn Ruffy's own effects. */
    public static synchronized void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            POINTS.clear();
            RINGS.clear();
            SLASHES.clear();
            wasGrounded = true;
            wasSwinging = false;
            previousHealth = 20.0F;
            tick = 0L;
            ambientCounter = 0;
            return;
        }

        LocalPlayer player = client.player;
        Vec3 position = player.position();
        Vec3 velocity = player.getDeltaMovement();
        boolean grounded = player.onGround();
        boolean swinging = player.swinging;
        double horizontalSpeed = Math.sqrt(velocity.horizontalDistanceSqr());

        int quality = RuffyConfig.getInt("effect_quality");
        int detail = quality == 0 ? 1 : quality == 1 ? 2 : 3;
        int ambientInterval = quality == 0 ? 12 : quality == 1 ? 8 : 5;

        float ambient = amount("nebula_particles");
        float orbit = amount("orbit_particles");
        float trail = amount("movement_trail");
        float jump = amount("jump_burst");
        float landing = amount("landing_ring");
        float sprint = amount("sprint_dust");
        float footsteps = amount("step_sparks");
        float air = amount("critical_sparks");
        float lowHealth = amount("low_health_aura");
        float potion = amount("potion_glow");
        float damage = amount("damage_pulse");
        float bubbles = amount("underwater_bubbles");
        float weather = amount("weather_fx");
        float cave = amount("cave_dust");
        float starfall = amount("starfall");
        float impact = amount("hit_sparks");

        // Slow drifting glints around the player, using Ruffy's own point integrator.
        if (shouldEmit(ambient, ambientCounter++, ambientInterval)) {
            double angle = RANDOM.nextDouble() * TAU;
            double radius = 0.9 + RANDOM.nextDouble() * 1.35;
            Vec3 p = position.add(Math.cos(angle) * radius,
                    0.25 + RANDOM.nextDouble() * 1.7, Math.sin(angle) * radius);
            addPoint(p, new Vec3(-Math.cos(angle) * 0.006, 0.012,
                    -Math.sin(angle) * 0.006), randomAccent(), 0.018F, 42, 0.994F, -0.00005F);
        }

        // Two counter-rotating orbital loops are drawn as custom smooth curves in render().
        if (orbit > 0.01F && tick % (quality == 0 ? 4 : 2) == 0) {
            double angle = tick * 0.075;
            for (int i = 0; i < detail; i++) {
                double a = angle + TAU * i / detail;
                Vec3 p = position.add(Math.cos(a) * 0.66,
                        0.72 + Math.sin(a * 1.7) * 0.38,
                        Math.sin(a) * 0.66);
                addPoint(p, new Vec3(-Math.sin(a) * 0.012, 0.004,
                        Math.cos(a) * 0.012), i % 2 == 0
                        ? RuffyConfig.primaryColor() : RuffyConfig.secondaryColor(),
                        0.026F, 34, 0.988F, 0.0F);
            }
        }

        // Movement ribbon: tiny points are emitted behind the real velocity vector.
        if (trail > 0.01F && horizontalSpeed > 0.08
                && shouldEmit(trail, (int) tick, quality == 0 ? 4 : 2)) {
            Vec3 back = new Vec3(-velocity.x / horizontalSpeed, 0.0,
                    -velocity.z / horizontalSpeed);
            Vec3 side = new Vec3(-back.z, 0.0, back.x);
            for (int i = 0; i < scaledCount(detail, trail); i++) {
                double distance = 0.18 + i * 0.16;
                Vec3 p = position.add(back.scale(distance))
                        .add(side.scale((i - (detail - 1) * 0.5) * 0.12))
                        .add(0.0, 0.12 + i * 0.045, 0.0);
                addPoint(p, back.scale(0.012).add(0.0, 0.006, 0.0),
                        i % 2 == 0 ? RuffyConfig.primaryColor()
                                : RuffyConfig.secondaryColor(),
                        0.022F + i * 0.004F, 20, 0.955F, 0.0F);
            }
        }

        // Jump: two expanding rings and an upward radial burst.
        if (jump > 0.01F && wasGrounded && !grounded) {
            addRing(position.add(0.0, 0.045, 0.0), 0.28, 1.05,
                    17, RuffyConfig.primaryColor(), 0.36F);
            addRing(position.add(0.0, 0.055, 0.0), 0.16, 0.72,
                    13, RuffyConfig.secondaryColor(), 0.29F);
            burst(position.add(0.0, 0.18, 0.0), scaledCount(15 * detail, jump),
                    0.10, 0.16, RuffyConfig.secondaryColor(), 24, 0.002F);
        }

        // Landing uses a broad red shock ring and a second inner pulse.
        if (landing > 0.01F && !wasGrounded && grounded) {
            addRing(position.add(0.0, 0.055, 0.0), 0.24, 1.22,
                    20, RuffyConfig.primaryColor(), 0.38F);
            addRing(position.add(0.0, 0.065, 0.0), 0.18, 0.78,
                    16, RuffyConfig.secondaryColor(), 0.30F);
            burst(position.add(0.0, 0.08, 0.0), scaledCount(10 * detail, landing),
                    0.18, 0.09, RuffyConfig.primaryColor(), 19, 0.004F);
        }

        // Sprint particles are narrow streaks that follow the ground plane.
        if (sprint > 0.01F && player.isSprinting() && horizontalSpeed > 0.07
                && shouldEmit(sprint, (int) tick, quality == 0 ? 5 : 3)) {
            Vec3 back = new Vec3(-velocity.x / horizontalSpeed, 0.0,
                    -velocity.z / horizontalSpeed);
            Vec3 side = new Vec3(-back.z, 0.0, back.x);
            for (int i = 0; i < scaledCount(detail, sprint); i++) {
                double spread = (i - (detail - 1) * 0.5) * 0.19;
                Vec3 p = position.add(back.scale(0.32)).add(side.scale(spread))
                        .add(0.0, 0.055, 0.0);
                addPoint(p, back.scale(0.028).add(0.0, 0.014, 0.0),
                        RuffyConfig.secondaryColor(), 0.034F, 18, 0.90F, 0.001F);
            }
        }

        // Alternating left/right footstep glints.
        if (footsteps > 0.01F && grounded && horizontalSpeed > 0.045
                && shouldEmit(footsteps, (int) tick, quality == 0 ? 9 : 5)) {
            double sideSign = ((tick / 5) % 2 == 0) ? 1.0 : -1.0;
            double yaw = Math.toRadians(player.getYRot() + 90.0F);
            Vec3 p = position.add(Math.cos(yaw) * sideSign * 0.19,
                    0.055, Math.sin(yaw) * sideSign * 0.19);
            addPoint(p, new Vec3(-velocity.x * 0.05, 0.026, -velocity.z * 0.05),
                    RuffyConfig.primaryColor(), 0.026F, 16, 0.91F, 0.0004F);
        }

        // Air sparks are small custom streaks around the lower body.
        if (air > 0.01F && !grounded && velocity.y < -0.04
                && shouldEmit(air, (int) tick, quality == 0 ? 7 : 4)) {
            for (int i = 0; i < scaledCount(detail, air); i++) {
                double a = RANDOM.nextDouble() * TAU;
                double r = 0.18 + RANDOM.nextDouble() * 0.34;
                Vec3 p = position.add(Math.cos(a) * r,
                        player.getBbHeight() * 0.48, Math.sin(a) * r);
                addPoint(p, new Vec3(Math.cos(a) * 0.012, 0.024,
                        Math.sin(a) * 0.012), randomAccent(), 0.021F, 22, 0.975F, 0.0001F);
            }
        }

        // Critical-health aura follows a rising helical path.
        if (lowHealth > 0.01F && player.getHealth() <= 6.0F
                && shouldEmit(lowHealth, (int) tick, quality == 0 ? 8 : 5)) {
            double angle = tick * 0.20;
            double y = 0.20 + Math.floorMod((int) tick, 30) / 30.0 * 1.45;
            Vec3 p = position.add(Math.cos(angle) * 0.58, y, Math.sin(angle) * 0.58);
            addPoint(p, new Vec3(-Math.sin(angle) * 0.008, 0.018,
                    Math.cos(angle) * 0.008), 0xFFFF3B5E, 0.031F, 29, 0.982F, 0.0F);
        }

        // Potion effect: a small double helix around the torso.
        if (potion > 0.01F && !player.getActiveEffects().isEmpty()
                && shouldEmit(potion, (int) tick, quality == 0 ? 10 : 6)) {
            double angle = tick * 0.17;
            for (int strand = 0; strand < 2; strand++) {
                double a = angle + strand * Math.PI;
                Vec3 p = position.add(Math.cos(a) * 0.43,
                        0.60 + Math.sin(a * 1.7) * 0.48,
                        Math.sin(a) * 0.43);
                addPoint(p, new Vec3(0.0, 0.018, 0.0),
                        strand == 0 ? RuffyConfig.primaryColor()
                                : RuffyConfig.secondaryColor(),
                        0.019F, 32, 0.986F, 0.0F);
            }
        }

        // Damage produces a quickly expanding crimson ring, not a vanilla damage particle.
        if (damage > 0.01F && player.getHealth() < previousHealth) {
            addRing(position.add(0.0, 0.75, 0.0), 0.20, 0.96,
                    18, 0xFFFF4565, 0.28F);
            burst(position.add(0.0, 0.78, 0.0), scaledCount(20 * detail, damage),
                    0.25, 0.16, 0xFFFF4565, 23, 0.001F);
        }

        // Underwater bubbles are our own outlined circles with an upward drift.
        if (bubbles > 0.01F && player.isUnderWater()
                && shouldEmit(bubbles, (int) tick, quality == 0 ? 7 : 4)) {
            Vec3 p = position.add((RANDOM.nextDouble() - 0.5) * 0.8,
                    0.25 + RANDOM.nextDouble(), (RANDOM.nextDouble() - 0.5) * 0.8);
            addPoint(p, new Vec3((RANDOM.nextDouble() - 0.5) * 0.006, 0.034,
                    (RANDOM.nextDouble() - 0.5) * 0.006),
                    0xFFB6E8FF, 0.027F, 42, 0.995F, 0.0F, Shape.RING);
        }

        // Rain streaks are custom line segments and only appear during real rain.
        if (weather > 0.01F && client.level.isRainingAt(player.blockPosition())
                && shouldEmit(weather, (int) tick, quality == 0 ? 7 : 4)) {
            Vec3 p = position.add((RANDOM.nextDouble() - 0.5) * 5.0,
                    2.0 + RANDOM.nextDouble(), (RANDOM.nextDouble() - 0.5) * 5.0);
            addPoint(p, new Vec3(0.0, -0.24, 0.0), 0x99FF647D,
                    0.014F, 12, 0.995F, 0.0F, Shape.STREAK);
        }

        // Cave ambience floats as soft custom glints instead of ASH particles.
        if (cave > 0.01F && player.getY() < 32.0
                && shouldEmit(cave, (int) tick, quality == 0 ? 14 : 9)) {
            Vec3 p = position.add((RANDOM.nextDouble() - 0.5) * 4.0,
                    0.5 + RANDOM.nextDouble() * 1.6,
                    (RANDOM.nextDouble() - 0.5) * 4.0);
            addPoint(p, new Vec3((RANDOM.nextDouble() - 0.5) * 0.006,
                    0.008, (RANDOM.nextDouble() - 0.5) * 0.006),
                    randomAccent(), 0.014F, 64, 0.998F, 0.0F);
        }

        // Starfall consists of fine custom streaks above the nearby player.
        if (starfall > 0.01F && shouldEmit(starfall, (int) tick,
                quality == 0 ? 18 : 12)) {
            Vec3 p = position.add((RANDOM.nextDouble() - 0.5) * 8.0,
                    3.0 + RANDOM.nextDouble() * 3.0,
                    (RANDOM.nextDouble() - 0.5) * 8.0);
            addPoint(p, new Vec3(0.018, -0.075, 0.0),
                    randomAccent(), 0.020F, 40, 0.995F, 0.0F, Shape.STREAK);
        }

        // The slash is a custom bezier-like arc generated entirely by Ruffy.
        if (impact > 0.01F && swinging && !wasSwinging) {
            spawnSlash(player, detail);
        }

        // Simulate on the game tick, but timestamp each step for render-frame interpolation.
        long stepNanos = System.nanoTime();
        for (Iterator<GlowPoint> it = POINTS.iterator(); it.hasNext();) {
            GlowPoint point = it.next();
            point.step(stepNanos);
            if (point.age >= point.life) it.remove();
        }
        for (Iterator<PulseRing> it = RINGS.iterator(); it.hasNext();) {
            PulseRing ring = it.next();
            ring.step(stepNanos);
            if (ring.age >= ring.life) it.remove();
        }
        for (Iterator<SlashArc> it = SLASHES.iterator(); it.hasNext();) {
            SlashArc slash = it.next();
            slash.step(stepNanos);
            if (slash.age >= slash.life) it.remove();
        }

        previousHealth = player.getHealth();
        wasGrounded = grounded;
        wasSwinging = swinging;
        tick++;
    }

    /** Draw custom meshes in Fabric's world-render pass. */
    public static synchronized void render(WorldRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        if (context == null || client.player == null || client.level == null
                || context.matrices() == null || context.consumers() == null
                || client.screen instanceof RuffyClickGui
                || client.screen instanceof RuffySpotifyScreen) {
            return;
        }
        if (POINTS.isEmpty() && RINGS.isEmpty() && SLASHES.isEmpty()
                && !RuffyConfig.get("orbit_particles")
                && !RuffyConfig.get("prism_core") && !RuffyConfig.get("orbit_drones")) {
            return;
        }

        LocalPlayer player = client.player;
        Vec3 camera = context.gameRenderer().getMainCamera().position();
        int quality = RuffyConfig.getInt("effect_quality");
        int pointBudget = quality == 0 ? 24 : quality == 1 ? 48 : 96;
        double renderDistance = quality == 0 ? 16.0 : quality == 1 ? 24.0 : 32.0;
        double renderDistanceSqr = renderDistance * renderDistance;
        // If the local player is outside the quality-tier radius, none of the
        // player-attached geometry can contribute to the current camera view.
        if (distanceSqr(camera, player.position()) > renderDistanceSqr) return;

        PoseStack matrices = context.matrices();
        matrices.pushPose();
        try {
            matrices.translate(-camera.x, -camera.y, -camera.z);
            PoseStack.Pose pose = matrices.last();
            VertexConsumer lines = context.consumers().getBuffer(RenderTypes.LINES_TRANSLUCENT);
            if (lines == null) return;

            long renderNanos = System.nanoTime();
            double timeSeconds = renderNanos * 1.0E-9;

            // Draw only recent, nearby points. Older particles are still simulated
            // and expire normally, but distant points never consume GPU vertices.
            for (int i = RINGS.size() - 1; i >= 0; i--) {
                PulseRing ring = RINGS.get(i);
                if (distanceSqr(camera, ring.center) <= renderDistanceSqr) {
                    drawRing(lines, pose, ring, renderNanos, quality);
                }
            }
            for (int i = SLASHES.size() - 1; i >= 0; i--) {
                SlashArc slash = SLASHES.get(i);
                if (distanceSqr(camera, slash.center) <= renderDistanceSqr) {
                    drawSlash(lines, pose, slash, renderNanos, quality);
                }
            }
            int renderedPoints = 0;
            for (int i = POINTS.size() - 1; i >= 0 && renderedPoints < pointBudget; i--) {
                GlowPoint point = POINTS.get(i);
                if (distanceSqr(camera, point.position) > renderDistanceSqr) continue;
                drawPoint(lines, pose, point, renderNanos, quality);
                renderedPoints++;
            }
            if (RuffyConfig.get("orbit_particles")) {
                drawOrbit(lines, pose, player, quality, timeSeconds);
            }
            if (RuffyConfig.get("prism_core")) {
                drawPrismCore(lines, pose, player, quality, timeSeconds);
            }
            if (RuffyConfig.get("orbit_drones") && quality > 0) {
                drawOrbitDrones(lines, pose, player, timeSeconds);
            }
        } finally {
            matrices.popPose();
        }
    }

    private static void spawnSlash(LocalPlayer player, int detail) {
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 upWorld = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(upWorld);
        if (right.lengthSqr() < 0.001) right = new Vec3(1.0, 0.0, 0.0);
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        Vec3 center = player.position().add(0.0, player.getBbHeight() * 0.63, 0.0)
                .add(forward.scale(0.88));
        if (SLASHES.size() >= MAX_SLASHES) SLASHES.remove(0);
        SLASHES.add(new SlashArc(center, right, up, 0.94, 0.30,
                RuffyConfig.primaryColor(), RuffyConfig.secondaryColor(),
                11 + detail * 3));
        burst(center, 5 * detail, 0.12, 0.11,
                RuffyConfig.secondaryColor(), 16, 0.0015F);
    }

    private static void addRing(Vec3 center, double start, double end,
                                int life, int color, float width) {
        if (RINGS.size() >= MAX_RINGS) RINGS.remove(0);
        RINGS.add(new PulseRing(center, start, end, life, color, width));
    }

    private static void burst(Vec3 center, int count, double speed, double lift,
                              int color, int life, float gravity) {
        for (int i = 0; i < Math.min(count, 80); i++) {
            double a = RANDOM.nextDouble() * TAU;
            double vertical = (RANDOM.nextDouble() - 0.5) * lift;
            double radial = speed * (0.45 + RANDOM.nextDouble() * 0.8);
            Vec3 velocity = new Vec3(Math.cos(a) * radial, vertical,
                    Math.sin(a) * radial);
            addPoint(center.add((RANDOM.nextDouble() - 0.5) * 0.12,
                            (RANDOM.nextDouble() - 0.5) * 0.10,
                            (RANDOM.nextDouble() - 0.5) * 0.12),
                    velocity, color, 0.020F + RANDOM.nextFloat() * 0.015F,
                    life, 0.965F, gravity);
        }
    }

    private static void addPoint(Vec3 position, Vec3 velocity, int color,
                                 float size, int life, float drag, float gravity) {
        addPoint(position, velocity, color, size, life, drag, gravity, Shape.GLINT);
    }

    private static void addPoint(Vec3 position, Vec3 velocity, int color,
                                 float size, int life, float drag, float gravity, Shape shape) {
        if (POINTS.size() >= MAX_POINTS) POINTS.remove(0);
        POINTS.add(new GlowPoint(position, velocity, color, size, life, drag, gravity, shape));
    }

    private static float amount(String key) {
        return RuffyAnimation.animate("effect." + key,
                RuffyConfig.get(key) ? 1.0F : 0.0F);
    }

    private static boolean shouldEmit(float amount, int currentTick, int interval) {
        if (amount <= 0.015F) return false;
        int adjusted = Math.max(interval, Math.round(interval / Math.max(0.02F, amount)));
        return currentTick % adjusted == 0;
    }

    private static int scaledCount(int count, float amount) {
        return Math.max(1, Math.round(count * Math.max(0.0F, Math.min(1.0F, amount))));
    }

    private static int randomAccent() {
        return RANDOM.nextBoolean() ? RuffyConfig.primaryColor() : RuffyConfig.secondaryColor();
    }

    private static void drawOrbit(VertexConsumer out, PoseStack.Pose pose,
                                   LocalPlayer player, int quality, double time) {
        float opacity = amountForRender("orbit_particles");
        if (opacity <= 0.01F) return;
        int segments = quality == 0 ? 12 : quality == 1 ? 24 : 48;
        int stride = CIRCLE_SAMPLES / segments;
        Vec3 base = player.position().add(0.0, 0.95, 0.0);
        int alpha = Math.round(150 * opacity);
        for (int ring = 0; ring < 2; ring++) {
            double radius = ring == 0 ? 0.72 : 0.54;
            double tilt = ring == 0 ? 0.28 : -0.42;
            int color = ring == 0 ? RuffyConfig.primaryColor() : RuffyConfig.secondaryColor();
            Vec3 previous = null;
            for (int i = 0; i <= segments; i++) {
                int index = (i * stride) % CIRCLE_SAMPLES;
                double x = CIRCLE_COS[index] * radius;
                double z = CIRCLE_SIN[index] * radius;
                double angle = TAU * index / CIRCLE_SAMPLES;
                double y = Math.sin((angle + time) * 2.0 + time) * 0.13;
                Vec3 next = base.add(x, y + z * tilt, z);
                if (previous != null) {
                    drawLine(out, pose, previous, next, withAlpha(color, alpha), 1.35F);
                }
                previous = next;
            }
        }
    }

    /**
     * Custom 3D prism. Mesh transforms are prepared once per object and
     * the latitude loops use a small, quality-scaled, precomputed circle.
     */
    private static void drawPrismCore(VertexConsumer out, PoseStack.Pose pose,
                                      LocalPlayer player, int quality, double time) {
        float opacity = amountForRender("prism_core");
        if (opacity <= 0.01F) return;

        Vec3 center = player.position().add(0.0, 1.13, 0.0);
        drawOctahedron(out, pose, center, 0.52,
                time, 0.48 + Math.sin(time * 0.7) * 0.16, -time * 0.62,
                withAlpha(RuffyConfig.primaryColor(), Math.round(225 * opacity)), 1.55F);
        drawOctahedron(out, pose, center, 0.37,
                -time * 0.72, time * 0.54, time * 0.88,
                withAlpha(RuffyConfig.secondaryColor(), Math.round(175 * opacity)), 1.2F);

        Vec3[] cube = cubeVertices(center, 0.20, time * 0.8, time * 0.5, 0.0);
        drawEdges(out, pose, cube, CUBE_EDGES,
                withAlpha(0xFFFFF0F4, Math.round(140 * opacity)), 0.9F);

        int segments = quality == 0 ? 12 : quality == 1 ? 24 : 24;
        int stride = CIRCLE_SAMPLES / segments;
        int loopCount = quality == 0 ? 1 : quality == 1 ? 2 : 3;
        for (int loop = 0; loop < loopCount; loop++) {
            double radius = 0.72 + loop * 0.055;
            double tilt = (loop - 1) * 0.52;
            RuffyAnimation.Rotation3 rotation =
                    RuffyAnimation.rotation3D(time * 0.2, tilt, time * 0.08);
            Vec3 previous = null;
            for (int i = 0; i <= segments; i++) {
                int index = (i * stride) % CIRCLE_SAMPLES;
                double angle = TAU * index / CIRCLE_SAMPLES;
                double y = Math.sin(angle * 2.0 + time) * 0.10;
                Vec3 local = new Vec3(CIRCLE_COS[index] * radius,
                        y, CIRCLE_SIN[index] * radius);
                Vec3 next = center.add(rotation.transform(local));
                if (previous != null) {
                    int color = loop == 1 ? RuffyConfig.secondaryColor()
                            : RuffyConfig.primaryColor();
                    int loopAlpha = Math.round((quality == 0 ? 68 : 82 - loop * 10) * opacity);
                    drawLine(out, pose, previous, next, withAlpha(color, loopAlpha), 0.9F);
                }
                previous = next;
            }
        }
    }

    /** Three small rotating crystals; only rendered at medium/high quality. */
    private static void drawOrbitDrones(VertexConsumer out, PoseStack.Pose pose,
                                        LocalPlayer player, double time) {
        float opacity = amountForRender("orbit_drones");
        if (opacity <= 0.01F) return;

        Vec3 body = player.position().add(0.0, 1.00, 0.0);
        for (int i = 0; i < 3; i++) {
            double angle = time + TAU * i / 3.0;
            double radius = 1.12 + Math.sin(time * 1.2 + i) * 0.08;
            Vec3 center = body.add(Math.cos(angle) * radius,
                    0.25 + Math.sin(angle * 1.8) * 0.30,
                    Math.sin(angle) * radius);
            int color = i == 0 ? RuffyConfig.primaryColor()
                    : i == 1 ? RuffyConfig.secondaryColor() : 0xFFFFE8EF;
            drawOctahedron(out, pose, center, 0.17 + i * 0.012,
                    -angle * 1.6, angle * 0.8, time * (i % 2 == 0 ? 1.2 : -1.1),
                    withAlpha(color, Math.round(205 * opacity)), 1.05F);
            drawLine(out, pose, body, center,
                    withAlpha(color, Math.round(38 * opacity)), 0.7F);
        }
    }



    private static void drawOctahedron(VertexConsumer out, PoseStack.Pose pose,
                                       Vec3 center, double radius,
                                       double yaw, double pitch, double roll,
                                       int color, float width) {
        Vec3[] vertices = new Vec3[6];
        vertices[0] = new Vec3(radius, 0.0, 0.0);
        vertices[1] = new Vec3(-radius, 0.0, 0.0);
        vertices[2] = new Vec3(0.0, radius, 0.0);
        vertices[3] = new Vec3(0.0, -radius, 0.0);
        vertices[4] = new Vec3(0.0, 0.0, radius);
        vertices[5] = new Vec3(0.0, 0.0, -radius);
        RuffyAnimation.Rotation3 rotation = RuffyAnimation.rotation3D(yaw, pitch, roll);
        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = center.add(rotation.transform(vertices[i]));
        }
        drawEdges(out, pose, vertices, OCTAHEDRON_EDGES, color, width);
    }

    private static Vec3[] cubeVertices(Vec3 center, double radius,
                                       double yaw, double pitch, double roll) {
        Vec3[] vertices = new Vec3[8];
        RuffyAnimation.Rotation3 rotation = RuffyAnimation.rotation3D(yaw, pitch, roll);
        for (int i = 0; i < 8; i++) {
            Vec3 local = new Vec3(
                    (i & 1) == 0 ? -radius : radius,
                    (i & 2) == 0 ? -radius : radius,
                    (i & 4) == 0 ? -radius : radius);
            vertices[i] = center.add(rotation.transform(local));
        }
        return vertices;
    }

    private static void drawEdges(VertexConsumer out, PoseStack.Pose pose,
                                  Vec3[] vertices, int[][] edges,
                                  int color, float width) {
        for (int[] edge : edges) {
            drawLine(out, pose, vertices[edge[0]], vertices[edge[1]], color, width);
        }
    }

    private static final int[][] OCTAHEDRON_EDGES = {
            {0, 2}, {0, 3}, {0, 4}, {0, 5},
            {1, 2}, {1, 3}, {1, 4}, {1, 5},
            {2, 4}, {2, 5}, {3, 4}, {3, 5}
    };

    private static final int[][] CUBE_EDGES = {
            {0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5},
            {2, 3}, {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}
    };

    private static double distanceSqr(Vec3 a, Vec3 b) {
        return a.distanceToSqr(b);
    }

    private static float amountForRender(String key) {
        return RuffyAnimation.animate("effect.render." + key,
                RuffyConfig.get(key) ? 1.0F : 0.0F);
    }

    private static void drawRing(VertexConsumer out, PoseStack.Pose pose,
                                 PulseRing ring, long now, int quality) {
        float progress = lifeProgress(ring.age, ring.life, ring.lastStepNanos, now);
        float radius = (float) RuffyAnimation.lerp((float) ring.startRadius,
                (float) ring.endRadius, RuffyAnimation.easeOut(progress));
        float fade = 1.0F - RuffyAnimation.easeInOut(progress);
        int alpha = Math.round(((ring.color >>> 24) & 0xFF) * fade);
        if (alpha < 3) return;

        int segments = quality == 0 ? 12 : quality == 1 ? 24 : 48;
        int stride = CIRCLE_SAMPLES / segments;
        double y = ring.verticalDrift(progress);
        Vec3 previous = ring.center.add(radius, y, 0.0);
        for (int i = 1; i <= segments; i++) {
            int index = (i * stride) % CIRCLE_SAMPLES;
            Vec3 next = ring.center.add(CIRCLE_COS[index] * radius, y,
                    CIRCLE_SIN[index] * radius);
            drawLine(out, pose, previous, next, withAlpha(ring.color, alpha), ring.width);
            previous = next;
        }
    }

    private static void drawSlash(VertexConsumer out, PoseStack.Pose pose,
                                   SlashArc slash, long now, int quality) {
        float progress = lifeProgress(slash.age, slash.life, slash.lastStepNanos, now);
        float fade = 1.0F - RuffyAnimation.easeInOut(progress);
        if (fade < 0.015F) return;
        float growth = 0.5F + 0.5F * RuffyAnimation.easeOut(Math.min(1.0F, progress * 1.8F));
        int primary = withAlpha(slash.primary, Math.round(255 * fade));
        int secondary = withAlpha(slash.secondary, Math.round(185 * fade));
        Vec3 depth = slash.right.cross(slash.up).normalize().scale(0.13 * growth);
        Vec3 last = slash.point(0.0, growth);
        Vec3 lastGlow = slash.point(0.0, growth * 1.08);
        Vec3 lastBack = last.add(depth);
        int segments = quality == 0 ? 12 : quality == 1 ? 20 : 28;
        boolean volume = quality > 0 && RuffyConfig.get("three_d_slash");
        int ribEvery = quality == 1 ? 5 : 4;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            Vec3 next = slash.point(t, growth);
            Vec3 glow = slash.point(t, growth * 1.08);
            Vec3 nextBack = next.add(depth);
            drawLine(out, pose, lastGlow, glow, withAlpha(secondary, Math.round(65 * fade)), 3.0F);
            drawLine(out, pose, last, next, primary, 1.8F);

            if (volume && i % ribEvery == 0) {
                int ribAlpha = Math.round(90 * fade);
                drawLine(out, pose, last, lastBack, withAlpha(secondary, ribAlpha), 0.9F);
                drawLine(out, pose, next, nextBack, withAlpha(secondary, ribAlpha), 0.9F);
                drawLine(out, pose, lastBack, nextBack, withAlpha(secondary, ribAlpha), 0.8F);
            }
            last = next;
            lastGlow = glow;
            lastBack = nextBack;
        }
    }

    private static void drawPoint(VertexConsumer out, PoseStack.Pose pose,
                                  GlowPoint point, long now, int quality) {
        float progress = lifeProgress(point.age, point.life, point.lastStepNanos, now);
        float fade = 1.0F - RuffyAnimation.easeInOut(progress);
        if (fade < 0.015F) return;
        float pulse = 0.82F + 0.18F * RuffyAnimation.breathe(
                now / 1_000_000_000.0, 0.9, point.phase);
        float size = point.size * (0.75F + 0.25F * RuffyAnimation.easeOut(progress));
        int alpha = Math.round(((point.color >>> 24) & 0xFF) * fade * pulse);
        if (alpha < 3) return;
        int color = (point.color & 0x00FFFFFF) | (alpha << 24);
        double frameBlend = interpolation(point.lastStepNanos, now);
        Vec3 position = point.previousPosition.add(
                point.position.subtract(point.previousPosition).scale(frameBlend));

        if (point.shape == Shape.RING) {
            int segments = quality == 0 ? 6 : quality == 1 ? 8 : 12;
            int stride = CIRCLE_SAMPLES / segments;
            Vec3 previous = position.add(size, 0.0, 0.0);
            for (int i = 1; i <= segments; i++) {
                int index = (i * stride) % CIRCLE_SAMPLES;
                Vec3 next = position.add(CIRCLE_COS[index] * size, 0.0,
                        CIRCLE_SIN[index] * size);
                drawLine(out, pose, previous, next, color, 0.9F);
                previous = next;
            }
            return;
        }

        Vec3 tail = position.subtract(point.velocity.scale(2.8));
        drawLine(out, pose, tail, position, color, 1.0F);
        if (point.shape == Shape.STREAK || quality == 0) return;

        drawLine(out, pose, position.add(-size, 0.0, 0.0),
                position.add(size, 0.0, 0.0), color, 0.9F);
        drawLine(out, pose, position.add(0.0, -size, 0.0),
                position.add(0.0, size, 0.0), color, 0.9F);
        if (quality >= 2) {
            drawLine(out, pose, position.add(0.0, 0.0, -size),
                    position.add(0.0, 0.0, size), color, 0.8F);
        }
    }

    private static void drawLine(VertexConsumer out, PoseStack.Pose pose,
                                 Vec3 from, Vec3 to, int color, float width) {
        if (((color >>> 24) & 0xFF) < 2) return;
        out.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(color).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(width);
        out.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(color).setNormal(0.0F, 1.0F, 0.0F).setLineWidth(width);
    }

    private static float interpolation(long lastStepNanos, long now) {
        return RuffyAnimation.clamp01((float) ((now - lastStepNanos)
                / (double) CLIENT_TICK_NANOS));
    }

    private static float lifeProgress(int age, int life, long lastStepNanos, long now) {
        float partial = interpolation(lastStepNanos, now);
        return RuffyAnimation.clamp01((age + partial) / Math.max(1.0F, life));
    }

    private static int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (clamped << 24);
    }

    private enum Shape { GLINT, RING, STREAK }

    private static final class GlowPoint {
        private final int color;
        private final float size;
        private final int life;
        private final float drag;
        private final float gravity;
        private final Shape shape;
        private final float phase;
        private Vec3 position;
        private Vec3 previousPosition;
        private Vec3 velocity;
        private int age;
        private long lastStepNanos;

        private GlowPoint(Vec3 position, Vec3 velocity, int color,
                          float size, int life, float drag, float gravity, Shape shape) {
            this.position = position;
            this.previousPosition = position;
            this.velocity = velocity;
            this.lastStepNanos = System.nanoTime();
            this.color = color;
            this.size = size;
            this.life = life;
            this.drag = drag;
            this.gravity = gravity;
            this.shape = shape;
            this.phase = RANDOM.nextFloat() * (float) TAU;
        }

        private void step(long now) {
            previousPosition = position;
            position = position.add(velocity);
            velocity = velocity.scale(drag).add(0.0, -gravity, 0.0);
            age++;
            lastStepNanos = now;
        }
    }

    private static final class PulseRing {
        private final Vec3 center;
        private final double startRadius;
        private final double endRadius;
        private final int life;
        private final int color;
        private final float width;
        private int age;
        private long lastStepNanos;

        private PulseRing(Vec3 center, double startRadius, double endRadius,
                          int life, int color, float width) {
            this.center = center;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.life = life;
            this.color = color;
            this.width = width;
            this.lastStepNanos = System.nanoTime();
        }

        private void step(long now) {
            age++;
            lastStepNanos = now;
        }

        private double verticalDrift(float progress) {
            return 0.025 * progress;
        }
    }

    private static final class SlashArc {
        private final Vec3 center;
        private final Vec3 right;
        private final Vec3 up;
        private final double radius;
        private final double bend;
        private final int primary;
        private final int secondary;
        private final int life;
        private int age;
        private long lastStepNanos;

        private SlashArc(Vec3 center, Vec3 right, Vec3 up, double radius,
                         double bend, int primary, int secondary, int life) {
            this.center = center;
            this.right = right;
            this.up = up;
            this.radius = radius;
            this.bend = bend;
            this.primary = primary;
            this.secondary = secondary;
            this.life = life;
            this.lastStepNanos = System.nanoTime();
        }

        private void step(long now) {
            age++;
            lastStepNanos = now;
        }

        private Vec3 point(double progress, double growth) {
            double angle = Math.PI * (0.14 + 0.72 * progress);
            double side = Math.cos(angle) * radius * growth;
            double height = Math.sin(angle) * radius * growth;
            double curve = Math.sin(progress * Math.PI) * bend * growth;
            return center.add(right.scale(side))
                    .add(up.scale(height + curve));
        }
    }
}
