package de.ruffy.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/** Main client entrypoint for the standalone, visual-only Ruffy client. */
public final class RuffyClient implements ClientModInitializer {
    public static final String MOD_ID = "ruffyclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Random RANDOM = new Random();
    private static KeyMapping openMenuKey;
    private static int tick;
    private static boolean wasOnGround = true;

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
            return;
        }

        var player = client.player;
        boolean onGround = player.onGround();

        if (RuffyConfig.get("nebula_particles") && tick % 7 == 0) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double radius = 1.15 + RANDOM.nextDouble() * 1.2;
            double x = player.getX() + Math.cos(angle) * radius;
            double y = player.getY() + 0.25 + RANDOM.nextDouble() * 1.65;
            double z = player.getZ() + Math.sin(angle) * radius;
            client.level.addParticle(ParticleTypes.PORTAL, x, y, z, 0.0, 0.025, 0.0);
        }

        if (RuffyConfig.get("movement_trail") && player.getDeltaMovement().horizontalDistanceSqr() > 0.006) {
            client.level.addParticle(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 0.12, player.getZ(),
                    0.0, 0.012, 0.0);
        }

        if (RuffyConfig.get("jump_burst") && wasOnGround && !onGround) {
            for (int i = 0; i < 9; i++) {
                double angle = (Math.PI * 2.0 * i) / 9.0;
                double vx = Math.cos(angle) * 0.045;
                double vz = Math.sin(angle) * 0.045;
                client.level.addParticle(ParticleTypes.END_ROD,
                        player.getX() + Math.cos(angle) * 0.35,
                        player.getY() + 0.08,
                        player.getZ() + Math.sin(angle) * 0.35,
                        vx, 0.025, vz);
            }
        }

        wasOnGround = onGround;
        tick++;
    }
}
