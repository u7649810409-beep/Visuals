package de.ruffy.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** A compact module panel with a dark nebula theme. */
public final class RuffyClickGui extends Screen {
    private static final int BG = 0xE90A0A18;
    private static final int SIDEBAR = 0xEE101025;
    private static final int CARD = 0xD91A1830;
    private static final int CARD_HOVER = 0xF0262044;
    private static final int PURPLE = 0xFF9D5CFF;
    private static final int CYAN = 0xFF63D7F5;
    private static final int WHITE = 0xFFF7F2FF;
    private static final int MUTED = 0xFFB7AACA;

    private RuffyFeature.Category category = RuffyFeature.Category.HUD;
    private int panelX, panelY, panelW, panelH, sidebarW, contentX, contentW;

    public RuffyClickGui() {
        super(Component.literal("Ruffy Visuals"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        calculateLayout();
        g.fill(0, 0, width, height, 0x99030712);
        drawStars(g);

        g.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0x552B1B56);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, BG);
        g.fill(panelX, panelY, panelX + panelW, panelY + 3, PURPLE);
        g.fill(panelX, panelY + 3, panelX + sidebarW, panelY + panelH, SIDEBAR);
        g.fill(panelX + sidebarW, panelY + 3, panelX + sidebarW + 1, panelY + panelH, 0x554F4770);

        g.drawString(font, "RUFFY", panelX + 18, panelY + 18, WHITE, false);
        g.drawString(font, "VISUALS CLIENT", panelX + 67, panelY + 18, PURPLE, false);
        g.drawString(font, "RIGHT SHIFT  ·  CLOSE WITH ESC", panelX + panelW - 180, panelY + 19, MUTED, false);
        g.fill(panelX + 16, panelY + 42, panelX + panelW - 16, panelY + 43, 0x443C315B);

        drawSidebar(g, mouseX, mouseY);
        drawContent(g, mouseX, mouseY);
    }

    private void calculateLayout() {
        panelW = Math.min(760, Math.max(360, width - 36));
        panelH = Math.min(500, Math.max(300, height - 36));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sidebarW = 146;
        contentX = panelX + sidebarW + 18;
        contentW = panelW - sidebarW - 36;
    }

    private void drawSidebar(GuiGraphics g, int mouseX, int mouseY) {
        int y = panelY + 63;
        for (RuffyFeature.Category item : RuffyFeature.Category.values()) {
            int x = panelX + 12;
            int w = sidebarW - 24;
            int h = 34;
            boolean selected = item == category;
            boolean hovered = inside(mouseX, mouseY, x, y, w, h);
            if (selected || hovered) {
                g.fill(x, y, x + w, y + h, selected ? 0xFF2A1B46 : 0x882A2540);
                if (selected) g.fill(x, y, x + 2, y + h, CYAN);
            }
            g.drawString(font, item.title(), x + 11, y + 7, selected ? WHITE : MUTED, false);
            y += 42;
        }
        g.drawString(font, "NEBULA THEME", panelX + 18, panelY + panelH - 44, PURPLE, false);
        g.drawString(font, "CLIENT SIDE ONLY", panelX + 18, panelY + panelH - 29, MUTED, false);
    }

    private void drawContent(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, category.title(), contentX, panelY + 63, WHITE, false);
        g.drawString(font, category.subtitle(), contentX, panelY + 77, MUTED, false);
        if (category == RuffyFeature.Category.ABOUT) {
            drawAbout(g);
            return;
        }

        List<RuffyFeature> features = RuffyFeature.ALL.stream()
                .filter(feature -> feature.category() == category).toList();
        int columns = contentW >= 430 ? 2 : 1;
        int cardGap = 10;
        int cardW = (contentW - cardGap * (columns - 1)) / columns;
        int cardH = 68;
        int startY = panelY + 112;

        for (int i = 0; i < features.size(); i++) {
            RuffyFeature feature = features.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = contentX + col * (cardW + cardGap);
            int y = startY + row * (cardH + 10);
            boolean hovered = inside(mouseX, mouseY, x, y, cardW, cardH);
            boolean enabled = RuffyConfig.get(feature.key());

            g.fill(x, y, x + cardW, y + cardH, hovered ? CARD_HOVER : CARD);
            g.renderOutline(x, y, cardW, cardH, hovered ? 0xAA8D6BC5 : 0x554B4266);
            g.fill(x + 10, y + 12, x + 12, y + cardH - 12, enabled ? PURPLE : 0xFF51465F);
            g.drawString(font, feature.title(), x + 20, y + 13, WHITE, false);
            g.drawString(font, feature.description(), x + 20, y + 31, MUTED, false);

            int pillW = 43;
            int pillX = x + cardW - pillW - 10;
            int pillY = y + 23;
            g.fill(pillX, pillY, pillX + pillW, pillY + 20, enabled ? 0xFF43266A : 0xFF2B2937);
            g.drawCenteredString(font, enabled ? "ON" : "OFF", pillX + pillW / 2,
                    pillY + 6, enabled ? 0xFFEBD9FF : MUTED);
        }
        g.drawString(font, "Click a card to toggle a feature", contentX, panelY + panelH - 24, MUTED, false);
    }

    private void drawAbout(GuiGraphics g) {
        int y = panelY + 125;
        String[] lines = {
                "Ruffy Visuals is an independent client-side visuals mod.",
                "It changes your HUD and adds cosmetic particle effects.",
                "It does not send gameplay commands or modify combat reach.",
                "",
                "Menu key: Right Shift",
                "Config: .minecraft/config/ruffyclient.properties",
                "Minecraft: Java Edition 1.21.11  ·  Fabric  ·  Java 21"
        };
        for (String line : lines) {
            g.drawString(font, line, contentX, y, line.isEmpty() ? MUTED : WHITE, false);
            y += 19;
        }
    }

    private void drawStars(GuiGraphics g) {
        for (int i = 0; i < 34; i++) {
            int x = Math.floorMod(i * 97 + 31, Math.max(1, width));
            int y = Math.floorMod(i * 53 + 17, Math.max(1, height));
            int color = (i % 4 == 0) ? 0x779D5CFF : 0x448BD7F5;
            g.fill(x, y, x + 2, y + 2, color);
        }
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

        int categoryY = panelY + 63;
        for (RuffyFeature.Category item : RuffyFeature.Category.values()) {
            if (inside(mouseX, mouseY, panelX + 12, categoryY, sidebarW - 24, 34)) {
                category = item;
                return true;
            }
            categoryY += 42;
        }

        if (category != RuffyFeature.Category.ABOUT) {
            List<RuffyFeature> features = RuffyFeature.ALL.stream()
                    .filter(feature -> feature.category() == category).toList();
            int columns = contentW >= 430 ? 2 : 1;
            int cardGap = 10;
            int cardW = (contentW - cardGap * (columns - 1)) / columns;
            int cardH = 68;
            int startY = panelY + 112;
            for (int i = 0; i < features.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x = contentX + col * (cardW + cardGap);
                int y = startY + row * (cardH + 10);
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
