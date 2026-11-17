package de.ruffy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Palette-aware transparent HUD with automatic wrapping to small screens. */
public final class RuffyHud {
    private static final int WHITE = 0xFFF3F7FF;
    private static final int MUTED = 0xFFB6C5D6;
    private static final int CHIP_HEIGHT = 22;
    private static final int CHIP_GAP = 5;

    private RuffyHud() {}

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.options.hideGui) return;

        int screenWidth = client.getWindow().getGuiScaledWidth();
        HudCursor cursor = new HudCursor(12, 12, screenWidth - 12);

        if (RuffyConfig.get("watermark")) {
            cursor.draw(graphics, client, "RUFFY  VISUALS", RuffyConfig.primaryColor());
        }

        if (RuffyConfig.get("fps")) {
            cursor.draw(graphics, client, client.getFps() + " FPS",
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

        if (RuffyConfig.get("keystrokes")) {
            int keyY = client.getWindow().getGuiScaledHeight() - 76;
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

        private void draw(GuiGraphics graphics, Minecraft client, String text, int accent) {
            int maxTextWidth = Math.max(42, rightLimit - startX - 18);
            String visible = text;
            while (visible.length() > 3 && client.font.width(visible) > maxTextWidth) {
                visible = visible.substring(0, visible.length() - 1);
            }
            int width = client.font.width(visible) + 18;
            if (x + width > rightLimit && x > startX) {
                x = startX;
                y += CHIP_HEIGHT + CHIP_GAP;
            }
            panel(graphics, x, y, width, CHIP_HEIGHT, accent);
            graphics.drawString(client.font, visible, x + 9, y + 7, WHITE, false);
            x += width + CHIP_GAP;
        }
    }

    private static void panel(GuiGraphics graphics, int x, int y,
                              int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, RuffyConfig.hudPanelColor());
        graphics.renderOutline(x, y, width, height, RuffyConfig.hudBorderColor());
        graphics.fill(x, y + 4, x + 2, y + height - 4, withAlpha(accent, 205));
        graphics.fill(x + 3, y, x + width - 3, y + 1, withAlpha(accent, 45));
    }

    private static void drawKeystrokes(GuiGraphics graphics, Minecraft client, int x, int y) {
        int keyW = 24;
        int keyH = 20;
        int gap = 3;

        drawKey(graphics, client, "W", x + keyW + gap, y,
                keyW, keyH, client.options.keyUp.isDown());
        drawKey(graphics, client, "A", x, y + keyH + gap,
                keyW, keyH, client.options.keyLeft.isDown());
        drawKey(graphics, client, "S", x + keyW + gap, y + keyH + gap,
                keyW, keyH, client.options.keyDown.isDown());
        drawKey(graphics, client, "D", x + (keyW + gap) * 2, y + keyH + gap,
                keyW, keyH, client.options.keyRight.isDown());

        drawKey(graphics, client, "JUMP", x, y + (keyH + gap) * 2,
                keyW * 2 + gap, 16, client.options.keyJump.isDown());
        drawKey(graphics, client, "SPR", x + (keyW + gap) * 2, y + (keyH + gap) * 2,
                keyW, 16, client.options.keySprint.isDown());
    }

    private static void drawKey(GuiGraphics graphics, Minecraft client, String label,
                                int x, int y, int width, int height, boolean pressed) {
        int fill = pressed
                ? withAlpha(RuffyConfig.primaryColor(), 175)
                : RuffyConfig.hudPanelColor();
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.renderOutline(x, y, width, height, RuffyConfig.hudBorderColor());
        if (pressed) {
            graphics.fill(x + 3, y + 2, x + width - 3, y + 3,
                    withAlpha(RuffyConfig.primaryColor(), 230));
        }
        graphics.drawCenteredString(client.font, label,
                x + width / 2, y + Math.max(3, (height - 8) / 2),
                pressed ? WHITE : MUTED);
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
