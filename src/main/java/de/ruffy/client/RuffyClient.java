package de.ruffy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Client-side visual effects engine. Effects are local cosmetic particles only;
 * no attack timing, reach, hit registration or server behaviour is modified.
 */
public final class RuffyClient implements ClientModInitializer {
    public static final String MOD_ID = "ruffyclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Random RANDOM = new Random();

    private static KeyMapping openMenuKey;
    private static int tick;
    private static boolean wasOnGround = true;
    private static boolean wasSwinging;
    private static float previousHealth = 20.0F;

    @Override
    public void onInitializeClient() {
        RuffyConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.ruffyclient.open_menu",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "overlay"),
                (graphics, deltaTracker) -> RuffyHud.render(graphics)
        );

        ClientTickEvents.END_CLIENT_TICK.register(RuffyClient::tick);
        LOGGER.info("Ruffy Visuals initialized: compact GUI and enhanced effects engine ready");
    }

    private static void tick(Minecraft client) {
        while (openMenuKey.consumeClick()) {
            if (client.screen instanceof RuffyClickGui) {
                client.setScreen(null);
            } else {
                client.setScreen(new RuffyClickGui());
            }
        }

        if (client.player == null || client.level == null) {
            wasOnGround = true;
            wasSwinging = false;
            previousHealth = 20.0F;
            return;
        }

        LocalPlayer player = client.player;
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeedSq = velocity.horizontalDistanceSqr();
        boolean onGround = player.onGround();
        boolean swinging = player.swinging;
        int quality = RuffyConfig.getInt("effect_quality");
        int detail = quality == 0 ? 1 : quality == 1 ? 2 : 3;
        int lowFrequency = quality == 0 ? 14 : quality == 1 ? 9 : 6;

        // Every continuous particle module fades in/out by easing its emission rate.
        float nebula = effectAmount("nebula_particles");
        float orbit = effectAmount("orbit_particles");
        float trail = effectAmount("movement_trail");
        float jump = effectAmount("jump_burst");
        float landing = effectAmount("landing_ring");
        float sprintDust = effectAmount("sprint_dust");
        float footsteps = effectAmount("step_sparks");
        float airSparks = effectAmount("critical_sparks");
        float lowHealth = effectAmount("low_health_aura");
        float potionGlow = effectAmount("potion_glow");
        float damagePulse = effectAmount("damage_pulse");
        float bubbles = effectAmount("underwater_bubbles");
        float weather = effectAmount("weather_fx");
        float caveDust = effectAmount("cave_dust");
        float starfall = effectAmount("starfall");
        float impact = effectAmount("hit_sparks");

        // Ambient nebula: slow drifting points with a faint enchanted shimmer.
        if (shouldEmit(nebula, tick, lowFrequency)) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double radius = 0.95 + RANDOM.nextDouble() * 1.45;
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + 0.25 + RANDOM.nextDouble() * 1.7;
            double z = player.getZ() + Math.sin(angle) * radius;
            client.level.addParticle(ParticleTypes.PORTAL, x, y, z,
                    -Math.cos(angle) * 0.008, 0.018, -Math.sin(angle) * 0.008);
            spawnPaletteDust(client, x, y, z, -Math.cos(angle) * 0.006, 0.016,
                    -Math.sin(angle) * 0.006, 0.72F, false);
            if (quality >= 1 && tick % (lowFrequency * 2) == 0) {
                client.level.addParticle(ParticleTypes.ENCHANT,
                        x + (RANDOM.nextDouble() - 0.5) * 0.25,
                        y + 0.12, z + (RANDOM.nextDouble() - 0.5) * 0.25,
                        0.0, 0.012, 0.0);
            }
        }

        // Two counter-rotating orbit points create a more stable halo than a single particle.
        if (shouldEmit(orbit, tick, 2)) {
            double angle = tick * 0.095;
            double radius = 0.66;
            for (int i = 0; i < detail; i++) {
                double a = angle + (Math.PI * 2.0 * i / detail);
                double height = 0.72 + Math.sin(a * 1.7) * 0.38;
                double px = player.getX() + Math.cos(a) * radius;
                double py = player.getY() + height;
                double pz = player.getZ() + Math.sin(a) * radius;
                client.level.addParticle(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT,
                        px, py, pz, -Math.sin(a) * 0.012, 0.006, Math.cos(a) * 0.012);
                spawnPaletteDust(client, px, py, pz, -Math.sin(a) * 0.008,
                        0.004, Math.cos(a) * 0.008, 0.62F, i % 2 == 0);
            }
        }

        // Smooth trailing ribbons: three small points are spaced behind the motion vector.
        if (trail > 0.01F && horizontalSpeedSq > 0.006
                && shouldEmit(trail, tick, quality == 0 ? 4 : 2)) {
            double speed = Math.sqrt(horizontalSpeedSq);
            double backX = -velocity.x / Math.max(0.001, speed);
            double backZ = -velocity.z / Math.max(0.001, speed);
            double sideX = -backZ * 0.12;
            double sideZ = backX * 0.12;
            for (int i = 0; i < detail; i++) {
                double distance = 0.18 + i * 0.16;
                double px = player.getX() + backX * distance + sideX * (i - 1);
                double py = player.getY() + 0.12 + i * 0.05;
                double pz = player.getZ() + backZ * distance + sideZ * (i - 1);
                client.level.addParticle(ParticleTypes.END_ROD, px, py, pz,
                        -velocity.x * 0.025, 0.008, -velocity.z * 0.025);
                spawnPaletteDust(client, px, py, pz, -velocity.x * 0.018,
                        0.006, -velocity.z * 0.018, 0.55F, i % 2 == 1);
            }
        }

        // Jump: expanding ground ring plus a small upward core burst.
        if (jump > 0.01F && wasOnGround && !onGround) {
            spawnRing(client, player, ParticleTypes.END_ROD, scaledCount(12 * detail, jump), 0.46, 0.025, 0.08);
            spawnRing(client, player, ParticleTypes.ELECTRIC_SPARK, scaledCount(8 * detail, jump), 0.25, 0.045, 0.13);
            spawnPaletteRing(client, player, scaledCount(12 * detail, jump), 0.38, 0.035, 0.1);
            spawnVerticalBurst(client, player, Math.max(1, scaledCount(detail, jump)));
        }

        // Landing: two offset rings and a short dust puff produce a layered impact.
        if (landing > 0.01F && !wasOnGround && onGround) {
            spawnRing(client, player, ParticleTypes.ELECTRIC_SPARK, scaledCount(14 * detail, landing), 0.72, 0.012, 0.06);
            spawnRing(client, player, ParticleTypes.CLOUD, scaledCount(10 * detail, landing), 0.48, 0.009, 0.04);
            spawnPaletteRing(client, player, scaledCount(16 * detail, landing), 0.62, 0.012, 0.055);
            client.level.addParticle(ParticleTypes.POOF,
                    player.getX(), player.getY() + 0.06, player.getZ(),
                    0.0, 0.018, 0.0);
        }

        // Sprint dust sits behind the player and spreads slightly to both sides.
        if (sprintDust > 0.01F && player.isSprinting()
                && horizontalSpeedSq > 0.004 && shouldEmit(sprintDust, tick, quality == 0 ? 5 : 3)) {
            double speed = Math.max(0.001, Math.sqrt(horizontalSpeedSq));
            double backX = -velocity.x / speed;
            double backZ = -velocity.z / speed;
            for (int i = 0; i < scaledCount(detail, sprintDust); i++) {
                double side = (i - (scaledCount(detail, sprintDust) - 1) / 2.0) * 0.23;
                double px = player.getX() + backX * 0.32 - backZ * side;
                double py = player.getY() + 0.06;
                double pz = player.getZ() + backZ * 0.32 + backX * side;
                client.level.addParticle(ParticleTypes.CLOUD, px, py, pz,
                        backX * 0.035, 0.012, backZ * 0.035);
                if (i == 0) spawnPaletteDust(client, px, py, pz,
                        backX * 0.025, 0.008, backZ * 0.025, 0.48F, false);
            }
        }

        // Tiny directional footstep sparks at alternating sides.
        if (footsteps > 0.01F && onGround
                && horizontalSpeedSq > 0.004 && shouldEmit(footsteps, tick, quality == 0 ? 9 : 5)) {
            double side = ((tick / 5) % 2 == 0 ? 1.0 : -1.0) * 0.19;
            double yaw = Math.toRadians(player.getYRot() + 90.0F);
            double px = player.getX() + Math.cos(yaw) * side;
            double py = player.getY() + 0.055;
            double pz = player.getZ() + Math.sin(yaw) * side;
            client.level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz,
                    -velocity.x * 0.03, 0.02, -velocity.z * 0.03);
            spawnPaletteDust(client, px, py, pz, -velocity.x * 0.02,
                    0.018, -velocity.z * 0.02, 0.58F, tick % 2 == 0);
        }

        // Falling/critical sparks drift around the player's lower body.
        if (airSparks > 0.01F && !onGround
                && velocity.y < -0.04 && shouldEmit(airSparks, tick, quality == 0 ? 7 : 4)) {
            for (int i = 0; i < scaledCount(detail, airSparks); i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2.0;
                double radius = 0.18 + RANDOM.nextDouble() * 0.34;
                client.level.addParticle(ParticleTypes.CRIT,
                        player.getX() + Math.cos(angle) * radius,
                        player.getY() + player.getBbHeight() * 0.48,
                        player.getZ() + Math.sin(angle) * radius,
                        Math.cos(angle) * 0.018, 0.026, Math.sin(angle) * 0.018);
            }
        }

        // Low health halo climbs in a narrow spiral around the player.
        if (lowHealth > 0.01F && player.getHealth() <= 6.0F
                && shouldEmit(lowHealth, tick, quality == 0 ? 8 : 5)) {
            double angle = tick * 0.18;
            double radius = 0.58 + Math.sin(angle * 1.4) * 0.08;
            double px = player.getX() + Math.cos(angle) * radius;
            double py = player.getY() + 0.25 + Math.floorMod(tick, 28) / 28.0 * 1.35;
            double pz = player.getZ() + Math.sin(angle) * radius;
            client.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz,
                    0.0, 0.015, 0.0);
            spawnPaletteDust(client, px, py, pz, 0.0, 0.012, 0.0, 0.82F, true);
        }

        // Potion glow: little enchant particles orbit the torso instead of random positions.
        if (potionGlow > 0.01F && !player.getActiveEffects().isEmpty()
                && shouldEmit(potionGlow, tick, quality == 0 ? 10 : 6)) {
            double angle = tick * 0.16;
            double radius = 0.43;
            double px = player.getX() + Math.cos(angle) * radius;
            double py = player.getY() + 0.55 + Math.sin(angle * 1.7) * 0.55;
            double pz = player.getZ() + Math.sin(angle) * radius;
            client.level.addParticle(ParticleTypes.ENCHANT, px, py, pz,
                    0.0, 0.022, 0.0);
            spawnPaletteDust(client, px, py, pz, 0.0, 0.018, 0.0, 0.5F, false);
        }

        // Damage pulse: short radial burst only when health actually decreases.
        if (damagePulse > 0.01F && player.getHealth() < previousHealth) {
            int pulseCount = scaledCount(10 * detail, damagePulse);
            for (int i = 0; i < pulseCount; i++) {
                double angle = (Math.PI * 2.0 * i) / pulseCount;
                double px = player.getX() + Math.cos(angle) * 0.58;
                double py = player.getY() + 0.72 + Math.sin(angle * 2.0) * 0.08;
                double pz = player.getZ() + Math.sin(angle) * 0.58;
                client.level.addParticle(ParticleTypes.DAMAGE_INDICATOR, px, py, pz,
                        Math.cos(angle) * 0.025, 0.035, Math.sin(angle) * 0.025);
                if (i % 2 == 0) spawnPaletteDust(client, px, py, pz,
                        Math.cos(angle) * 0.018, 0.025, Math.sin(angle) * 0.018, 0.54F, true);
            }
        }

        // Underwater bubbles rise from around the player at a restrained interval.
        if (shouldEmit(bubbles, tick, quality == 0 ? 6 : 3) && player.isUnderWater()) {
            client.level.addParticle(ParticleTypes.BUBBLE,
                    player.getX() + (RANDOM.nextDouble() - 0.5) * 0.8,
                    player.getY() + 0.25 + RANDOM.nextDouble() * 1.0,
                    player.getZ() + (RANDOM.nextDouble() - 0.5) * 0.8,
                    (RANDOM.nextDouble() - 0.5) * 0.012, 0.035,
                    (RANDOM.nextDouble() - 0.5) * 0.012);
        }

        // Local weather detail; vanilla weather itself is left untouched.
        if (shouldEmit(weather, tick, quality == 0 ? 6 : 3)
                && client.level.isRainingAt(player.blockPosition())) {
            client.level.addParticle(ParticleTypes.DRIPPING_WATER,
                    player.getX() + (RANDOM.nextDouble() - 0.5) * 5.0,
                    player.getY() + 2.0 + RANDOM.nextDouble(),
                    player.getZ() + (RANDOM.nextDouble() - 0.5) * 5.0,
                    0.0, -0.02, 0.0);
        }

        // Dust is faint and sparse so it does not flood dark scenes.
        if (shouldEmit(caveDust, tick, quality == 0 ? 14 : 9) && player.getY() < 32.0) {
            client.level.addParticle(ParticleTypes.ASH,
                    player.getX() + (RANDOM.nextDouble() - 0.5) * 4.0,
                    player.getY() + 0.5 + RANDOM.nextDouble() * 1.6,
                    player.getZ() + (RANDOM.nextDouble() - 0.5) * 4.0,
                    (RANDOM.nextDouble() - 0.5) * 0.008, 0.006,
                    (RANDOM.nextDouble() - 0.5) * 0.008);
        }

        // Starfall: short multi-point streaks outside the player's immediate field.
        if (shouldEmit(starfall, tick, quality == 0 ? 18 : 12)) {
            double sx = player.getX() + (RANDOM.nextDouble() - 0.5) * 8.0;
            double sy = player.getY() + 3.0 + RANDOM.nextDouble() * 3.0;
            double sz = player.getZ() + (RANDOM.nextDouble() - 0.5) * 8.0;
            for (int i = 0; i < scaledCount(detail, starfall); i++) {
                client.level.addParticle(ParticleTypes.END_ROD,
                        sx - i * 0.08, sy + i * 0.14, sz,
                        0.012, -0.06, 0.0);
            }
        }

        // Client-side slash particles trigger on the start of a swing animation.
        if (impact > 0.01F && swinging && !wasSwinging) {
            spawnImpactSlash(client, player, Math.max(1, scaledCount(detail, impact)));
        }

        previousHealth = player.getHealth();
        wasOnGround = onGround;
        wasSwinging = swinging;
        tick++;
    }

    private static float effectAmount(String key) {
        return RuffyAnimation.animate("effect." + key,
                RuffyConfig.get(key) ? 1.0F : 0.0F);
    }

    private static boolean shouldEmit(float amount, int currentTick, int baseInterval) {
        if (amount <= 0.01F) return false;
        int interval = Math.max(baseInterval,
                Math.round(baseInterval / Math.max(0.01F, amount)));
        return currentTick % interval == 0;
    }

    private static int scaledCount(int baseCount, float amount) {
        return Math.max(1, Math.round(baseCount * Math.max(0.0F, Math.min(1.0F, amount))));
    }

    private static void spawnImpactSlash(Minecraft client, LocalPlayer player, int detail) {
        Vec3 look = player.getLookAngle();
        double centerX = player.getX() + look.x * 0.92;
        double centerY = player.getY() + player.getBbHeight() * 0.62 + look.y * 0.18;
        double centerZ = player.getZ() + look.z * 0.92;

        client.level.addParticle(ParticleTypes.SWEEP_ATTACK,
                centerX, centerY, centerZ, 0.0, 0.0, 0.0);

        // Curved slash arc, followed by a small secondary spark burst.
        int arcCount = 7 + detail * 4;
        for (int i = 0; i < arcCount; i++) {
            double t = i / (double) (arcCount - 1);
            double side = (t - 0.5) * 0.92;
            double lift = Math.sin(t * Math.PI) * 0.26;
            double x = centerX + look.z * side;
            double y = centerY + lift;
            double z = centerZ - look.x * side;
            double vx = look.x * 0.025 + (RANDOM.nextDouble() - 0.5) * 0.025;
            double vy = 0.018 + RANDOM.nextDouble() * 0.03;
            double vz = look.z * 0.025 + (RANDOM.nextDouble() - 0.5) * 0.025;
            client.level.addParticle(i % 3 == 0
                            ? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK,
                    x, y, z, vx, vy, vz);
            spawnPaletteDust(client, x, y, z, vx * 0.75, vy * 0.7,
                    vz * 0.75, 0.6F, i % 2 == 0);
        }

        for (int i = 0; i < 3 * detail; i++) {
            client.level.addParticle(ParticleTypes.CRIT,
                    centerX + (RANDOM.nextDouble() - 0.5) * 0.28,
                    centerY + (RANDOM.nextDouble() - 0.5) * 0.22,
                    centerZ + (RANDOM.nextDouble() - 0.5) * 0.28,
                    (RANDOM.nextDouble() - 0.5) * 0.04, 0.024,
                    (RANDOM.nextDouble() - 0.5) * 0.04);
        }
    }

    private static void spawnPaletteDust(Minecraft client, double x, double y, double z,
                                         double vx, double vy, double vz,
                                         float scale, boolean secondary) {
        float paletteAmount = effectAmount("palette_sparks");
        if (paletteAmount <= 0.01F) return;
        scale *= paletteAmount;
        int color = (secondary ? RuffyConfig.secondaryColor() : RuffyConfig.primaryColor()) & 0x00FFFFFF;
        client.level.addParticle(new DustParticleOptions(color, scale), x, y, z, vx, vy, vz);
    }

    private static void spawnPaletteRing(Minecraft client, LocalPlayer player, int count,
                                         double radius, double velocity, double lift) {
        float paletteAmount = effectAmount("palette_sparks");
        if (paletteAmount <= 0.01F) return;
        count = scaledCount(count, paletteAmount);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            double r = radius + Math.sin(angle * 3.0) * 0.035;
            spawnPaletteDust(client, player.getX() + dx * r,
                    player.getY() + 0.065, player.getZ() + dz * r,
                    dx * velocity, lift, dz * velocity,
                    0.55F + (i % 3) * 0.08F, i % 2 == 0);
        }
    }

    private static void spawnVerticalBurst(Minecraft client, LocalPlayer player, int detail) {
        for (int i = 0; i < 5 * detail; i++) {
            double angle = (Math.PI * 2.0 * i) / (5 * detail);
            double radius = 0.22 + RANDOM.nextDouble() * 0.2;
            client.level.addParticle(ParticleTypes.WITCH,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.18,
                    player.getZ() + Math.sin(angle) * radius,
                    Math.cos(angle) * 0.015, 0.06,
                    Math.sin(angle) * 0.015);
        }
    }

    private static void spawnRing(Minecraft client, LocalPlayer player,
                                  net.minecraft.core.particles.SimpleParticleType particle,
                                  int count, double radius, double verticalVelocity,
                                  double radiusVelocity) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i) / count;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            client.level.addParticle(particle,
                    player.getX() + dx * radius,
                    player.getY() + 0.065,
                    player.getZ() + dz * radius,
                    dx * radiusVelocity, verticalVelocity, dz * radiusVelocity);
        }
    }
}
