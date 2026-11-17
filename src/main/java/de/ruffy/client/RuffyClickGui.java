package de.ruffy.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Module browser inspired by clean visual-client control panels:
 * category tabs, searchable module grid and a dedicated settings inspector.
 */
public final class RuffyClickGui extends Screen {
    private static final int WHITE = 0xFFF1F5FA;
    private static final int MUTED = 0xFF9AA9B9;
    private static final int DIM = 0xFF63758A;
    private static final int PANEL = 0xE90D141F;
    private static final int PANEL_ALT = 0xE9141D2A;
    private static final int CARD = 0x9B172230;
    private static final int CARD_HOVER = 0xC3223546;
    private static final int BORDER = 0x554E6578;
    private static final int GREEN = 0xFF67D8B4;

    private enum Tab {
        VISUALS("Visuals"), PRESETS("Presets"), WORLD("World"),
        HUD("HUD"), STYLE("Style"), CONFIGS("Configs"), SETUP("Setup");

        private final String label;
        Tab(String label) { this.label = label; }
    }

    private enum Filter {
        ALL("All"), ENTITIES("Entities"), MOTION("Motion"), STARRED("Starred");
        private final String label;
        Filter(String label) { this.label = label; }
    }

    private static final String[] PALETTE_NAMES = {
            "Nebula", "Orchid", "Emerald", "Solar", "Arctic", "Mono"
    };

    private Tab tab = Tab.VISUALS;
    private Filter filter = Filter.ALL;
    private EditBox searchBox;
    private String searchText = "";
    private String selectedKey = "nebula_particles";
    private int modulePage;
    private int panelX, panelY, panelW, panelH;
    private int leftX, leftW, rightX, rightW;
    private int searchX, searchY, searchW;

    public RuffyClickGui() {
        super(Component.literal("Ruffy Visuals Control Center"));
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();

        searchBox = new EditBox(font, searchX, searchY, searchW, 21,
                Component.literal("Search modules"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(36);
        searchBox.setTextColor(0xFFEAF2FA);
        searchBox.setHint(Component.literal("Search modules"));
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value.toLowerCase(Locale.ROOT);
            modulePage = 0;
        });
        searchBox.setVisible(tab == Tab.VISUALS);
        addRenderableWidget(searchBox);
    }

