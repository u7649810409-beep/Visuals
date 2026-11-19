package de.ruffy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact Galaxy-themed Spotify and lyrics panel. */
public final class RuffySpotifyScreen extends Screen {
    private static final int WHITE = 0xFFF7F2FF;
    private static final int MUTED = 0xFFBFB3D2;
    private static final int DIM = 0xFF8F82A9;
    private static final int CARD = 0xB30B0B1B;
    private static final int BORDER = 0x667A62A8;

    private final Screen parent;
    private EditBox clientIdBox;
    private int panelX, panelY, panelW, panelH;
    private int leftX, leftW, rightX, rightW;
    private int clientIdY, actionY, controlY;

    public RuffySpotifyScreen(Screen parent) {
        super(Component.literal("Ruffy Visuals — Spotify"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        clientIdBox = new EditBox(font, leftX + 10, clientIdY + 3,
                Math.max(80, leftW - 20), 18, Component.literal("Spotify Client ID"));
        clientIdBox.setBordered(false);
        clientIdBox.setMaxLength(96);
        clientIdBox.setTextColor(0xFFEAF2FA);
        clientIdBox.setHint(Component.literal("Paste your Spotify Client ID"));
        clientIdBox.setValue(RuffySpotify.getClientId());
        clientIdBox.setResponder(RuffySpotify::setClientId);
        addRenderableWidget(clientIdBox);
    }

    private void calculateLayout() {
        panelW = Math.min(600, Math.max(440, width - 24));
        panelW = Math.min(panelW, width - 12);
        panelH = Math.min(392, Math.max(320, height - 24));
        panelH = Math.min(panelH, height - 12);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        leftX = panelX + 18;
        leftW = (panelW - 50) * 45 / 100;
        rightX = leftX + leftW + 14;
        rightW = panelX + panelW - 18 - rightX;
        clientIdY = panelY + 92;
        actionY = panelY + 126;
        controlY = panelY + panelH - 95;
        if (clientIdBox != null) clientIdBox.setPosition(leftX + 10, clientIdY + 3);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        calculateLayout();
        RuffyGalaxyUi.draw(g, width, height);
        g.fill(0, 0, width, height, 0x69040712);

        int primary = RuffyConfig.primaryColor();
        int secondary = RuffyConfig.secondaryColor();
        int outer = withAlpha(secondary, 18);
        int body = 0xF10A0D19;

        RuffyRoundedUi.shadow(g, panelX - 2, panelY, panelW + 4, panelH + 4,
                RuffyRoundedUi.RADIUS_WINDOW, 48);
        RuffyRoundedUi.fill(g, panelX - 5, panelY - 4,
                panelW + 10, panelH + 8, outer, RuffyRoundedUi.RADIUS_WINDOW);
        RuffyRoundedUi.fill(g, panelX, panelY, panelW, panelH, body,
                RuffyRoundedUi.RADIUS_WINDOW);
        RuffyRoundedUi.outline(g, panelX, panelY, panelW, panelH,
                withAlpha(primary, 150), RuffyRoundedUi.RADIUS_WINDOW);
        RuffyRoundedUi.fill(g, panelX + 28, panelY + 1, panelW - 56, 1,
                withAlpha(0xFFFFFFFF, 24), 1);

        renderMouseX = mouseX;
        renderMouseY = mouseY;
        drawHeader(g, primary, secondary);
        drawConnectionCard(g, mouseX, mouseY, primary);
        drawLyricsCard(g, primary);
        drawFooter(g);
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawHeader(GuiGraphics g, int primary, int secondary) {
        RuffyRoundedUi.fill(g, panelX, panelY, panelW, 48,
                0xEF111426, RuffyRoundedUi.RADIUS_WINDOW);
        g.fill(panelX, panelY + 23, panelX + panelW, panelY + 48, 0xE9110D21);
        RuffyRoundedUi.fill(g, panelX + 13, panelY + 9, 30, 30,
                withAlpha(primary, 195), RuffyRoundedUi.RADIUS_ROW);
        g.drawCenteredString(font, "♪", panelX + 28, panelY + 18, WHITE);
        g.drawString(font, "RUFFY VISUALS  /  COSMIC PLAYER", panelX + 53,
                panelY + 10, WHITE, false);
        g.drawString(font, "COSMIC PLAYER  ·  LYRICS  ·  2.7", panelX + 53,
                panelY + 25, secondary, false);

        int backX = panelX + panelW - 92;
        int backY = panelY + 10;
        boolean hover = inside(mouseXValue(), mouseYValue(), backX, backY, 78, 26);
        drawButton(g, backX, backY, 78, 26, "BACK  ×",
                hover, withAlpha(primary, 68));
    }

    // Mouse coordinates are set during render and consumed by the header helper.
    private int renderMouseX;
    private int renderMouseY;

    private int mouseXValue() { return renderMouseX; }
    private int mouseYValue() { return renderMouseY; }

    private void drawConnectionCard(GuiGraphics g, int mouseX, int mouseY, int primary) {
        renderMouseX = mouseX;
        renderMouseY = mouseY;
        int y = panelY + 60;
        int h = panelH - 94;
        RuffyRoundedUi.fill(g, leftX, y, leftW, h, CARD, RuffyRoundedUi.RADIUS_PANEL);
        RuffyRoundedUi.outline(g, leftX, y, leftW, h, BORDER, RuffyRoundedUi.RADIUS_PANEL);

        g.drawString(font, "SPOTIFY ACCOUNT", leftX + 12, y + 11, WHITE, false);
        g.drawString(font, "CLIENT ID", leftX + 12, y + 29, MUTED, false);
        RuffyRoundedUi.fill(g, leftX + 7, clientIdY, leftW - 14, 24,
                0xD20C1020, RuffyRoundedUi.RADIUS_CONTROL);
        RuffyRoundedUi.outline(g, leftX + 7, clientIdY, leftW - 14, 24,
                withAlpha(primary, 95), RuffyRoundedUi.RADIUS_CONTROL);

        boolean connectHover = inside(mouseX, mouseY, leftX + 9, actionY,
                Math.max(65, (leftW - 25) / 2), 25);
        boolean disconnectHover = inside(mouseX, mouseY, leftX + 16 + Math.max(65, (leftW - 25) / 2),
                actionY, Math.max(65, (leftW - 25) / 2), 25);
        int half = Math.max(65, (leftW - 25) / 2);
        drawButton(g, leftX + 9, actionY, half, 25,
                RuffySpotify.isConnected() ? "CONNECTED" : "CONNECT",
                connectHover, withAlpha(primary, 78));
        drawButton(g, leftX + 16 + half, actionY, half, 25,
                "DISCONNECT", disconnectHover, 0x783A243B);

        String status = RuffySpotify.getStatus();
        g.drawString(font, fit(status, leftW - 24), leftX + 12, actionY + 34,
                RuffySpotify.isConnected() ? primary : MUTED, false);
        g.drawString(font, "NOW PLAYING", leftX + 12, actionY + 54, DIM, false);
        g.drawString(font, fit(RuffySpotify.getTrackName(), leftW - 24),
                leftX + 12, actionY + 69, WHITE, false);
        g.drawString(font, fit(RuffySpotify.getArtistName(), leftW - 24),
                leftX + 12, actionY + 83, MUTED, false);
        g.drawString(font, fit(RuffySpotify.getAlbumName(), leftW - 24),
                leftX + 12, actionY + 97, DIM, false);

        long duration = RuffySpotify.getDurationMs();
        long position = RuffySpotify.getProgressMs();
        int barY = actionY + 120;
        int barX = leftX + 12;
        int barW = Math.max(10, leftW - 24);
        float progress = duration <= 0L ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, position / (float) duration));
        RuffyRoundedUi.fill(g, barX, barY, barW, 4, 0xFF332F40, 2);
        RuffyRoundedUi.fill(g, barX, barY, Math.round(barW * progress), 4, primary, 2);
        g.drawString(font, formatTime(position), barX, barY + 8, DIM, false);
        String total = formatTime(duration);
        g.drawString(font, total, barX + barW - font.width(total), barY + 8, DIM, false);

        int buttonY = controlY;
        int buttonW = Math.max(34, Math.min(46, (leftW - 42) / 3));
        int gap = 7;
        int totalControls = buttonW * 3 + gap * 2;
        int start = leftX + (leftW - totalControls) / 2;
        drawButton(g, start, buttonY, buttonW, 25, "◀", inside(mouseX, mouseY, start, buttonY, buttonW, 25),
                0x7D221B35);
        drawButton(g, start + buttonW + gap, buttonY, buttonW, 25,
                RuffySpotify.isPlaying() ? "Ⅱ" : "▶",
                inside(mouseX, mouseY, start + buttonW + gap, buttonY, buttonW, 25),
                withAlpha(primary, 90));
        drawButton(g, start + (buttonW + gap) * 2, buttonY, buttonW, 25, "▶▶",
                inside(mouseX, mouseY, start + (buttonW + gap) * 2, buttonY, buttonW, 25),
                0x7D221B35);

        int copyY = y + h - 31;
        drawButton(g, leftX + 9, copyY, leftW - 18, 20, "COPY REDIRECT URI",
                inside(mouseX, mouseY, leftX + 9, copyY, leftW - 18, 20),
                0x6B2D2147);
    }

    private void drawLyricsCard(GuiGraphics g, int primary) {
        int y = panelY + 60;
        int h = panelH - 94;
        RuffyRoundedUi.fill(g, rightX, y, rightW, h, CARD, RuffyRoundedUi.RADIUS_PANEL);
        RuffyRoundedUi.outline(g, rightX, y, rightW, h, BORDER, RuffyRoundedUi.RADIUS_PANEL);
        RuffyRoundedUi.fill(g, rightX + 3, y + 3, rightW - 6, 2,
                withAlpha(primary, 125), 1);

        g.drawString(font, "LYRICS", rightX + 12, y + 12, WHITE, false);
        g.drawString(font, "LRCLIB  ·  STATIC TEXT", rightX + 12, y + 27, primary, false);

        String source = RuffySpotify.getLyrics();
        int textX = rightX + 12;
        int textY = y + 49;
        int maxWidth = Math.max(50, rightW - 24);
        int maxLines = Math.max(1, (h - 65) / 11);
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String sourceLine : source.split("\\R")) {
            List<FormattedCharSequence> wrapped = font.split(Component.literal(sourceLine), maxWidth);
            if (!wrapped.isEmpty()) lines.addAll(wrapped);
        }
        if (lines.isEmpty()) {
            lines.addAll(font.split(Component.literal("Lyrics will appear here when a match is found."),
                    maxWidth));
        }
        int shown = Math.min(maxLines, lines.size());
        for (int i = 0; i < shown; i++) {
            g.drawString(font, lines.get(i), textX, textY + i * 11,
                    i == 0 ? WHITE : MUTED, false);
        }
        if (lines.size() > shown) {
            String more = "… " + (lines.size() - shown) + " more lines";
            g.drawString(font, more, textX, y + h - 17, DIM, false);
        }
    }

