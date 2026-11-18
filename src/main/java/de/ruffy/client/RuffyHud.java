package de.ruffy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Matching Crimson Red control-center HUD: transparent charcoal chips,
 * fine neon outlines and compact input/status panels.
 */
public final class RuffyHud {
    private static final int WHITE = 0xFFF7F2FB;
    private static final int MUTED = 0xFFC1B6C9;
    private static final int CHIP_HEIGHT = 23;
    private static final int CHIP_GAP = 5;

    private RuffyHud() {}

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.options.hideGui) return;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        HudCursor cursor = new HudCursor(12, 12, screenWidth - 12);

        if (RuffyConfig.get("watermark")) {
            cursor.drawBrand(graphics, client);
        }

        if (RuffyConfig.get("fps")) {
            cursor.draw(graphics, client, "fps", "FPS  " + client.getFps(),
                    RuffyConfig.secondaryColor());
        }

        if (RuffyConfig.get("coordinates")) {
            String coords = "XYZ  " + player.blockPosition().getX() + "  "
                    + player.blockPosition().getY() + "  " + player.blockPosition().getZ();
            cursor.draw(graphics, client, "coordinates", coords, RuffyConfig.primaryColor());
        }

        if (RuffyConfig.get("direction")) {
            String[] directions = {"S", "W", "N", "E"};
            int index = Math.floorMod(Math.round(player.getYRot() / 90.0F), 4);
            cursor.draw(graphics, client, "direction", "DIR  " + directions[index],
                    RuffyConfig.secondaryColor());
        }

        if (RuffyConfig.get("speed")) {
            double speed = Math.sqrt(player.getDeltaMovement().horizontalDistanceSqr()) * 20.0;
            cursor.draw(graphics, client, "speed", String.format(Locale.ROOT, "SPD  %.1f B/S", speed),
                    RuffyConfig.primaryColor());
        }

        if (RuffyConfig.get("world_clock") && client.level != null) {
            long time = Math.floorMod(client.level.getDayTime() + 6000L, 24000L);
            int hours = (int) (time / 1000L);
            int minutes = (int) ((time % 1000L) * 60L / 1000L);
            cursor.draw(graphics, client, "world-clock",
                    String.format(Locale.ROOT, "WORLD  %02d:%02d", hours, minutes),
                    RuffyConfig.secondaryColor());
        }

        if (RuffyConfig.get("health_hud")) {
            float health = player.getHealth();
            float maxHealth = Math.max(1.0F, player.getMaxHealth());
            int healthColor = health / maxHealth <= 0.25F ? 0xFFFF365B
                    : health / maxHealth <= 0.5F ? 0xFFFF9A65 : RuffyConfig.primaryColor();
            cursor.drawBar(graphics, client, "health",
                    String.format(Locale.ROOT, "HP  %.1f / %.0f", health, maxHealth),
                    healthColor, health / maxHealth);
        }

        if (RuffyConfig.get("armor_hud")) {
            cursor.draw(graphics, client, "armor", "ARMOR  " + player.getArmorValue(),
                    RuffyConfig.secondaryColor());
        }

        if (RuffyConfig.get("keystrokes")) {
            int keyY = client.getWindow().getGuiScaledHeight() - 80;
            drawKeystrokes(graphics, client, 12, keyY);
        }
    }

    private static final class HudCursor {
        private int x;
        private int y;
        private final int startX;
        private final int rightLimit;

        private HudCursor(int x, int y, int rightLimit) {
            this.x = x;
            this.y = y;
            this.startX = x;
            this.rightLimit = Math.max(x + 70, rightLimit);
        }

        private void nextLineIfNeeded(int chipWidth) {
            if (x + chipWidth > rightLimit && x > startX) {
                x = startX;
                y += CHIP_HEIGHT + CHIP_GAP;
            }
        }

        private void drawBrand(GuiGraphics graphics, Minecraft client) {
            float opacity = RuffyAnimation.animate("hud.brand", 1.0F);
            int width = 104;
            nextLineIfNeeded(width);
            panel(graphics, x, y, width, CHIP_HEIGHT, RuffyConfig.primaryColor(), opacity);
            graphics.fill(x + 6, y + 4, x + 21, y + 19,
                    withAlpha(RuffyConfig.primaryColor(), Math.round(55 * opacity)));
            graphics.renderOutline(x + 6, y + 4, 15, 15,
                    withAlpha(RuffyConfig.primaryColor(), Math.round(180 * opacity)));
            graphics.drawCenteredString(client.font, "R", x + 13, y + 7,
                    RuffyAnimation.multiplyAlpha(RuffyConfig.primaryColor(), opacity));
            graphics.drawString(client.font, "RUFFY", x + 27, y + 4,
                    RuffyAnimation.multiplyAlpha(WHITE, opacity), false);
            graphics.drawString(client.font, "VISUALS", x + 27, y + 14,
                    RuffyAnimation.multiplyAlpha(RuffyConfig.primaryColor(), opacity), false);
            x += width + CHIP_GAP;
        }

        private void draw(GuiGraphics graphics, Minecraft client,
                          String animationId, String text, int accent) {
            float opacity = RuffyAnimation.animate("hud.chip." + animationId, 1.0F);
            int maxTextWidth = Math.max(42, rightLimit - startX - 18);
            String visible = text;
            while (visible.length() > 3 && client.font.width(visible) > maxTextWidth) {
                visible = visible.substring(0, visible.length() - 1);
            }
            int width = client.font.width(visible) + 18;
            nextLineIfNeeded(width);
            panel(graphics, x, y, width, CHIP_HEIGHT, accent, opacity);
            graphics.drawString(client.font, visible, x + 9, y + 7,
                    RuffyAnimation.multiplyAlpha(WHITE, opacity), false);
            x += width + CHIP_GAP;
        }

        private void drawBar(GuiGraphics graphics, Minecraft client, String animationId,
                             String text, int accent, float progress) {
            float opacity = RuffyAnimation.animate("hud.chip." + animationId, 1.0F);
            float smoothProgress = RuffyAnimation.animate("hud.bar." + animationId,
                    Math.max(0.0F, Math.min(1.0F, progress)));
            int width = client.font.width(text) + 20;
            nextLineIfNeeded(width);
            panel(graphics, x, y, width, CHIP_HEIGHT, accent, opacity);
            graphics.drawString(client.font, text, x + 9, y + 4,
                    RuffyAnimation.multiplyAlpha(WHITE, opacity), false);
            int barLeft = x + 9;
            int barRight = x + width - 9;
            graphics.fill(barLeft, y + 17, barRight, y + 19, RuffyAnimation.multiplyAlpha(0x80534A5C, opacity));
            int fill = Math.max(0, Math.min(barRight - barLeft,
                    Math.round((barRight - barLeft) * smoothProgress)));
            graphics.fill(barLeft, y + 17, barLeft + fill, y + 19,
                    RuffyAnimation.multiplyAlpha(withAlpha(accent, 235), opacity));
            x += width + CHIP_GAP;
        }
    }

    private static void panel(GuiGraphics graphics, int x, int y,
                              int width, int height, int accent, float opacity) {
        graphics.fill(x - 2, y - 1, x + width + 2, y + height + 1,
                withAlpha(accent, Math.round(15 * opacity)));
        graphics.fill(x, y, x + width, y + height,
                RuffyAnimation.multiplyAlpha(RuffyConfig.hudPanelColor(), opacity));
        graphics.renderOutline(x, y, width, height,
                RuffyAnimation.multiplyAlpha(RuffyConfig.hudBorderColor(), opacity));
        graphics.fill(x, y + 4, x + 2, y + height - 4,
                withAlpha(accent, Math.round(220 * opacity)));
        graphics.fill(x + 3, y, x + width - 3, y + 1,
                withAlpha(accent, Math.round(75 * opacity)));
    }

    private static void drawKeystrokes(GuiGraphics graphics, Minecraft client, int x, int y) {
        int keyW = 24;
        int keyH = 20;
        int gap = 3;
        int panelW = keyW * 3 + gap * 2;
        int panelH = keyH * 3 + gap * 2 + 18;

        float inputOpacity = RuffyAnimation.animate("hud.input.panel", 1.0F);
        graphics.fill(x - 5, y - 5, x + panelW + 5, y + panelH + 4,
                withAlpha(RuffyConfig.primaryColor(), Math.round(15 * inputOpacity)));
        graphics.fill(x - 3, y - 3, x + panelW + 3, y + panelH + 2,
                RuffyAnimation.multiplyAlpha(RuffyConfig.hudPanelColor(), inputOpacity));
        graphics.renderOutline(x - 3, y - 3, panelW + 6, panelH + 5,
                RuffyAnimation.multiplyAlpha(RuffyConfig.hudBorderColor(), inputOpacity));
        graphics.drawString(client.font, "INPUT", x + 2, y - 1,
                RuffyAnimation.multiplyAlpha(RuffyConfig.primaryColor(), inputOpacity), false);

        int keysY = y + 12;
        drawKey(graphics, client, "key.w", "W", x + keyW + gap, keysY,
                keyW, keyH, client.options.keyUp.isDown());
        drawKey(graphics, client, "key.a", "A", x, keysY + keyH + gap,
                keyW, keyH, client.options.keyLeft.isDown());
        drawKey(graphics, client, "key.s", "S", x + keyW + gap, keysY + keyH + gap,
                keyW, keyH, client.options.keyDown.isDown());
        drawKey(graphics, client, "key.d", "D", x + (keyW + gap) * 2, keysY + keyH + gap,
                keyW, keyH, client.options.keyRight.isDown());

        drawKey(graphics, client, "key.jump", "JUMP", x, keysY + (keyH + gap) * 2,
                keyW * 2 + gap, 16, client.options.keyJump.isDown());
        drawKey(graphics, client, "key.sprint", "SPR", x + (keyW + gap) * 2,
                keysY + (keyH + gap) * 2, keyW, 16,
                client.options.keySprint.isDown());
    }

    private static void drawKey(GuiGraphics graphics, Minecraft client, String animationId,
                                String label, int x, int y, int width, int height, boolean pressed) {
        float active = RuffyAnimation.animate("hud.input." + animationId, pressed ? 1.0F : 0.0F);
        int accent = RuffyConfig.primaryColor();
        if (active > 0.01F) {
            graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2,
                    withAlpha(accent, Math.round(40 * active)));
        }
        int fill = RuffyAnimation.blendColor(RuffyConfig.hudPanelColor(),
                withAlpha(accent, 175), active);
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.renderOutline(x, y, width, height,
                RuffyAnimation.blendColor(RuffyConfig.hudBorderColor(),
                        withAlpha(RuffyConfig.secondaryColor(), 230), active));
        if (active > 0.01F) {
            graphics.fill(x + 3, y + 2, x + width - 3, y + 3,
                    withAlpha(accent, Math.round(230 * active)));
        }
        int textColor = RuffyAnimation.blendColor(MUTED, WHITE, active);
        graphics.drawCenteredString(client.font, label,
                x + width / 2, y + Math.max(3, (height - 8) / 2), textColor);
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
