package de.ruffy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * Matching violet control-center HUD: transparent charcoal chips,
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
            cursor.draw(graphics, client, "FPS  " + client.getFps(),
                    RuffyConfig.secondaryColor());
        }

        if (RuffyConfig.get("coordinates")) {
            String coords = "XYZ  " + player.blockPosition().getX() + "  "
                    + player.blockPosition().getY() + "  " + player.blockPosition().getZ();
            cursor.draw(graphics, client, coords, RuffyConfig.primaryColor());
        }

        if (RuffyConfig.get("direction")) {
            String[] directions = {"S", "W", "N", "E"};
            int index = Math.floorMod(Math.round(player.getYRot() / 90.0F), 4);
            cursor.draw(graphics, client, "DIR  " + directions[index],
                    RuffyConfig.secondaryColor());
        }

        if (RuffyConfig.get("speed")) {
            double speed = Math.sqrt(player.getDeltaMovement().horizontalDistanceSqr()) * 20.0;
            cursor.draw(graphics, client, String.format(Locale.ROOT, "SPD  %.1f B/S", speed),
                    RuffyConfig.primaryColor());
        }

        if (RuffyConfig.get("world_clock") && client.level != null) {
            long time = Math.floorMod(client.level.getDayTime() + 6000L, 24000L);
            int hours = (int) (time / 1000L);
            int minutes = (int) ((time % 1000L) * 60L / 1000L);
            cursor.draw(graphics, client,
                    String.format(Locale.ROOT, "WORLD  %02d:%02d", hours, minutes),
                    RuffyConfig.secondaryColor());
        }

        if (RuffyConfig.get("health_hud")) {
            float health = player.getHealth();
            float maxHealth = Math.max(1.0F, player.getMaxHealth());
            int healthColor = health / maxHealth <= 0.25F ? 0xFFFF647D
                    : health / maxHealth <= 0.5F ? 0xFFFFB56B : RuffyConfig.primaryColor();
            cursor.drawBar(graphics, client,
                    String.format(Locale.ROOT, "HP  %.1f / %.0f", health, maxHealth),
                    healthColor, health / maxHealth);
        }

        if (RuffyConfig.get("armor_hud")) {
            cursor.draw(graphics, client, "ARMOR  " + player.getArmorValue(),
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
            int width = 104;
            nextLineIfNeeded(width);
            panel(graphics, x, y, width, CHIP_HEIGHT, RuffyConfig.primaryColor());
            graphics.fill(x + 6, y + 4, x + 21, y + 19,
                    withAlpha(RuffyConfig.primaryColor(), 55));
            graphics.renderOutline(x + 6, y + 4, 15, 15,
                    withAlpha(RuffyConfig.primaryColor(), 180));
            graphics.drawCenteredString(client.font, "R", x + 13, y + 7,
                    RuffyConfig.primaryColor());
            graphics.drawString(client.font, "RUFFY", x + 27, y + 4, WHITE, false);
            graphics.drawString(client.font, "VISUALS", x + 27, y + 14,
                    RuffyConfig.primaryColor(), false);
            x += width + CHIP_GAP;
        }

        private void draw(GuiGraphics graphics, Minecraft client, String text, int accent) {
            int maxTextWidth = Math.max(42, rightLimit - startX - 18);
            String visible = text;
            while (visible.length() > 3 && client.font.width(visible) > maxTextWidth) {
                visible = visible.substring(0, visible.length() - 1);
            }
            int width = client.font.width(visible) + 18;
            nextLineIfNeeded(width);
            panel(graphics, x, y, width, CHIP_HEIGHT, accent);
            graphics.drawString(client.font, visible, x + 9, y + 7, WHITE, false);
            x += width + CHIP_GAP;
        }

        private void drawBar(GuiGraphics graphics, Minecraft client, String text,
                             int accent, float progress) {
            int width = client.font.width(text) + 20;
            nextLineIfNeeded(width);
            panel(graphics, x, y, width, CHIP_HEIGHT, accent);
            graphics.drawString(client.font, text, x + 9, y + 4, WHITE, false);
            int barLeft = x + 9;
            int barRight = x + width - 9;
            graphics.fill(barLeft, y + 17, barRight, y + 19, 0x80534A5C);
            int fill = Math.max(0, Math.min(barRight - barLeft,
                    Math.round((barRight - barLeft) * progress)));
            graphics.fill(barLeft, y + 17, barLeft + fill, y + 19,
                    withAlpha(accent, 235));
            x += width + CHIP_GAP;
        }
    }

    private static void panel(GuiGraphics graphics, int x, int y,
                              int width, int height, int accent) {
        graphics.fill(x - 2, y - 1, x + width + 2, y + height + 1,
                withAlpha(accent, 15));
        graphics.fill(x, y, x + width, y + height,
                RuffyConfig.hudPanelColor());
        graphics.renderOutline(x, y, width, height,
                RuffyConfig.hudBorderColor());
        graphics.fill(x, y + 4, x + 2, y + height - 4,
                withAlpha(accent, 220));
        graphics.fill(x + 3, y, x + width - 3, y + 1,
                withAlpha(accent, 75));
    }

    private static void drawKeystrokes(GuiGraphics graphics, Minecraft client, int x, int y) {
        int keyW = 24;
        int keyH = 20;
        int gap = 3;
        int panelW = keyW * 3 + gap * 2;
        int panelH = keyH * 3 + gap * 2 + 18;

        graphics.fill(x - 5, y - 5, x + panelW + 5, y + panelH + 4,
                withAlpha(RuffyConfig.primaryColor(), 15));
        graphics.fill(x - 3, y - 3, x + panelW + 3, y + panelH + 2,
                RuffyConfig.hudPanelColor());
        graphics.renderOutline(x - 3, y - 3, panelW + 6, panelH + 5,
                RuffyConfig.hudBorderColor());
        graphics.drawString(client.font, "INPUT", x + 2, y - 1,
                RuffyConfig.primaryColor(), false);

        int keysY = y + 12;
        drawKey(graphics, client, "W", x + keyW + gap, keysY,
                keyW, keyH, client.options.keyUp.isDown());
        drawKey(graphics, client, "A", x, keysY + keyH + gap,
                keyW, keyH, client.options.keyLeft.isDown());
        drawKey(graphics, client, "S", x + keyW + gap, keysY + keyH + gap,
                keyW, keyH, client.options.keyDown.isDown());
        drawKey(graphics, client, "D", x + (keyW + gap) * 2, keysY + keyH + gap,
                keyW, keyH, client.options.keyRight.isDown());

        drawKey(graphics, client, "JUMP", x, keysY + (keyH + gap) * 2,
                keyW * 2 + gap, 16, client.options.keyJump.isDown());
        drawKey(graphics, client, "SPR", x + (keyW + gap) * 2,
                keysY + (keyH + gap) * 2, keyW, 16,
                client.options.keySprint.isDown());
    }

    private static void drawKey(GuiGraphics graphics, Minecraft client, String label,
                                int x, int y, int width, int height, boolean pressed) {
        int accent = RuffyConfig.primaryColor();
        if (pressed) {
            graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2,
                    withAlpha(accent, 40));
        }
        int fill = pressed ? withAlpha(accent, 175) : RuffyConfig.hudPanelColor();
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.renderOutline(x, y, width, height,
                pressed ? withAlpha(RuffyConfig.secondaryColor(), 230)
                        : RuffyConfig.hudBorderColor());
        if (pressed) {
            graphics.fill(x + 3, y + 2, x + width - 3, y + 3,
                    withAlpha(accent, 230));
        }
        graphics.drawCenteredString(client.font, label,
                x + width / 2, y + Math.max(3, (height - 8) / 2),
                pressed ? WHITE : MUTED);
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
