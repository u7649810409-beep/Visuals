package de.ruffy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Minimal transparent HUD chips with the selected client palette. */
public final class RuffyHud {
    private static final int WHITE = 0xFFF3F7FF;
    private static final int MUTED = 0xFFB6C5D6;

    private RuffyHud() {}

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.options.hideGui) return;

        int primary = RuffyConfig.primaryColor();
        int secondary = RuffyConfig.secondaryColor();
        int x = 12;
        int y = 12;
        int rowHeight = 23;

        if (RuffyConfig.get("watermark")) {
            String name = "RUFFY";
            String tag = "VISUALS";
            int width = client.font.width(name) + client.font.width(tag) + 33;
            panel(graphics, x, y, width, rowHeight, primary);
            graphics.drawString(client.font, name, x + 10, y + 7, WHITE, false);
            graphics.drawString(client.font, tag, x + 10 + client.font.width(name) + 5,
                    y + 7, primary, false);
            x += width + 5;
        }

        if (RuffyConfig.get("fps")) {
            String value = client.getFps() + " FPS";
            int width = client.font.width(value) + 18;
            panel(graphics, x, y, width, rowHeight, secondary);
            graphics.drawString(client.font, value, x + 9, y + 7, WHITE, false);
            x += width + 5;
        }

        if (RuffyConfig.get("coordinates")) {
            String value = player.blockPosition().getX() + "  "
                    + player.blockPosition().getY() + "  " + player.blockPosition().getZ();
            int width = client.font.width(value) + 18;
            panel(graphics, x, y, width, rowHeight, primary);
            graphics.drawString(client.font, value, x + 9, y + 7, MUTED, false);
            x += width + 5;
        }

        if (RuffyConfig.get("direction")) {
            String[] directions = {"S", "W", "N", "E"};
            int index = Math.floorMod(Math.round(player.getYRot() / 90.0F), 4);
            String value = "DIR  " + directions[index];
            int width = client.font.width(value) + 18;
            panel(graphics, x, y, width, rowHeight, secondary);
            graphics.drawString(client.font, value, x + 9, y + 7, WHITE, false);
            x += width + 5;
        }

        if (RuffyConfig.get("speed")) {
            double speed = Math.sqrt(player.getDeltaMovement().horizontalDistanceSqr()) * 20.0;
            String value = String.format(Locale.ROOT, "%.1f B/S", speed);
            int width = client.font.width(value) + 18;
            panel(graphics, x, y, width, rowHeight, primary);
            graphics.drawString(client.font, value, x + 9, y + 7, WHITE, false);
            x += width + 5;
        }

        if (RuffyConfig.get("world_clock") && client.level != null) {
            long time = Math.floorMod(client.level.getDayTime() + 6000L, 24000L);
            int hours = (int) (time / 1000L);
            int minutes = (int) ((time % 1000L) * 60L / 1000L);
            String value = String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
            int width = client.font.width(value) + 18;
            panel(graphics, x, y, width, rowHeight, secondary);
            graphics.drawString(client.font, value, x + 9, y + 7, WHITE, false);
        }

        if (RuffyConfig.get("keystrokes")) {
            int keyY = client.getWindow().getGuiScaledHeight() - 72;
            drawKeystrokes(graphics, client, 12, keyY);
        }
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, RuffyConfig.hudPanelColor());
        graphics.renderOutline(x, y, width, height, RuffyConfig.hudBorderColor());
        graphics.fill(x, y + 4, x + 2, y + height - 4, withAlpha(accent, 205));
        graphics.fill(x + 3, y, x + width - 3, y + 1, withAlpha(accent, 45));
    }

    private static void drawKeystrokes(GuiGraphics graphics, Minecraft client, int x, int y) {
        int keyW = 25;
        int keyH = 22;
        int gap = 3;
        drawKey(graphics, client, "W", x + keyW + gap, y, client.options.keyUp.isDown());
        drawKey(graphics, client, "A", x, y + keyH + gap, client.options.keyLeft.isDown());
        drawKey(graphics, client, "S", x + keyW + gap, y + keyH + gap, client.options.keyDown.isDown());
        drawKey(graphics, client, "D", x + (keyW + gap) * 2, y + keyH + gap,
                client.options.keyRight.isDown());
    }

    private static void drawKey(GuiGraphics graphics, Minecraft client, String label,
                                int x, int y, boolean pressed) {
        int panel = RuffyConfig.hudPanelColor();
        int fill = pressed ? withAlpha(RuffyConfig.primaryColor(), 175) : panel;
        graphics.fill(x, y, x + 25, y + 22, fill);
        graphics.renderOutline(x, y, 25, 22, RuffyConfig.hudBorderColor());
        if (pressed) graphics.fill(x + 3, y + 2, x + 22, y + 3,
                withAlpha(RuffyConfig.primaryColor(), 230));
        graphics.drawCenteredString(client.font, label, x + 12, y + 7,
                pressed ? WHITE : MUTED);
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }
}
