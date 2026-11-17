package de.ruffy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/** Lightweight HUD overlays rendered through the 1.21.11 Fabric HudElement API. */
public final class RuffyHud {
    private static final int PANEL = 0xC9101022;
    private static final int PANEL_EDGE = 0xFF9D5CFF;
    private static final int TEXT = 0xFFF5F2FF;

    private RuffyHud() {}

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.options.hideGui) return;

        int left = 10;
        int top = 10;

        if (RuffyConfig.get("watermark")) {
            panel(graphics, left, top, 142, 24);
            graphics.drawString(client.font, "RUFFY", left + 9, top + 8, TEXT, false);
            graphics.drawString(client.font, "VISUALS", left + 56, top + 8, 0xFFB78BFF, false);
            top += 30;
        }

        if (RuffyConfig.get("fps")) {
            String fps = "FPS  " + client.getFps();
            panel(graphics, left, top, client.font.width(fps) + 18, 22);
            graphics.drawString(client.font, fps, left + 9, top + 7, TEXT, false);
            top += 28;
        }

        if (RuffyConfig.get("coordinates")) {
            String coords = "XYZ  " + player.blockPosition().getX() + " / "
                    + player.blockPosition().getY() + " / " + player.blockPosition().getZ();
            panel(graphics, left, top, client.font.width(coords) + 18, 22);
            graphics.drawString(client.font, coords, left + 9, top + 7, TEXT, false);
        }

        if (RuffyConfig.get("keystrokes")) {
            int y = client.getWindow().getGuiScaledHeight() - 76;
            drawKeystrokes(graphics, client, 10, y);
        }
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.fill(x, y, x + 2, y + height, PANEL_EDGE);
        graphics.fill(x + 2, y, x + width, y + 1, 0x559B70FF);
    }

    private static void drawKeystrokes(GuiGraphics graphics, Minecraft client, int x, int y) {
        int keyW = 23;
        int keyH = 20;
        int gap = 3;
        drawKey(graphics, client, "W", x + keyW + gap, y, client.options.keyUp.isDown());
        drawKey(graphics, client, "A", x, y + keyH + gap, client.options.keyLeft.isDown());
        drawKey(graphics, client, "S", x + keyW + gap, y + keyH + gap, client.options.keyDown.isDown());
        drawKey(graphics, client, "D", x + (keyW + gap) * 2, y + keyH + gap, client.options.keyRight.isDown());
    }

    private static void drawKey(GuiGraphics graphics, Minecraft client, String label, int x, int y, boolean pressed) {
        int fill = pressed ? 0xFF7441C7 : PANEL;
        graphics.fill(x, y, x + 23, y + 20, fill);
        graphics.renderOutline(x, y, 23, 20, pressed ? 0xFFE2C8FF : 0x886A5595);
        graphics.drawCenteredString(client.font, label, x + 11, y + 6, TEXT);
    }
}