    private void drawFooter(GuiGraphics g) {
        g.drawString(font, "REDIRECT: " + RuffySpotify.REDIRECT_URI,
                panelX + 18, panelY + panelH - 19, DIM, false);
        g.drawString(font, "RIGHT SHIFT / ESC  ·  BACK",
                panelX + panelW - 150, panelY + panelH - 19, DIM, false);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h,
                            String label, boolean hover, int tint) {
        int background = hover ? RuffyAnimation.blendColor(0xC8191428,
                withAlpha(RuffyConfig.primaryColor(), 105), 0.7F) : tint;
        int radius = Math.min(RuffyRoundedUi.RADIUS_CONTROL, h / 2);
        RuffyRoundedUi.fill(g, x, y, w, h, background, radius);
        RuffyRoundedUi.outline(g, x, y, w, h,
                withAlpha(RuffyConfig.primaryColor(), hover ? 175 : 78), radius);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2,
                hover ? WHITE : MUTED);
    }

    private String fit(String value, int maxWidth) {
        if (value == null || value.isBlank()) return "—";
        String result = value;
        while (result.length() > 2 && font.width(result) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result.length() < value.length() ? result + "…" : result;
    }

    private static String formatTime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        calculateLayout();
        int mx = (int) event.x();
        int my = (int) event.y();

        int backX = panelX + panelW - 92;
        if (inside(mx, my, backX, panelY + 10, 78, 26)) {
            returnToParent();
            return true;
        }

        int half = Math.max(65, (leftW - 25) / 2);
        if (inside(mx, my, leftX + 9, actionY, half, 25)) {
            RuffySpotify.connect();
            return true;
        }
        if (inside(mx, my, leftX + 16 + half, actionY, half, 25)) {
            RuffySpotify.disconnect();
            return true;
        }

        int y = panelY + 60;
        int h = panelH - 94;
        int copyY = y + h - 31;
        if (inside(mx, my, leftX + 9, copyY, leftW - 18, 20)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(RuffySpotify.REDIRECT_URI);
            return true;
        }

        int buttonW = Math.max(34, Math.min(46, (leftW - 42) / 3));
        int gap = 7;
        int start = leftX + (leftW - (buttonW * 3 + gap * 2)) / 2;
        if (inside(mx, my, start, controlY, buttonW, 25)) {
            RuffySpotify.previousTrack();
            return true;
        }
        if (inside(mx, my, start + buttonW + gap, controlY, buttonW, 25)) {
            RuffySpotify.togglePlayback();
            return true;
        }
        if (inside(mx, my, start + (buttonW + gap) * 2, controlY, buttonW, 25)) {
            RuffySpotify.nextTrack();
            return true;
        }
        return true;
    }

    private void returnToParent() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public void onClose() {
        returnToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24);
    }
}
