package de.ruffy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/** Main client entrypoint for Ruffy Visuals. All added effects are local cosmetics. */
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
        LOGGER.info("Ruffy Visuals initialized: multi-palette HUD and visual effects ready");
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
        boolean onGround = player.onGround();
        boolean swinging = player.swinging;
        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeedSq = velocity.horizontalDistanceSqr();

        // Soft nebula particles: sparse and unobtrusive.
        if (RuffyConfig.get("nebula_particles") && tick % 8 == 0) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double radius = 1.0 + RANDOM.nextDouble() * 1.3;
            client.level.addParticle(ParticleTypes.PORTAL,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.35 + RANDOM.nextDouble() * 1.5,
                    player.getZ() + Math.sin(angle) * radius,
                    0.0, 0.018, 0.0);
        }

        // Orbit shards move around the player's upper body.
        if (RuffyConfig.get("orbit_particles") && tick % 3 == 0) {
            double angle = tick * 0.11;
            double radius = 0.62;
            double height = 0.72 + Math.sin(angle * 1.7) * 0.42;
            client.level.addParticle(ParticleTypes.END_ROD,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + height,
                    player.getZ() + Math.sin(angle) * radius,
                    0.0, 0.008, 0.0);
        }

        if (RuffyConfig.get("movement_trail") && horizontalSpeedSq > 0.006 && tick % 2 == 0) {
            client.level.addParticle(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 0.12, player.getZ(),
                    -velocity.x * 0.08, 0.012, -velocity.z * 0.08);
        }

        if (RuffyConfig.get("jump_burst") && wasOnGround && !onGround) {
            spawnRing(client, player, ParticleTypes.END_ROD, 12, 0.48, 0.025);
        }

        if (RuffyConfig.get("landing_ring") && !wasOnGround && onGround) {
            spawnRing(client, player, ParticleTypes.ELECTRIC_SPARK, 18, 0.72, 0.012);
        }

        if (RuffyConfig.get("sprint_dust") && player.isSprinting()
                && horizontalSpeedSq > 0.004 && tick % 3 == 0) {
            double length = Math.max(0.001, Math.sqrt(horizontalSpeedSq));
            double backX = player.getX() - velocity.x / length * 0.34;
            double backZ = player.getZ() - velocity.z / length * 0.34;
            client.level.addParticle(ParticleTypes.CLOUD,
                    backX, player.getY() + 0.08, backZ,
                    -velocity.x * 0.12, 0.015, -velocity.z * 0.12);
        }

        if (RuffyConfig.get("step_sparks") && onGround
                && horizontalSpeedSq > 0.004 && tick % 5 == 0) {
            double side = RANDOM.nextBoolean() ? 0.18 : -0.18;
            double yaw = Math.toRadians(player.getYRot() + 90.0F);
            client.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    player.getX() + Math.cos(yaw) * side,
                    player.getY() + 0.06,
                    player.getZ() + Math.sin(yaw) * side,
                    0.0, 0.018, 0.0);
        }

        if (RuffyConfig.get("critical_sparks") && !onGround
                && velocity.y < -0.04 && tick % 4 == 0) {
            double x = player.getX() + (RANDOM.nextDouble() - 0.5) * 0.55;
            double y = player.getY() + player.getBbHeight() * 0.58;
            double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * 0.55;
            client.level.addParticle(ParticleTypes.CRIT, x, y, z,
                    (RANDOM.nextDouble() - 0.5) * 0.035, 0.025,
                    (RANDOM.nextDouble() - 0.5) * 0.035);
        }

        if (RuffyConfig.get("low_health_aura") && player.getHealth() <= 6.0F && tick % 7 == 0) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double radius = 0.6 + RANDOM.nextDouble() * 0.45;
            client.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX() + Math.cos(angle) * radius,
                    player.getY() + 0.2 + RANDOM.nextDouble() * 1.2,
                    player.getZ() + Math.sin(angle) * radius,
                    0.0, 0.012, 0.0);
        }

        if (RuffyConfig.get("potion_glow") && !player.getActiveEffects().isEmpty() && tick % 6 == 0) {
            client.level.addParticle(ParticleTypes.ENCHANT,
                    player.getX() + (RANDOM.nextDouble() - 0.5) * 0.9,
                    player.getY() + 0.35 + RANDOM.nextDouble() * 1.1,
                    player.getZ() + (RANDOM.nextDouble() - 0.5) * 0.9,
                    0.0, 0.025, 0.0);
        }

        if (RuffyConfig.get("damage_pulse") && player.getHealth() < previousHealth) {
            for (int i = 0; i < 9; i++) {
                double angle = (Math.PI * 2.0 * i) / 9.0;
                client.level.addParticle(ParticleTypes.DAMAGE_INDICATOR,
                        player.getX() + Math.cos(angle) * 0.55,
                        player.getY() + 0.75,
                        player.getZ() + Math.sin(angle) * 0.55,
                        0.0, 0.04, 0.0);
            }
        }

        if (RuffyConfig.get("underwater_bubbles") && player.isUnderWater() && tick % 3 == 0) {
            client.level.addParticle(ParticleTypes.BUBBLE,
                    player.getX() + (RANDOM.nextDouble() - 0.5) * 0.8,
                    player.getY() + 0.3 + RANDOM.nextDouble() * 1.0,
                    player.getZ() + (RANDOM.nextDouble() - 0.5) * 0.8,
                    0.0, 0.035, 0.0);
        }

        if (RuffyConfig.get("weather_fx") && client.level.isRainingAt(player.blockPosition())
                && tick % 3 == 0) {
            client.level.addParticle(ParticleTypes.DRIPPING_WATER,
                    player.getX() + (RANDOM.nextDouble() - 0.5) * 5.0,
                    player.getY() + 2.2 + RANDOM.nextDouble(),
                    player.getZ() + (RANDOM.nextDouble() - 0.5) * 5.0,
                    0.0, -0.02, 0.0);
        }

        if (RuffyConfig.get("cave_dust") && player.getY() < 32.0 && tick % 9 == 0) {
            client.level.addParticle(ParticleTypes.ASH,
                    player.getX() + (RANDOM.nextDouble() - 0.5) * 4.0,
                    player.getY() + 0.5 + RANDOM.nextDouble() * 1.6,
                    player.getZ() + (RANDOM.nextDouble() - 0.5) * 4.0,
                    0.0, 0.006, 0.0);
        }

        if (RuffyConfig.get("starfall") && tick % 13 == 0) {
            client.level.addParticle(ParticleTypes.END_ROD,
                    player.getX() + (RANDOM.nextDouble() - 0.5) * 8.0,
                    player.getY() + 3.0 + RANDOM.nextDouble() * 3.0,
                    player.getZ() + (RANDOM.nextDouble() - 0.5) * 8.0,
                    (RANDOM.nextDouble() - 0.5) * 0.025, -0.055,
                    (RANDOM.nextDouble() - 0.5) * 0.025);
        }

        if (RuffyConfig.get("hit_sparks") && swinging && !wasSwinging) {
            spawnImpactSlash(client, player);
        }

        previousHealth = player.getHealth();
        wasOnGround = onGround;
        wasSwinging = swinging;
        tick++;
    }

    private static void spawnImpactSlash(Minecraft client, LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        double centerX = player.getX() + look.x * 0.92;
        double centerY = player.getY() + player.getBbHeight() * 0.62 + look.y * 0.18;
        double centerZ = player.getZ() + look.z * 0.92;

        client.level.addParticle(ParticleTypes.SWEEP_ATTACK,
                centerX, centerY, centerZ, 0.0, 0.0, 0.0);

        for (int i = 0; i < 11; i++) {
            double t = i / 10.0;
            double side = (t - 0.5) * 0.82;
            double lift = Math.sin(t * Math.PI) * 0.22;
            client.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    centerX + look.z * side,
                    centerY + lift,
                    centerZ - look.x * side,
                    look.x * 0.025 + (RANDOM.nextDouble() - 0.5) * 0.025,
                    0.018 + RANDOM.nextDouble() * 0.025,
                    look.z * 0.025 + (RANDOM.nextDouble() - 0.5) * 0.025);
        }

        for (int i = 0; i < 4; i++) {
            client.level.addParticle(ParticleTypes.CRIT,
                    centerX + (RANDOM.nextDouble() - 0.5) * 0.22,
                    centerY + (RANDOM.nextDouble() - 0.5) * 0.18,
                    centerZ + (RANDOM.nextDouble() - 0.5) * 0.22,
                    0.0, 0.02, 0.0);
        }
    }

    private static void spawnRing(Minecraft client, LocalPlayer player,
                                  net.minecraft.core.particles.SimpleParticleType particle,
                                  int count, double radius, double verticalVelocity) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i) / count;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            client.level.addParticle(particle,
                    player.getX() + dx * radius,
                    player.getY() + 0.08,
                    player.getZ() + dz * radius,
                    dx * 0.022, verticalVelocity, dz * 0.022);
        }
    }
}
