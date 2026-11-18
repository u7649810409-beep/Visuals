package de.ruffy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Ruffy's compact status HUD.
 *
 * A single visual language is shared across the top status strip, vitals and
 * movement keys: quiet translucent surfaces, small labels, consistent spacing
 * and a single palette accent. Everything is drawn by Ruffy.
 */
public final class RuffyHud {
    private static final int WHITE = 0xFFF5F4F7;
    private static final int MUTED = 0xFF9E9AA5;
    private static final int SOFT = 0xFFCAC5CF;
    private static final int CHIP_HEIGHT = 31;
    private static final int CHIP_GAP = 4;

    private RuffyHud() {}

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.options.hideGui) return;

        float scale = Math.max(0.75F,
                Math.min(1.20F, RuffyConfig.getInt("hud_scale") / 100.0F));
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);

        int logicalWidth = Math.round(client.getWindow().getGuiScaledWidth() / scale);
        int logicalHeight = Math.round(client.getWindow().getGuiScaledHeight() / scale);
        HudCursor cursor = new HudCursor(12, 12, logicalWidth - 12);

        // Primary status line: identity first, then only the values enabled by the user.
        if (RuffyConfig.get("watermark")) {
            cursor.drawBrand(graphics, client);
        }
        if (RuffyConfig.get("fps")) {
            cursor.drawMetric(graphics, client, "fps", "FPS",
                    Integer.toString(client.getFps()), RuffyConfig.secondaryColor());
        }
        if (RuffyConfig.get("direction")) {
            float yaw = player.getYRot();
            int degrees = Math.floorMod(Math.round(yaw), 360);
            String[] directions = {"S", "W", "N", "E"};
            int index = Math.floorMod(Math.round(yaw / 90.0F), 4);
            cursor.drawMetric(graphics, client, "direction", "FACING",
                    directions[index] + "  " + String.format(Locale.ROOT, "%03d°", degrees),
                    RuffyConfig.primaryColor());
        }
        if (RuffyConfig.get("speed")) {
            double speed = Math.sqrt(player.getDeltaMovement().horizontalDistanceSqr()) * 20.0;
            cursor.drawMetric(graphics, client, "speed", "SPEED",
                    String.format(Locale.ROOT, "%.2f BLK/S", speed), RuffyConfig.secondaryColor());
        }
        if (RuffyConfig.get("world_clock") && client.level != null) {
            long time = Math.floorMod(client.level.getDayTime() + 6000L, 24000L);
            int hours = (int) (time / 1000L);
            int minutes = (int) ((time % 1000L) * 60L / 1000L);
            cursor.drawMetric(graphics, client, "world-clock", "WORLD TIME",
                    String.format(Locale.ROOT, "%02d:%02d", hours, minutes),
                    RuffyConfig.secondaryColor());
        }
        if (RuffyConfig.get("coordinates")) {
            String coords = player.blockPosition().getX() + ", "
                    + player.blockPosition().getY() + ", "
                    + player.blockPosition().getZ();
            cursor.drawMetric(graphics, client, "coordinates", "POSITION",
                    coords, RuffyConfig.primaryColor());
        }

        // Vitals are separate from world telemetry, with eased proportional bars.
        if (RuffyConfig.get("health_hud")) {
            float health = player.getHealth();
            float maximum = Math.max(1.0F, player.getMaxHealth());
            float ratio = clamp01(health / maximum);
            int healthColor = ratio <= 0.25F ? 0xFFFF4D68
                    : ratio <= 0.50F ? 0xFFFFB45C : RuffyConfig.primaryColor();
            cursor.drawVital(graphics, client, "health", "HEALTH",
                    String.format(Locale.ROOT, "%.1f / %.0f", health, maximum),
                    healthColor, ratio);
        }
        if (RuffyConfig.get("armor_hud")) {
            int armor = Math.max(0, Math.min(20, player.getArmorValue()));
            cursor.drawVital(graphics, client, "armor", "ARMOR",
                    armor + " / 20", RuffyConfig.secondaryColor(), armor / 20.0F);
        }

        if (RuffyConfig.get("keystrokes")) {
            drawKeystrokes(graphics, client, 12, logicalHeight - 82);
        }

        graphics.pose().popPose();
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
            this.rightLimit = Math.max(x + 72, rightLimit);
        }

        private void nextLineIfNeeded(int width) {
            if (x + width > rightLimit && x > startX) {
                x = startX;
                y += CHIP_HEIGHT + CHIP_GAP;
            }
        }

        private void drawBrand(GuiGraphics graphics, Minecraft client) {
            float opacity = RuffyAnimation.animate("hud.brand", 1.0F);
            int width = 78;
            nextLineIfNeeded(width);
            panel(graphics, x, y, width, CHIP_HEIGHT, RuffyConfig.primaryColor(), opacity);
            fillRounded(graphics, x + 9, y + 9, 8, 8,
                    RuffyAnimation.multiplyAlpha(RuffyConfig.primaryColor(), opacity), 4);
            graphics.drawString(client.font, "RUFFY", x + 24, y + 6,
                    RuffyAnimation.multiplyAlpha(WHITE, opacity), false);
            graphics.drawString(client.font, "VISUALS", x + 24, y + 17,
                    RuffyAnimation.multiplyAlpha(MUTED, opacity), false);
            x += width + CHIP_GAP;
        }

        private void drawMetric(GuiGraphics graphics, Minecraft client,
                                String id, String label, String value, int accent) {
            float opacity = RuffyAnimation.animate("hud.metric." + id, 1.0F);
            int available = Math.max(44, rightLimit - startX - 18);
            String shown = fit(client, value, available - 18);
            int width = Math.max(49,
                    Math.max(client.font.width(label), client.font.width(shown)) + 18);
            nextLineIfNeeded(width);
            panel(graphics, x, y, width, CHIP_HEIGHT, accent, opacity);
            graphics.drawString(client.font, label, x + 9, y + 5,
                    RuffyAnimation.multiplyAlpha(MUTED, opacity), false);
            graphics.drawString(client.font, shown, x + 9, y + 17,
                    RuffyAnimation.multiplyAlpha(WHITE, opacity), false);
            x += width + CHIP_GAP;
        }

        private void drawVital(GuiGraphics graphics, Minecraft client,
                               String id, String label, String value,
                               int accent, float ratio) {
            float opacity = RuffyAnimation.animate("hud.vital." + id, 1.0F);
            float progress = RuffyAnimation.animate("hud.vital.bar." + id, clamp01(ratio));
            int width = Math.max(86, client.font.width(value) + 22);
            nextLineIfNeeded(width);
            panel(graphics, x, y, width, CHIP_HEIGHT, accent, opacity);
            graphics.drawString(client.font, label, x + 9, y + 4,
                    RuffyAnimation.multiplyAlpha(MUTED, opacity), false);
            graphics.drawString(client.font, value, x + 9, y + 14,
                    RuffyAnimation.multiplyAlpha(WHITE, opacity), false);
            int left = x + 9;
            int barWidth = width - 18;
            fillRounded(graphics, left, y + 25, barWidth, 3,
                    RuffyAnimation.multiplyAlpha(0xFF3B343F, opacity), 2);
            int filled = Math.max(0, Math.min(barWidth, Math.round(barWidth * progress)));
            fillRounded(graphics, left, y + 25, filled, 3,
                    RuffyAnimation.multiplyAlpha(accent, opacity), 2);
            x += width + CHIP_GAP;
        }
    }

    private static String fit(Minecraft client, String value, int maxWidth) {
        if (client.font.width(value) <= maxWidth) return value;
        String clipped = value;
        while (clipped.length() > 2 && client.font.width(clipped + "…") > maxWidth) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        return clipped.length() > 2 ? clipped + "…" : clipped;
    }

    private static void panel(GuiGraphics graphics, int x, int y,
                              int width, int height, int accent, float opacity) {
        int fill = RuffyAnimation.multiplyAlpha(RuffyConfig.hudPanelColor(), opacity);
        int border = RuffyAnimation.multiplyAlpha(RuffyConfig.hudBorderColor(), opacity);
        fillRounded(graphics, x, y, width, height, fill, 7);
        outlineRounded(graphics, x, y, width, height, border, 7);
        // A short, quiet accent mark replaces the old full-height neon rail.
        fillRounded(graphics, x + 8, y + 1, Math.min(15, width - 16), 1,
                withAlpha(accent, Math.round(110 * opacity)), 1);
    }

    private static void drawKeystrokes(GuiGraphics graphics, Minecraft client, int x, int y) {
        int keyW = 22;
        int keyH = 18;
        int gap = 3;
        int panelW = keyW * 3 + gap * 2;
        int panelH = 71;
        float opacity = RuffyAnimation.animate("hud.input.panel", 1.0F);

        fillRounded(graphics, x - 3, y - 3, panelW + 6, panelH + 6,
                RuffyAnimation.multiplyAlpha(RuffyConfig.hudPanelColor(), opacity), 9);
        outlineRounded(graphics, x - 3, y - 3, panelW + 6, panelH + 6,
                RuffyAnimation.multiplyAlpha(RuffyConfig.hudBorderColor(), opacity), 9);
        graphics.drawString(client.font, "MOVEMENT", x + 2, y + 1,
                RuffyAnimation.multiplyAlpha(MUTED, opacity), false);

        int keysY = y + 13;
        drawKey(graphics, client, "key.w", "W", x + keyW + gap, keysY,
                keyW, keyH, client.options.keyUp.isDown());
        drawKey(graphics, client, "key.a", "A", x, keysY + keyH + gap,
                keyW, keyH, client.options.keyLeft.isDown());
        drawKey(graphics, client, "key.s", "S", x + keyW + gap, keysY + keyH + gap,
                keyW, keyH, client.options.keyDown.isDown());
        drawKey(graphics, client, "key.d", "D", x + (keyW + gap) * 2,
                keysY + keyH + gap, keyW, keyH, client.options.keyRight.isDown());

        int lowerY = keysY + (keyH + gap) * 2;
        drawKey(graphics, client, "key.jump", "JUMP", x,
                lowerY, keyW * 2 + gap, 14, client.options.keyJump.isDown());
        drawKey(graphics, client, "key.sprint", "SPR", x + (keyW + gap) * 2,
                lowerY, keyW, 14, client.options.keySprint.isDown());
    }

    private static void drawKey(GuiGraphics graphics, Minecraft client, String animationId,
                                String label, int x, int y, int width, int height, boolean pressed) {
        float active = RuffyAnimation.animate("hud.input." + animationId, pressed ? 1.0F : 0.0F);
        int accent = RuffyConfig.primaryColor();
        int idle = RuffyConfig.hudPanelColor();
        int activeFill = withAlpha(accent, 150);
        int fill = RuffyAnimation.blendColor(idle, activeFill, active);
        fillRounded(graphics, x, y, width, height, fill, 5);
        int border = RuffyAnimation.blendColor(RuffyConfig.hudBorderColor(),
                withAlpha(RuffyConfig.secondaryColor(), 220), active);
        outlineRounded(graphics, x, y, width, height, border, 5);
        graphics.drawCenteredString(client.font, label,
                x + width / 2, y + Math.max(2, (height - 8) / 2),
                RuffyAnimation.blendColor(MUTED, WHITE, active));
    }

    private static void fillRounded(GuiGraphics g, int x, int y, int w, int h,
                                    int color, int radius) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + r, y, x + w - r, y + h, color);
        for (int row = 0; row < r; row++) {
            double dy = r - row - 0.5;
            int inset = Math.max(0, r - (int) Math.floor(Math.sqrt(r * r - dy * dy)));
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

    private static void outlineRounded(GuiGraphics g, int x, int y, int w, int h,
                                       int color, int radius) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(1, Math.min(radius, Math.min(w, h) / 2));
        g.fill(x + r, y, x + w - r, y + 1, color);
        g.fill(x + r, y + h - 1, x + w - r, y + h, color);
        g.fill(x, y + r, x + 1, y + h - r, color);
        g.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int row = 0; row < r; row++) {
            double dy = r - row - 0.5;
            int inset = Math.max(0, r - (int) Math.floor(Math.sqrt(r * r - dy * dy)));
            g.fill(x + inset, y + row, x + inset + 1, y + row + 1, color);
            g.fill(x + w - inset - 1, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - row - 1, x + inset + 1, y + h - row, color);
            g.fill(x + w - inset - 1, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
