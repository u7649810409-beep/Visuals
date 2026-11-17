package de.ruffy.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Minimal, futuristic control center for Ruffy Visuals. */
public final class RuffyClickGui extends Screen {
    private static final int OVERLAY = 0x99040A16;
    private static final int PANEL = 0xF20B1221;
    private static final int PANEL_ALT = 0xF20E192B;
    private static final int CARD = 0xE8111D30;
    private static final int CARD_HOVER = 0xF0192A40;
    private static final int WHITE = 0xFFF3F7FF;
    private static final int MUTED = 0xFF8FA3B9;
    private static final int DIM = 0xFF53677F;
    private static final int CYAN = 0xFF52D9F5;
    private static final int VIOLET = 0xFF9B7BFF;
    private static final int GREEN = 0xFF5DE0B1;
    private static final int BORDER = 0x553E5875;

    private RuffyFeature.Category category = RuffyFeature.Category.HUD;
    private int panelX, panelY, panelW, panelH, sidebarW, contentX, contentW;

    public RuffyClickGui() {
        super(Component.literal("Ruffy Visuals Control Center"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        calculateLayout();

        g.fill(0, 0, width, height, OVERLAY);
        drawAmbientGrid(g);

        g.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, 0x553A8DA6);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        g.renderOutline(panelX, panelY, panelW, panelH, BORDER);

        drawHeader(g);
        drawSidebar(g, mouseX, mouseY);
        drawContent(g, mouseX, mouseY);
        drawFooter(g);
    }

    private void calculateLayout() {
        panelW = Math.min(850, Math.max(340, width - 36));
        panelH = Math.min(530, Math.max(310, height - 32));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sidebarW = Math.min(168, Math.max(124, panelW / 4));
        contentX = panelX + sidebarW + 26;
        contentW = panelW - sidebarW - 48;
    }

    private void drawAmbientGrid(GuiGraphics g) {
        for (int x = 0; x < width; x += 48) {
            g.fill(x, 0, x + 1, height, 0x0C78B9D0);
        }
        for (int y = 0; y < height; y += 48) {
            g.fill(0, y, width, y + 1, 0x0A78B9D0);
        }
        for (int i = 0; i < 12; i++) {
            int x = Math.floorMod(i * 137 + 29, Math.max(1, width));
            int y = Math.floorMod(i * 83 + 41, Math.max(1, height));
            g.fill(x, y, x + 2, y + 2, i % 3 == 0 ? 0x8877E8FF : 0x557E84FF);
        }
    }

    private void drawHeader(GuiGraphics g) {
        g.fill(panelX, panelY, panelX + panelW, panelY + 58, PANEL_ALT);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, CYAN);

        int logoX = panelX + 18;
        int logoY = panelY + 15;
        g.fill(logoX, logoY, logoX + 28, logoY + 28, 0xFF14283A);
        g.renderOutline(logoX, logoY, 28, 28, 0xFF43849B);
        g.drawCenteredString(font, "R", logoX + 14, logoY + 9, CYAN);

        g.drawString(font, "RUFFY", panelX + 56, panelY + 15, WHITE, false);
        g.drawString(font, "VISUALS", panelX + 56, panelY + 29, CYAN, false);
        g.fill(panelX + 132, panelY + 17, panelX + 133, panelY + 41, BORDER);

        int statusX = panelX + panelW - 155;
        g.fill(statusX, panelY + 22, statusX + 6, panelY + 28, GREEN);
        g.drawString(font, "SYSTEM ONLINE", statusX + 13, panelY + 20, 0xFFB8CFD9, false);
        g.drawString(font, "RIGHT SHIFT  /  ESC", panelX + panelW - 155, panelY + 36, DIM, false);
        g.fill(panelX, panelY + 58, panelX + panelW, panelY + 59, BORDER);
    }

    private void drawSidebar(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(panelX, panelY + 59, panelX + sidebarW, panelY + panelH - 1, 0x99101B2B);
        g.fill(panelX + sidebarW, panelY + 59, panelX + sidebarW + 1, panelY + panelH - 1, BORDER);

        g.drawString(font, "WORKSPACE", panelX + 18, panelY + 79, DIM, false);
        int y = panelY + 98;
        int index = 1;
        for (RuffyFeature.Category item : RuffyFeature.Category.values()) {
            int x = panelX + 12;
            int w = sidebarW - 24;
            int h = 38;
            boolean selected = item == category;
            boolean hovered = inside(mouseX, mouseY, x, y, w, h);

            if (selected) {
                g.fill(x, y, x + w, y + h, 0xDD183149);
                g.fill(x, y + 5, x + 2, y + h - 5, CYAN);
                g.renderOutline(x, y, w, h, 0x5545A8C0);
            } else if (hovered) {
                g.fill(x, y, x + w, y + h, 0x661D2B3E);
            }

            g.drawString(font, String.format("%02d", index), x + 9, y + 13,
                    selected ? CYAN : DIM, false);
            g.drawString(font, item.title(), x + 35, y + 13,
                    selected ? WHITE : MUTED, false);
            y += 46;
            index++;
        }

        int bottom = panelY + panelH - 50;
        g.fill(panelX + 16, bottom - 10, panelX + sidebarW - 16, bottom - 9, BORDER);
        g.drawString(font, "BUILD  1.21.11", panelX + 18, bottom, MUTED, false);
        g.drawString(font, "FABRIC  /  CLIENT", panelX + 18, bottom + 15, DIM, false);
    }