    private void calculateLayout() {
        panelW = Math.min(1320, Math.max(760, width - 44));
        panelH = Math.min(720, Math.max(460, height - 34));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        leftX = panelX + 22;
        leftW = Math.max(360, (panelW - 64) * 55 / 100);
        rightX = leftX + leftW + 20;
        rightW = panelX + panelW - 22 - rightX;
        searchX = leftX + 270;
        searchY = panelY + 132;
        searchW = Math.max(100, leftW - 278);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        calculateLayout();
        drawBackdrop(g);

        int alpha = 120 + RuffyConfig.getInt("menu_opacity") * 115 / 100;
        int body = (alpha << 24) | 0x000B111B;
        int header = (Math.min(250, alpha + 8) << 24) | 0x00131D2B;

        g.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0x5538B7CF);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, body);
        g.renderOutline(panelX, panelY, panelW, panelH, 0x775E8096);

        drawHeader(g, header);
        drawNavigation(g, mouseX, mouseY);

        switch (tab) {
            case VISUALS -> drawModuleBrowser(g, mouseX, mouseY, null);
            case WORLD -> drawModuleBrowser(g, mouseX, mouseY, RuffyFeature.Section.WORLD);
            case HUD -> drawHudPage(g, mouseX, mouseY);
            case PRESETS -> drawPresets(g, mouseX, mouseY);
            case STYLE -> drawStyle(g, mouseX, mouseY);
            case CONFIGS -> drawConfigs(g, mouseX, mouseY);
            case SETUP -> drawSetup(g);
        }

        drawFooter(g);
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawBackdrop(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xB8070C14);
        int grid = 0x087CC8DE;
        for (int x = 0; x < width; x += 56) g.fill(x, 0, x + 1, height, grid);
        for (int y = 0; y < height; y += 56) g.fill(0, y, width, y + 1, grid);
        int accent = RuffyConfig.primaryColor();
        for (int i = 0; i < 20; i++) {
            int x = Math.floorMod(i * 139 + 31, Math.max(1, width));
            int y = Math.floorMod(i * 83 + 19, Math.max(1, height));
            g.fill(x, y, x + 2, y + 2, withAlpha(accent, i % 3 == 0 ? 110 : 55));
        }
    }

    private void drawHeader(GuiGraphics g, int header) {
        int primary = RuffyConfig.primaryColor();
        g.fill(panelX, panelY, panelX + panelW, panelY + 58, header);
        g.fill(panelX, panelY, panelX + panelW, panelY + 2, primary);

        int logoX = panelX + 20;
        int logoY = panelY + 15;
        g.fill(logoX, logoY, logoX + 29, logoY + 29, 0xD91A2B3B);
        g.renderOutline(logoX, logoY, 29, 29, withAlpha(primary, 190));
        g.drawCenteredString(font, "R", logoX + 14, logoY + 9, primary);

        g.drawString(font, "RUFFY", panelX + 59, panelY + 14, WHITE, false);
        g.drawString(font, "VISUALS", panelX + 59, panelY + 29, primary, false);
        g.drawString(font, "CLIENT CONTROL CENTER", panelX + 138, panelY + 22, DIM, false);

        String status = "●  SYSTEM ONLINE";
        int statusWidth = font.width(status);
        g.drawString(font, status, panelX + panelW - statusWidth - 126,
                panelY + 22, GREEN, false);
        g.fill(panelX + panelW - 93, panelY + 15,
                panelX + panelW - 22, panelY + 43, 0x7A101A28);
        g.renderOutline(panelX + panelW - 93, panelY + 15, 71, 28, BORDER);
        g.drawCenteredString(font, "ESC  ×", panelX + panelW - 57,
                panelY + 25, MUTED);
        g.fill(panelX, panelY + 58, panelX + panelW, panelY + 59, BORDER);
    }

    private void drawNavigation(GuiGraphics g, int mouseX, int mouseY) {
        int y = panelY + 60;
        g.fill(panelX, y, panelX + panelW, y + 51, 0x6C101925);
        int x = panelX + 20;
        for (Tab item : Tab.values()) {
            int w = switch (item) {
                case VISUALS -> 112;
                case PRESETS, CONFIGS, SETUP -> 98;
                case WORLD, HUD, STYLE -> 82;
            };
            boolean selected = item == tab;
            boolean hovered = inside(mouseX, mouseY, x, y + 5, w - 5, 39);
            if (selected) {
                g.fill(x, y + 5, x + w - 5, y + 43, 0x8B1E3546);
                g.fill(x + 9, y + 41, x + w - 14, y + 43,
                        RuffyConfig.primaryColor());
            } else if (hovered) {
                g.fill(x, y + 5, x + w - 5, y + 43, 0x541F2B3A);
            }
            g.drawString(font, navIcon(item), x + 10, y + 18,
                    selected ? RuffyConfig.primaryColor() : DIM, false);
            g.drawString(font, item.label, x + 29, y + 18,
                    selected ? WHITE : MUTED, false);
            x += w;
        }
        g.fill(panelX, y + 50, panelX + panelW, y + 51, BORDER);
    }

    private String navIcon(Tab value) {
        return switch (value) {
            case VISUALS -> "✧";
            case PRESETS -> "▤";
            case WORLD -> "◇";
            case HUD -> "▣";
            case STYLE -> "◉";
            case CONFIGS -> "☷";
            case SETUP -> "⌘";
        };
    }

    private void drawModuleBrowser(GuiGraphics g, int mouseX, int mouseY,
                                   RuffyFeature.Section requiredSection) {
        int filterY = panelY + 122;
        drawFilterBar(g, mouseX, mouseY, filterY, requiredSection == null);

        List<RuffyFeature> features = filteredFeatures(requiredSection);
        int maxPages = Math.max(1, (features.size() + 15) / 16);
        modulePage = Math.max(0, Math.min(modulePage, maxPages - 1));

        int startY = panelY + 164;
        int gap = 8;
        int cardGap = 8;
        int cardW = (leftW - cardGap) / 2;
        int cardH = 41;

        for (int i = 0; i < 16; i++) {
            int featureIndex = modulePage * 16 + i;
            if (featureIndex >= features.size()) break;
            RuffyFeature feature = features.get(featureIndex);
            int col = i / 8;
            int row = i % 8;
            int x = leftX + col * (cardW + cardGap);
            int y = startY + row * (cardH + gap);
            drawModuleRow(g, mouseX, mouseY, feature, x, y, cardW, cardH);
        }

        drawModuleFooter(g, features.size(), maxPages);
        drawInspector(g, mouseX, mouseY, features);
    }

    private void drawFilterBar(GuiGraphics g, int mouseX, int mouseY, int y, boolean showFilters) {
        if (showFilters) {
            int x = leftX;
            for (Filter item : Filter.values()) {
                int w = switch (item) {
                    case ALL -> 48;
                    case ENTITIES -> 74;
                    case MOTION -> 64;
                    case STARRED -> 64;
                };
                boolean selected = item == filter;
                boolean hovered = inside(mouseX, mouseY, x, y, w, 24);
                if (selected || hovered) {
                    g.fill(x, y, x + w, y + 24,
                            selected ? 0xAA1C3A4D : 0x551E2B3B);
                    if (selected) g.fill(x, y + 22, x + w, y + 24,
                            RuffyConfig.primaryColor());
                }
                g.drawCenteredString(font, item.label, x + w / 2, y + 8,
                        selected ? WHITE : MUTED);
                x += w + 4;
            }
        } else {
            String title = tab == Tab.WORLD ? "WORLD MODULES" : "HUD MODULES";
            g.drawString(font, title, leftX + 3, y + 8, RuffyConfig.primaryColor(), false);
        }

        if (showFilters) {
            int searchWidth = searchBox == null ? searchW : searchBox.getWidth();
            g.fill(searchX - 5, searchY - 1, searchX + searchWidth + 5, searchY + 22,
                    0x77111D2A);
            g.renderOutline(searchX - 5, searchY - 1, searchWidth + 10, 23, BORDER);
            g.drawString(font, "⌕", searchX + 7, searchY + 6, DIM, false);
        }
        if (searchBox != null) searchBox.setVisible(showFilters);
    }

    private List<RuffyFeature> filteredFeatures(RuffyFeature.Section requiredSection) {
        List<RuffyFeature> result = new ArrayList<>();
        for (RuffyFeature feature : RuffyFeature.ALL) {
            if (requiredSection != null && feature.section() != requiredSection) continue;

            if (tab == Tab.HUD && feature.section() != RuffyFeature.Section.HUD) continue;
            if (tab == Tab.WORLD && feature.section() != RuffyFeature.Section.WORLD) continue;

            if (tab == Tab.VISUALS) {
                if (filter == Filter.ENTITIES && feature.group() != RuffyFeature.Group.ENTITIES) continue;
                if (filter == Filter.MOTION && feature.group() != RuffyFeature.Group.MOTION) continue;
                if (filter == Filter.STARRED && !RuffyConfig.get("favorite_" + feature.key())) continue;
            }

            if (!searchText.isBlank()) {
                String term = searchText.toLowerCase(Locale.ROOT);
                if (!feature.title().toLowerCase(Locale.ROOT).contains(term)
                        && !feature.description().toLowerCase(Locale.ROOT).contains(term)) continue;
            }
            result.add(feature);
        }
        return result;
    }

    private void drawModuleRow(GuiGraphics g, int mouseX, int mouseY, RuffyFeature feature,
                               int x, int y, int w, int h) {
        boolean selected = feature.key().equals(selectedKey);
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        boolean enabled = RuffyConfig.get(feature.key());
        boolean favorite = RuffyConfig.get("favorite_" + feature.key());
        int primary = RuffyConfig.primaryColor();

        g.fill(x, y, x + w, y + h,
                selected ? 0xB32A3C4E : hovered ? CARD_HOVER : CARD);
        g.renderOutline(x, y, w, h, selected ? withAlpha(primary, 150) : BORDER);
        if (selected || enabled) {
            g.fill(x, y + 7, x + 2, y + h - 7,
                    enabled ? primary : withAlpha(primary, 90));
        }

        String symbol = iconFor(feature);
        g.drawString(font, symbol, x + 10, y + 14,
                enabled ? primary : DIM, false);
        g.drawString(font, feature.title(), x + 31, y + 8,
                selected ? WHITE : 0xFFE0E7EF, false);
        g.drawString(font, feature.section().title().toUpperCase(Locale.ROOT),
                x + 31, y + 23, DIM, false);

        int starX = x + w - 57;
        g.drawString(font, favorite ? "★" : "☆", starX, y + 13,
                favorite ? 0xFFFFD47A : DIM, false);
        drawSwitch(g, x + w - 34, y + 12, enabled, 26, 16);
    }

    private String iconFor(RuffyFeature feature) {
        return switch (feature.section()) {
            case HUD -> "▣";
            case MOTION -> "✧";
            case ENTITIES -> "◇";
            case WORLD -> "◌";
        };
    }

    private void drawModuleFooter(GuiGraphics g, int count, int pages) {
        int enabled = 0;
        for (RuffyFeature feature : RuffyFeature.ALL) {
            if (RuffyConfig.get(feature.key())) enabled++;
        }
        int y = panelY + panelH - 42;
        g.drawString(font, count + " MODULES  ·  " + enabled + " ENABLED",
                leftX + 2, y + 7, MUTED, false);

        int mid = leftX + leftW - 112;
        drawSmallButton(g, mid, y, 24, 23, "‹", modulePage > 0);
        g.drawCenteredString(font, (modulePage + 1) + " / " + pages,
                mid + 55, y + 8, WHITE);
        drawSmallButton(g, mid + 86, y, 24, 23, "›", modulePage < pages - 1);
    }

    private void drawInspector(GuiGraphics g, int mouseX, int mouseY,
                               List<RuffyFeature> visibleFeatures) {
        int x = rightX;
        int y = panelY + 122;
        int w = rightW;
        int h = panelH - 164;
        g.fill(x, y, x + w, y + h, 0x7A0E1723);
        g.renderOutline(x, y, w, h, BORDER);
        g.fill(x, y, x + w, y + 2, 0x553F91AA);

        RuffyFeature selected = RuffyFeature.ALL.stream()
                .filter(feature -> feature.key().equals(selectedKey)).findFirst().orElse(null);

        if (selected == null) {
            g.drawCenteredString(font, "⚙", x + w / 2, y + h / 2 - 18, DIM);
            g.drawCenteredString(font, "SELECT A MODULE", x + w / 2,
                    y + h / 2 + 8, MUTED);
            return;
        }

        int pad = 20;
        int primary = RuffyConfig.primaryColor();
        g.drawString(font, "MODULE SETTINGS", x + pad, y + 18, DIM, false);

        g.fill(x + pad, y + 44, x + pad + 42, y + 86, 0xAA1D3040);
        g.renderOutline(x + pad, y + 44, 42, 42, withAlpha(primary, 140));
        g.drawCenteredString(font, iconFor(selected), x + pad + 21, y + 58, primary);

        g.drawString(font, selected.title(), x + pad + 56, y + 49, WHITE, false);
        g.drawString(font, selected.section().title().toUpperCase(Locale.ROOT),
                x + pad + 56, y + 67, primary, false);

        int descY = y + 105;
        drawWrapped(g, selected.description(), x + pad, descY, w - pad * 2, MUTED, 16);

        int toggleY = y + 174;
        g.fill(x + pad, toggleY, x + w - pad, toggleY + 50, 0x641A2B3B);
        g.drawString(font, "MODULE STATE", x + pad + 12, toggleY + 10, MUTED, false);
        boolean active = RuffyConfig.get(selected.key());
        g.drawString(font, active ? "ENABLED" : "DISABLED",
                x + pad + 12, toggleY + 27, active ? primary : DIM, false);
        drawSwitch(g, x + w - pad - 48, toggleY + 16, active, 36, 18);

        int infoY = toggleY + 70;
        g.drawString(font, "DETAILS", x + pad, infoY, DIM, false);
        g.drawString(font, "Client-side visual module", x + pad, infoY + 20, WHITE, false);
        g.drawString(font, "No server changes or combat automation", x + pad,
                infoY + 38, MUTED, false);
        g.drawString(font, "CONFIG KEY", x + pad, infoY + 72, DIM, false);
        g.drawString(font, selected.key(), x + pad, infoY + 90, MUTED, false);

        if (selected.key().equals("watermark") || selected.section() == RuffyFeature.Section.HUD) {
            g.drawString(font, "HUD OPACITY", x + pad, infoY + 122, DIM, false);
            drawSlider(g, x + pad, infoY + 143, w - pad * 2,
                    RuffyConfig.getInt("hud_opacity"), 12, 90);
        }

        if (h > 430) {
            g.drawString(font, "ACCENT COLOR", x + pad, y + h - 61, DIM, false);
            swatch(g, x + pad, y + h - 40, 22, primary,
                    true, mouseX, mouseY);
            swatch(g, x + pad + 32, y + h - 40, 22,
                    RuffyConfig.secondaryColor(), false, mouseX, mouseY);
            g.drawString(font, "EDIT IN STYLE TAB", x + pad + 66,
                    y + h - 33, MUTED, false);
        }
    }

    private void drawHudPage(GuiGraphics g, int mouseX, int mouseY) {
        drawModuleBrowser(g, mouseX, mouseY, RuffyFeature.Section.HUD);
        int x = rightX + 18;
        int y = panelY + panelH - 100;
        g.drawString(font, "TRANSPARENCY", x, y - 24, DIM, false);
        drawSlider(g, x, y, rightW - 36, RuffyConfig.getInt("hud_opacity"), 12, 90);
    }

    private void drawPresets(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX + 32;
        int y = panelY + 132;
        g.drawString(font, "QUICK PROFILES", x, y - 26, DIM, false);
        String[] names = {"NEBULA", "CINEMATIC", "PERFORMANCE", "CLEAN"};
        String[] descriptions = {
                "Balanced cyan and violet visuals",
                "Soft particles and a subtle HUD",
                "Reduced particles for higher FPS",
                "Minimal UI and low transparency"
        };
        int[] palettes = {0, 1, 4, 5};
        int gap = 16;
        int cardW = Math.min(420, (panelW - 92) / 2);
        int cardH = 116;
        for (int i = 0; i < names.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int cx = x + col * (cardW + gap);
            int cy = y + row * (cardH + gap);
            boolean hovered = inside(mouseX, mouseY, cx, cy, cardW, cardH);
            int color = palettePrimary(palettes[i]);
            g.fill(cx, cy, cx + cardW, cy + cardH, hovered ? CARD_HOVER : CARD);
            g.renderOutline(cx, cy, cardW, cardH,
                    hovered ? withAlpha(color, 190) : BORDER);
            g.fill(cx, cy, cx + cardW, cy + 2, color);
            g.fill(cx + 18, cy + 20, cx + 52, cy + 54, withAlpha(color, 42));
            g.renderOutline(cx + 18, cy + 20, 34, 34, withAlpha(color, 175));
            g.drawCenteredString(font, "✧", cx + 35, cy + 30, color);
            g.drawString(font, names[i], cx + 66, cy + 22, WHITE, false);
            g.drawString(font, descriptions[i], cx + 66, cy + 41, MUTED, false);
            g.drawString(font, "APPLY PROFILE  →", cx + 18, cy + 84, color, false);
        }
    }

    private void drawStyle(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX + 34;
        int y = panelY + 128;
        g.drawString(font, "COLOR SYSTEM", x, y, WHITE, false);
        g.drawString(font, "Choose a coordinated primary and secondary accent palette.",
                x, y + 18, MUTED, false);

        int sw = 178;
        int sh = 82;
        int gap = 14;
        for (int i = 0; i < PALETTE_NAMES.length; i++) {
            int col = i % 3;
            int row = i / 3;
            int cx = x + col * (sw + gap);
            int cy = y + 52 + row * (sh + gap);
            int primary = palettePrimary(i);
            boolean chosen = RuffyConfig.getInt("palette") == i;
            boolean hovered = inside(mouseX, mouseY, cx, cy, sw, sh);
            g.fill(cx, cy, cx + sw, cy + sh, hovered ? CARD_HOVER : CARD);
            g.renderOutline(cx, cy, sw, sh,
                    chosen ? withAlpha(primary, 220) : BORDER);
            g.fill(cx, cy, cx + sw, cy + 2, primary);
            swatch(g, cx + 14, cy + 19, 28, primary, chosen, mouseX, mouseY);
            swatch(g, cx + 50, cy + 19, 28, paletteSecondary(i), false, mouseX, mouseY);
            g.drawString(font, PALETTE_NAMES[i].toUpperCase(Locale.ROOT),
                    cx + 90, cy + 25, chosen ? WHITE : MUTED, false);
            if (chosen) g.drawString(font, "SELECTED", cx + 90, cy + 43, primary, false);
        }

        int sliderY = y + 52 + 2 * (sh + gap) + 24;
        g.drawString(font, "HUD TRANSPARENCY", x, sliderY, WHITE, false);
        g.drawString(font, RuffyConfig.getInt("hud_opacity") + "%",
                x + 280, sliderY, RuffyConfig.primaryColor(), false);
        drawSlider(g, x, sliderY + 24, 330, RuffyConfig.getInt("hud_opacity"), 12, 90);

        int menuY = sliderY + 82;
        g.drawString(font, "MENU OPACITY", x, menuY, WHITE, false);
        g.drawString(font, RuffyConfig.getInt("menu_opacity") + "%",
                x + 280, menuY, RuffyConfig.primaryColor(), false);
        drawSlider(g, x, menuY + 24, 330, RuffyConfig.getInt("menu_opacity"), 60, 100);
    }

    private void drawConfigs(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX + 34;
        int y = panelY + 132;
        g.drawString(font, "CONFIGURATION", x, y, WHITE, false);
        g.drawString(font, "Your options are saved automatically.", x, y + 19, MUTED, false);

        drawActionCard(g, mouseX, mouseY, x, y + 58, 360, 70,
                "RESET MODULES", "Restore the default module states");
        drawActionCard(g, mouseX, mouseY, x, y + 142, 360, 70,
                "RESET STYLE", "Return to the Nebula palette and opacity");
        drawActionCard(g, mouseX, mouseY, x, y + 226, 360, 70,
                "RESET EVERYTHING", "Restore all client options to defaults");

        int infoX = x + 410;
        g.fill(infoX, y + 58, infoX + 380, y + 296, 0x65131E2B);
        g.renderOutline(infoX, y + 58, 380, 238, BORDER);
        g.drawString(font, "LOCAL CONFIG", infoX + 18, y + 78, RuffyConfig.primaryColor(), false);
        g.drawString(font, ".minecraft/config/ruffyclient.properties",
                infoX + 18, y + 105, WHITE, false);
        g.drawString(font, "Version: 1.21.11 / Fabric", infoX + 18, y + 137, MUTED, false);
        g.drawString(font, "The config stays on this client.", infoX + 18, y + 159, MUTED, false);
    }

    private void drawSetup(GuiGraphics g) {
        int x = panelX + 34;
        int y = panelY + 132;
        g.drawString(font, "SETUP & CONTROLS", x, y, WHITE, false);
        drawInfoRow(g, x, y + 42, "RIGHT SHIFT", "Open or close the Visuals control center");
        drawInfoRow(g, x, y + 82, "LEFT CLICK", "Select modules, stars, colors and settings");
        drawInfoRow(g, x, y + 122, "ESC", "Close the control center");
        drawInfoRow(g, x, y + 162, "CONFIG", "Saved automatically in the Minecraft config folder");
        drawInfoRow(g, x, y + 202, "COMPATIBILITY", "Minecraft Java 1.21.11 · Fabric · Java 21");
        drawInfoRow(g, x, y + 242, "VISUAL EFFECTS", "Client-side cosmetics; no server-side combat changes");

        int right = x + Math.min(700, (panelW - 110) / 2);
        g.fill(right, y + 36, right + 340, y + 285, 0x65131E2B);
        g.renderOutline(right, y + 36, 340, 249, BORDER);
        g.drawString(font, "RENDER PROFILE", right + 18, y + 56, RuffyConfig.primaryColor(), false);
        g.drawString(font, "Balanced", right + 18, y + 83, WHITE, false);
        g.drawString(font, "Use the Performance preset to reduce", right + 18, y + 116, MUTED, false);
        g.drawString(font, "the number of cosmetic particles.", right + 18, y + 134, MUTED, false);
        g.drawString(font, "Tip: disable individual effects you", right + 18, y + 174, MUTED, false);
        g.drawString(font, "do not use to keep the screen clean.", right + 18, y + 192, MUTED, false);
    }

    private void drawActionCard(GuiGraphics g, int mouseX, int mouseY,
                                int x, int y, int w, int h, String title, String subtitle) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        g.fill(x, y, x + w, y + h, hovered ? CARD_HOVER : CARD);
        g.renderOutline(x, y, w, h, hovered ? withAlpha(RuffyConfig.primaryColor(), 170) : BORDER);
        g.fill(x, y, x + 2, y + h, RuffyConfig.primaryColor());
        g.drawString(font, title, x + 16, y + 16, WHITE, false);
        g.drawString(font, subtitle, x + 16, y + 37, MUTED, false);
    }

    private void drawInfoRow(GuiGraphics g, int x, int y, String left, String right) {
        g.fill(x, y, x + 670, y + 31, 0x57172230);
        g.drawString(font, left, x + 12, y + 10, RuffyConfig.primaryColor(), false);
        g.drawString(font, right, x + 150, y + 10, MUTED, false);
    }

    private void drawSlider(GuiGraphics g, int x, int y, int w, int value, int min, int max) {
        int barY = y + 7;
        int fill = Math.max(0, Math.min(w, (value - min) * w / (max - min)));
        g.fill(x, barY, x + w, barY + 4, 0xFF2B3948);
        g.fill(x, barY, x + fill, barY + 4, RuffyConfig.primaryColor());
        g.fill(x + fill - 3, barY - 3, x + fill + 3, barY + 7, WHITE);
        g.renderOutline(x, barY - 3, w, 10, 0x444F6274);
    }

    private void swatch(GuiGraphics g, int x, int y, int size, int color,
                        boolean selected, int mouseX, int mouseY) {
        g.fill(x, y, x + size, y + size, color);
        g.renderOutline(x, y, size, size,
                selected ? WHITE : 0x88596B7F);
        if (selected) g.renderOutline(x - 2, y - 2, size + 4, size + 4,
                withAlpha(RuffyConfig.primaryColor(), 220));
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean enabled, int w, int h) {
        int track = enabled ? withAlpha(RuffyConfig.primaryColor(), 160) : 0xFF293542;
        g.fill(x, y, x + w, y + h, track);
        g.renderOutline(x, y, w, h, enabled
                ? withAlpha(RuffyConfig.primaryColor(), 190) : BORDER);
        int knob = Math.max(8, h - 6);
        int knobX = enabled ? x + w - knob - 3 : x + 3;
        g.fill(knobX, y + 3, knobX + knob, y + h - 3,
                enabled ? WHITE : 0xFFAAB6C2);
    }

    private void drawSmallButton(GuiGraphics g, int x, int y, int w, int h,
                                 String label, boolean active) {
        g.fill(x, y, x + w, y + h, active ? 0xAA1C3647 : 0x55202B38);
        g.renderOutline(x, y, w, h, active ? withAlpha(RuffyConfig.primaryColor(), 170) : BORDER);
        g.drawCenteredString(font, label, x + w / 2, y + 7,
                active ? WHITE : DIM);
    }

    private void drawWrapped(GuiGraphics g, String text, int x, int y,
                             int maxWidth, int color, int lineHeight) {
        StringBuilder line = new StringBuilder();
        int lineY = y;
        for (String word : text.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.width(candidate) > maxWidth && !line.isEmpty()) {
                g.drawString(font, line.toString(), x, lineY, color, false);
                lineY += lineHeight;
                line.setLength(0);
                line.append(word);
            } else {
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
        }
        if (!line.isEmpty()) g.drawString(font, line.toString(), x, lineY, color, false);
    }

    private void drawFooter(GuiGraphics g) {
        int y = panelY + panelH - 1;
        g.fill(panelX, y, panelX + panelW, y + 1, BORDER);
        g.fill(panelX, y, panelX + 114, y + 1, RuffyConfig.primaryColor());
        g.drawString(font, "RUFFY VISUALS  ·  CLIENT ONLY",
                panelX + 21, panelY + panelH - 21, DIM, false);
        g.drawString(font, "RIGHT SHIFT TO TOGGLE",
                panelX + panelW - 156, panelY + panelH - 21, DIM, false);
    }

    private static int palettePrimary(int palette) {
        int[] colors = {0xFF58D8F5, 0xFFFF75CF, 0xFF67E4B6,
                0xFFFFC36B, 0xFF91BFFF, 0xFFE1E8F2};
        return colors[Math.max(0, Math.min(colors.length - 1, palette))];
    }

    private static int paletteSecondary(int palette) {
        int[] colors = {0xFF9B7BFF, 0xFF9D85FF, 0xFF65CFFF,
                0xFFFF826F, 0xFFB6D5FF, 0xFF9AA8BA};
        return colors[Math.max(0, Math.min(colors.length - 1, palette))];
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private static boolean inside(double mouseX, double mouseY,
                                  int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w
                && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        calculateLayout();
        int mx = (int) event.x();
        int my = (int) event.y();

        if (handleNavigation(mx, my)) return true;

        if (tab == Tab.VISUALS || tab == Tab.WORLD || tab == Tab.HUD) {
            if (tab == Tab.VISUALS && handleFilters(mx, my)) return true;
            if (handleModuleListClick(mx, my)) return true;
            if (handleInspectorClick(mx, my)) return true;
            if (tab == Tab.HUD && handleHudOpacityClick(mx, my)) return true;
            if (handlePagination(mx, my)) return true;
        }

        if (tab == Tab.PRESETS && handlePresetClick(mx, my)) return true;
        if (tab == Tab.STYLE && handleStyleClick(mx, my)) return true;
        if (tab == Tab.CONFIGS && handleConfigClick(mx, my)) return true;
        return true;
    }

    private boolean handleNavigation(int mx, int my) {
        int y = panelY + 60;
        if (my < y + 5 || my > y + 43) return false;
        int x = panelX + 20;
        for (Tab item : Tab.values()) {
            int w = switch (item) {
                case VISUALS -> 112;
                case PRESETS, CONFIGS, SETUP -> 98;
                case WORLD, HUD, STYLE -> 82;
            };
            if (inside(mx, my, x, y + 5, w - 5, 39)) {
                tab = item;
                modulePage = 0;
                if (searchBox != null) searchBox.setVisible(tab == Tab.VISUALS);
                return true;
            }
            x += w;
        }
        return false;
    }

    private boolean handleFilters(int mx, int my) {
        int y = panelY + 122;
        int x = leftX;
        for (Filter item : Filter.values()) {
            int w = switch (item) {
                case ALL -> 48;
                case ENTITIES -> 74;
                case MOTION -> 64;
                case STARRED -> 64;
            };
            if (inside(mx, my, x, y, w, 24)) {
                filter = item;
                modulePage = 0;
                return true;
            }
            x += w + 4;
        }
        return false;
    }

    private boolean handleModuleListClick(int mx, int my) {
        RuffyFeature.Section section = tab == Tab.HUD ? RuffyFeature.Section.HUD
                : tab == Tab.WORLD ? RuffyFeature.Section.WORLD : null;
        List<RuffyFeature> features = filteredFeatures(section);
        int startY = panelY + 164;
        int gap = 8;
        int cardGap = 8;
        int cardW = (leftW - cardGap) / 2;
        int cardH = 41;

        for (int i = 0; i < 16; i++) {
            int index = modulePage * 16 + i;
            if (index >= features.size()) break;
            RuffyFeature feature = features.get(index);
            int col = i / 8;
            int row = i % 8;
            int x = leftX + col * (cardW + cardGap);
            int y = startY + row * (cardH + gap);
            if (!inside(mx, my, x, y, cardW, cardH)) continue;

            if (mx >= x + cardW - 62 && mx < x + cardW - 38) {
                String key = "favorite_" + feature.key();
                RuffyConfig.set(key, !RuffyConfig.get(key));
                return true;
            }
            if (mx >= x + cardW - 36) {
                RuffyConfig.set(feature.key(), !RuffyConfig.get(feature.key()));
                selectedKey = feature.key();
                return true;
            }
            selectedKey = feature.key();
            return true;
        }
        return false;
    }

    private boolean handleInspectorClick(int mx, int my) {
        int x = rightX;
        int y = panelY + 122;
        RuffyFeature selected = RuffyFeature.ALL.stream()
                .filter(feature -> feature.key().equals(selectedKey)).findFirst().orElse(null);
        if (selected == null) return false;

        int toggleY = y + 174;
        if (inside(mx, my, x + 16, toggleY, rightW - 32, 50)) {
            RuffyConfig.set(selected.key(), !RuffyConfig.get(selected.key()));
            return true;
        }
        return false;
    }

    private boolean handlePagination(int mx, int my) {
        int y = panelY + panelH - 42;
        int mid = leftX + leftW - 112;
        if (!inside(mx, my, mid, y, 110, 23)) return false;
        List<RuffyFeature> features = filteredFeatures(
                tab == Tab.HUD ? RuffyFeature.Section.HUD
                        : tab == Tab.WORLD ? RuffyFeature.Section.WORLD : null
        );
        int pages = Math.max(1, (features.size() + 15) / 16);
        if (mx < mid + 25) modulePage = Math.max(0, modulePage - 1);
        else if (mx > mid + 84) modulePage = Math.min(pages - 1, modulePage + 1);
        return true;
    }

    private boolean handleHudOpacityClick(int mx, int my) {
        int x = rightX + 18;
        int y = panelY + panelH - 100;
        if (!inside(mx, my, x, y - 24, rightW - 36, 42)) return false;
        int value = 12 + Math.round((mx - x) * 78.0F / Math.max(1, rightW - 36));
        RuffyConfig.setInt("hud_opacity", value);
        return true;
    }

    private boolean handlePresetClick(int mx, int my) {
        int x = panelX + 32;
        int y = panelY + 132;
        int gap = 16;
        int cardW = Math.min(420, (panelW - 92) / 2);
        int cardH = 116;
        for (int i = 0; i < 4; i++) {
            int cx = x + (i % 2) * (cardW + gap);
            int cy = y + (i / 2) * (cardH + gap);
            if (inside(mx, my, cx, cy, cardW, cardH)) {
                applyPreset(i);
                return true;
            }
        }
        return false;
    }

    private void applyPreset(int preset) {
        RuffyConfig.setInt("palette", switch (preset) {
            case 1 -> 1;
            case 2 -> 4;
            case 3 -> 5;
            default -> 0;
        });
        RuffyConfig.setInt("hud_opacity", switch (preset) {
            case 1 -> 34;
            case 2 -> 30;
            case 3 -> 22;
            default -> 44;
        });

        boolean reduced = preset == 2 || preset == 3;
        RuffyConfig.set("nebula_particles", !reduced);
        RuffyConfig.set("orbit_particles", preset == 1);
        RuffyConfig.set("movement_trail", preset == 0 || preset == 1);
        RuffyConfig.set("jump_burst", preset != 3);
        RuffyConfig.set("sprint_dust", preset == 1);
        RuffyConfig.set("landing_ring", preset != 3);
        RuffyConfig.set("step_sparks", preset == 1);
        RuffyConfig.set("critical_sparks", preset != 3);
        RuffyConfig.set("hit_sparks", true);
        RuffyConfig.set("low_health_aura", preset == 1);
        RuffyConfig.set("potion_glow", preset == 1);
        RuffyConfig.set("damage_pulse", true);
        RuffyConfig.set("underwater_bubbles", true);
        RuffyConfig.set("weather_fx", preset == 1);
        RuffyConfig.set("cave_dust", preset == 1);
        RuffyConfig.set("starfall", preset == 1);
    }

    private boolean handleStyleClick(int mx, int my) {
        int x = panelX + 34;
        int y = panelY + 128;
        int sw = 178;
        int sh = 82;
        int gap = 14;
        for (int i = 0; i < PALETTE_NAMES.length; i++) {
            int cx = x + (i % 3) * (sw + gap);
            int cy = y + 52 + (i / 3) * (sh + gap);
            if (inside(mx, my, cx, cy, sw, sh)) {
                RuffyConfig.setInt("palette", i);
                return true;
            }
        }

        int sliderY = y + 52 + 2 * (sh + gap) + 24;
        if (inside(mx, my, x, sliderY + 17, 330, 19)) {
            int value = 12 + Math.round((mx - x) * 78.0F / 330.0F);
            RuffyConfig.setInt("hud_opacity", value);
            return true;
        }

        int menuY = sliderY + 82;
        if (inside(mx, my, x, menuY + 17, 330, 19)) {
            int value = 60 + Math.round((mx - x) * 40.0F / 330.0F);
            RuffyConfig.setInt("menu_opacity", value);
            return true;
        }
        return false;
    }

    private boolean handleConfigClick(int mx, int my) {
        int x = panelX + 34;
        int y = panelY + 132;
        if (inside(mx, my, x, y + 58, 360, 70)) {
            for (RuffyFeature feature : RuffyFeature.ALL) {
                // The module reset restores feature defaults without clearing style or favorites.
                RuffyConfig.set(feature.key(), defaultModuleState(feature.key()));
            }
            return true;
        }
        if (inside(mx, my, x, y + 142, 360, 70)) {
            RuffyConfig.setInt("palette", 0);
            RuffyConfig.setInt("hud_opacity", 44);
            RuffyConfig.setInt("menu_opacity", 94);
            return true;
        }
        if (inside(mx, my, x, y + 226, 360, 70)) {
            RuffyConfig.reset();
            return true;
        }
        return false;
    }

    private boolean defaultModuleState(String key) {
        return switch (key) {
            case "watermark", "fps", "keystrokes", "direction",
                    "nebula_particles", "jump_burst", "landing_ring",
                    "hit_sparks", "swing_animation", "critical_sparks",
                    "damage_pulse", "underwater_bubbles" -> true;
            default -> false;
        };
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
