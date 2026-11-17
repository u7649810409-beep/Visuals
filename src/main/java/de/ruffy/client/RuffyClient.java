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

public final class RuffyClient implements ClientModInitializer {
    public static final String MOD_ID = "ruffyclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Random RANDOM = new Random();
    private static KeyMapping openMenuKey;
    private static int tick;
    private static boolean wasOnGround = true;
    private static boolean wasSwinging;

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
        LOGGER.info("Ruffy Visuals client initialized for Minecraft 1.21.11");
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
            return;
        }

        LocalPlayer player = client.player;
        boolean onGround = player.onGround();
        boolean swinging = player.swinging;

        if (RuffyConfig.get("nebula_particles") && tick % 7 == 0) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double radius = 1.15 + RANDOM.nextDouble() * 1.2;
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + 0.25 + RANDOM.nextDouble() * 1.65;
            double z = player.getZ() + Math.sin(angle) * radius;
            client.level.addParticle(ParticleTypes.PORTAL, x, y, z, 0.0, 0.025, 0.0);
        }

        if (RuffyConfig.get("movement_trail")
                && player.getDeltaMovement().horizontalDistanceSqr() > 0.006
                && tick % 2 == 0) {
            client.level.addParticle(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 0.12, player.getZ(),
                    0.0, 0.012, 0.0);
        }

        if (RuffyConfig.get("jump_burst") && wasOnGround && !onGround) {
            spawnRing(client, player, ParticleTypes.END_ROD, 10, 0.48, 0.025);
        }

        if (RuffyConfig.get("landing_ring") && !wasOnGround && onGround) {
            spawnRing(client, player, ParticleTypes.ELECTRIC_SPARK, 16, 0.72, 0.012);
        }

        if (RuffyConfig.get("sprint_dust") && player.isSprinting()
                && player.getDeltaMovement().horizontalDistanceSqr() > 0.004
                && tick % 3 == 0) {
            Vec3 velocity = player.getDeltaMovement();
            double length = Math.max(0.001, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z));
            double backX = player.getX() - velocity.x / length * 0.32;
            double backZ = player.getZ() - velocity.z / length * 0.32;
            client.level.addParticle(ParticleTypes.CLOUD,
                    backX, player.getY() + 0.08, backZ,
                    -velocity.x * 0.12, 0.015, -velocity.z * 0.12);
        }

        if (RuffyConfig.get("critical_sparks") && !onGround
                && player.getDeltaMovement().y < -0.04 && tick % 4 == 0) {
            double x = player.getX() + (RANDOM.nextDouble() - 0.5) * 0.55;
            double y = player.getY() + player.getBbHeight() * 0.58;
            double z = player.getZ() + (RANDOM.nextDouble() - 0.5) * 0.55;
            client.level.addParticle(ParticleTypes.CRIT, x, y, z,
                    (RANDOM.nextDouble() - 0.5) * 0.035, 0.025,
                    (RANDOM.nextDouble() - 0.5) * 0.035);
        }

        if (RuffyConfig.get("hit_sparks") && swinging && !wasSwinging) {
            spawnImpactSlash(client, player);
        }

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

        for (int i = 0; i < 9; i++) {
            double t = i / 8.0;
            double side = (t - 0.5) * 0.78;
            double lift = Math.sin(t * Math.PI) * 0.2;
            double x = centerX + look.z * side;
            double y = centerY + lift;
            double z = centerZ - look.x * side;
            client.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    x, y, z,
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