    private void drawContent(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, category.title().toUpperCase(), contentX, panelY + 80, WHITE, false);
        g.drawString(font, category.subtitle(), contentX, panelY + 95, MUTED, false);

        if (category == RuffyFeature.Category.ABOUT) {
            drawAbout(g);
            return;
        }

        List<RuffyFeature> features = RuffyFeature.ALL.stream()
                .filter(feature -> feature.category() == category).toList();
        int columns = contentW >= 420 ? 2 : 1;
        int gap = 12;
        int cardW = (contentW - gap * (columns - 1)) / columns;
        int cardH = 76;
        int startY = panelY + 125;

        for (int i = 0; i < features.size(); i++) {
            RuffyFeature feature = features.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = contentX + col * (cardW + gap);
            int y = startY + row * (cardH + gap);
            boolean hovered = inside(mouseX, mouseY, x, y, cardW, cardH);
            boolean enabled = RuffyConfig.get(feature.key());

            g.fill(x, y, x + cardW, y + cardH, hovered ? CARD_HOVER : CARD);
            g.renderOutline(x, y, cardW, cardH, hovered ? 0xAA4FAFC6 : BORDER);
            g.fill(x, y, x + cardW, y + 1, enabled ? CYAN : 0xFF26364A);
            g.fill(x + 12, y + 14, x + 14, y + cardH - 14, enabled ? VIOLET : DIM);

            g.drawString(font, feature.title(), x + 23, y + 17, WHITE, false);
            g.drawString(font, feature.description(), x + 23, y + 36, MUTED, false);

            drawSwitch(g, x + cardW - 58, y + 27, enabled);
            g.drawString(font, enabled ? "ACTIVE" : "OFF", x + cardW - 66,
                    y + 53, enabled ? CYAN : DIM, false);
        }

        g.drawString(font, "SELECT A MODULE TO CHANGE ITS STATE",
                contentX, panelY + panelH - 29, DIM, false);
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean enabled) {
        int track = enabled ? 0xFF174A59 : 0xFF293647;
        int outline = enabled ? 0xFF43B9D4 : 0xFF46566A;
        g.fill(x, y, x + 42, y + 19, track);
        g.renderOutline(x, y, 42, 19, outline);
        int knobX = enabled ? x + 25 : x + 4;
        g.fill(knobX, y + 3, knobX + 13, y + 16, enabled ? CYAN : 0xFF91A0B2);
    }

    private void drawAbout(GuiGraphics g) {
        int x = contentX;
        int y = panelY + 128;
        int w = contentW;
        g.fill(x, y, x + w, y + 114, CARD);
        g.renderOutline(x, y, w, 114, BORDER);
        g.fill(x, y, x + 2, y + 114, CYAN);
        g.drawString(font, "VISUAL SYSTEM", x + 16, y + 15, CYAN, false);
        g.drawString(font, "Ruffy Visuals is a client-side overlay and cosmetics mod.",
                x + 16, y + 38, WHITE, false);
        g.drawString(font, "No server changes or gameplay automation.", x + 16, y + 56, MUTED, false);
        g.drawString(font, "CONFIG", x + 16, y + 82, DIM, false);
        g.drawString(font, ".minecraft/config/ruffyclient.properties", x + 76, y + 82, MUTED, false);

        int infoY = y + 128;
        g.drawString(font, "CONTROLS", x, infoY, WHITE, false);
        g.drawString(font, "RIGHT SHIFT   Open / close this control center", x, infoY + 22, MUTED, false);
        g.drawString(font, "ESC           Close the current screen", x, infoY + 40, MUTED, false);
    }

    private void drawFooter(GuiGraphics g) {
        int y = panelY + panelH - 1;
        g.fill(panelX, y, panelX + panelW, y + 1, BORDER);
        g.fill(panelX, y, panelX + 92, y + 1, CYAN);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        calculateLayout();
        double mouseX = event.x();
        double mouseY = event.y();

        int categoryY = panelY + 98;
        for (RuffyFeature.Category item : RuffyFeature.Category.values()) {
            if (inside(mouseX, mouseY, panelX + 12, categoryY, sidebarW - 24, 38)) {
                category = item;
                return true;
            }
            categoryY += 46;
        }

        if (category != RuffyFeature.Category.ABOUT) {
            List<RuffyFeature> features = RuffyFeature.ALL.stream()
                    .filter(feature -> feature.category() == category).toList();
            int columns = contentW >= 420 ? 2 : 1;
            int gap = 12;
            int cardW = (contentW - gap * (columns - 1)) / columns;
            int cardH = 76;
            int startY = panelY + 125;

            for (int i = 0; i < features.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x = contentX + col * (cardW + gap);
                int y = startY + row * (cardH + gap);
                if (inside(mouseX, mouseY, x, y, cardW, cardH)) {
                    RuffyFeature feature = features.get(i);
                    RuffyConfig.set(feature.key(), !RuffyConfig.get(feature.key()));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
