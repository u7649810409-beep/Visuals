package de.ruffy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/** Clean, compact sci-fi HUD rendered through Fabric's HudElement API. */
public final class RuffyHud {
    private static final int PANEL = 0xC90B1524;
    private static final int BORDER = 0x664C7187;
    private static final int CYAN = 0xFF52D9F5;
    private static final int VIOLET = 0xFF9B7BFF;
    private static final int WHITE = 0xFFF3F7FF;
    private static final int MUTED = 0xFFA4B5C8;

    private RuffyHud() {}

    public static void render(GuiGraphics graphics) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || client.options.hideGui) return;

        int x = 10;
        int y = 10;

        if (RuffyConfig.get("watermark")) {
            int width = 112;
            panel(graphics, x, y, width, 25, CYAN);
            graphics.fill(x + 8, y + 7, x + 10, y + 18, CYAN);
            graphics.drawString(client.font, "RUFFY", x + 17, y + 5, WHITE, false);
            graphics.drawString(client.font, "VISUALS", x + 59, y + 5, CYAN, false);
            x += width + 7;
        }

        if (RuffyConfig.get("fps")) {
            String value = client.getFps() + " FPS";
            int width = client.font.width(value) + 18;
            panel(graphics, x, y, width, 25, VIOLET);
            graphics.drawString(client.font, value, x + 9, y + 8, WHITE, false);
            x += width + 7;
        }

        if (RuffyConfig.get("coordinates")) {
            String value = player.blockPosition().getX() + "  /  "
                    + player.blockPosition().getY() + "  /  " + player.blockPosition().getZ();
            int width = client.font.width(value) + 18;
            panel(graphics, x, y, width, 25, CYAN);
            graphics.drawString(client.font, value, x + 9, y + 8, MUTED, false);
        }

        if (RuffyConfig.get("keystrokes")) {
            int keyY = client.getWindow().getGuiScaledHeight() - 63;
            drawKeystrokes(graphics, client, 10, keyY);
        }
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.renderOutline(x, y, width, height, BORDER);
        graphics.fill(x, y, x + 2, y + height, accent);
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
        int fill = pressed ? 0xFF164456 : PANEL;
        int accent = pressed ? CYAN : 0xFF526B83;
        graphics.fill(x, y, x + 25, y + 22, fill);
        graphics.renderOutline(x, y, 25, 22, BORDER);
        if (pressed) graphics.fill(x + 2, y + 2, x + 23, y + 3, accent);
        graphics.drawCenteredString(client.font, label, x + 12, y + 7,
                pressed ? WHITE : MUTED);
    }
}
