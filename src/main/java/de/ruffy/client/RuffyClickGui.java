package de.ruffy.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact inventory-sized control center for Ruffy Visuals. */
public final class RuffyClickGui extends Screen {
    private static final int WHITE = 0xFFFFF3F5;
    private static final int MUTED = 0xFFC8A9B2;
    private static final int DIM = 0xFF936B77;
    private static final int CARD = 0xA51D0D14;
    private static final int CARD_HOVER = 0xD52B101A;
    private static final int BORDER = 0x665C192A;
    private static final int GREEN = 0xFFFF365B;

    private enum Tab {
        VISUALS("Visuals", 68), PRESETS("Presets", 62), WORLD("World", 56),
        HUD("HUD", 44), STYLE("Style", 48), CONFIGS("Configs", 58), SETUP("Setup", 48);
        private final String label;
        private final int width;
        Tab(String label, int width) { this.label = label; this.width = width; }
    }

    private enum Filter {
        ALL("All", 35), ENTITIES("Entities", 59), MOTION("Motion", 48), STARRED("★", 28);
        private final String label;
        private final int width;
        Filter(String label, int width) { this.label = label; this.width = width; }
    }

    private static final String[] PALETTE_NAMES = {
            "RED", "ROSE", "MAG", "VIO", "AZURE", "ICE", "MINT", "EMER", "AMBER", "SOLAR"
    };

    private Tab tab = Tab.VISUALS;
    private Filter filter = Filter.ALL;
    private EditBox searchBox;
    private String searchText = "";
    private String selectedKey = "watermark";
    private int modulePage;
    private int panelX, panelY, panelW, panelH;
    private int leftX, leftW, rightX, rightW;
    private int searchX, searchY, searchW;
    private int listTop, rowHeight;

    public RuffyClickGui() {
        super(Component.literal("Ruffy Visuals Control Center"));
    }

    @Override
    protected void init() {
        super.init();
        RuffyAnimation.set("gui.open", 0.0F);
        calculateLayout();
        searchBox = new EditBox(font, searchX, searchY + 3, searchW, 18,
                Component.literal("Search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(36);
        searchBox.setTextColor(0xFFEAF2FA);
        searchBox.setHint(Component.literal("Search"));
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value.toLowerCase(Locale.ROOT);
            modulePage = 0;
        });
        searchBox.setVisible(tab == Tab.VISUALS);
        addRenderableWidget(searchBox);
    }

    private void calculateLayout() {
        panelW = Math.min(472, Math.max(370, width - 24));
        panelH = Math.min(318, Math.max(288, height - 24));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        leftX = panelX + 14;
        leftW = (panelW - 40) * 55 / 100;
        rightX = leftX + leftW + 12;
        rightW = panelX + panelW - 14 - rightX;

        boolean stackedSearch = leftW < 250;
        searchX = stackedSearch ? leftX : leftX + 177;
        searchY = stackedSearch ? panelY + 100 : panelY + 79;
        searchW = stackedSearch ? leftW : leftW - 181;
        listTop = panelY + (stackedSearch ? 130 : 108);
        rowHeight = Math.max(16, Math.min(22, (panelH - (listTop - panelY) - 46) / 8));
    }

    private void applyFrameLayout() {
        calculateLayout();
        float opening = RuffyAnimation.animate("gui.open", 1.0F);
        int slide = Math.round((1.0F - opening) * 10.0F);
        panelY += slide;
        searchY += slide;
        listTop += slide;
        if (searchBox != null) searchBox.setPosition(searchX, searchY + 3);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        applyFrameLayout();
        drawBackdrop(g);

        int alpha = 120 + RuffyConfig.getInt("menu_opacity") * 115 / 100;
        int body = (alpha << 24) | 0x000D0C13;
        int header = (Math.min(250, alpha + 8) << 24) | 0x00171320;
        int accent = RuffyConfig.primaryColor();

        // Layered crimson edge bloom keeps the launcher crisp instead of flat.
        fillRounded(g, panelX - 5, panelY - 5, panelW + 10, panelH + 10,
                withAlpha(accent, 18), 13);
        fillRounded(g, panelX - 2, panelY - 2, panelW + 4, panelH + 4,
                withAlpha(accent, 42), 12);
        fillRounded(g, panelX, panelY, panelW, panelH, body, 11);
        outlineRounded(g, panelX - 1, panelY - 1, panelW + 2, panelH + 2,
                withAlpha(accent, 190), 12);
        outlineRounded(g, panelX, panelY, panelW, panelH, 0x995B2A39, 11);

        drawHeader(g, header);
        drawNavigation(g, mouseX, mouseY);

        switch (tab) {
            case VISUALS -> drawModuleBrowser(g, mouseX, mouseY, null);
            case WORLD -> drawModuleBrowser(g, mouseX, mouseY, RuffyFeature.Section.WORLD);
            case HUD -> drawModuleBrowser(g, mouseX, mouseY, RuffyFeature.Section.HUD);
            case PRESETS -> drawPresets(g, mouseX, mouseY);
            case STYLE -> drawStyle(g, mouseX, mouseY);
            case CONFIGS -> drawConfigs(g, mouseX, mouseY);
            case SETUP -> drawSetup(g);
        }

        drawFooter(g);
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawBackdrop(GuiGraphics g) {
        g.fill(0, 0, width, height, 0xC90B0508);
        int accent = RuffyConfig.primaryColor();
        int trace = withAlpha(accent, 18);
        int brightTrace = withAlpha(accent, 38);

        // Quiet circuit-board traces stay around the edges and do not compete with text.
        for (int i = 0; i < 7; i++) {
            int y = 30 + i * 42;
            int length = 38 + (i * 23) % 72;
            g.fill(0, y, length, y + 1, trace);
            g.fill(length - 1, y, length, y + 12, trace);
            g.fill(length - 1, y + 11, length + 22, y + 12, brightTrace);

            int right = width - length - 22;
            g.fill(right, height - y, width, height - y + 1, trace);
            g.fill(right, height - y - 12, right + 1, height - y + 1, trace);
            g.fill(right - 22, height - y - 12, right + 1, height - y - 11, brightTrace);
            g.fill(length + 20, y + 9, length + 23, y + 12, brightTrace);
            g.fill(right - 23, height - y - 13, right - 20, height - y - 10, brightTrace);
        }

        for (int i = 0; i < 18; i++) {
            int x = Math.floorMod(i * 139 + 31, Math.max(1, width));
            int y = Math.floorMod(i * 83 + 19, Math.max(1, height));
            g.fill(x, y, x + 2, y + 2, withAlpha(accent, i % 4 == 0 ? 88 : 36));
        }
    }

    private void drawHeader(GuiGraphics g, int header) {
        int primary = RuffyConfig.primaryColor();
        int secondary = RuffyConfig.secondaryColor();
        fillRounded(g, panelX, panelY, panelW, 39, header, 11);
        g.fill(panelX, panelY + 11, panelX + panelW, panelY + 39, header);
        fillRounded(g, panelX, panelY, panelW, 3, primary, 2);
        g.fill(panelX + 3, panelY + 37, panelX + panelW - 3, panelY + 39,
                withAlpha(secondary, 70));

        int logoX = panelX + 12;
        int logoY = panelY + 7;
        fillRounded(g, logoX - 2, logoY - 2, 29, 29,
                withAlpha(primary, 28), 10);
        fillRounded(g, logoX, logoY, 24, 24, 0xE8200B12, 8);
        outlineRounded(g, logoX, logoY, 24, 24, withAlpha(primary, 210), 8);
        g.drawCenteredString(font, "R", logoX + 12, logoY + 6, primary);

        g.drawString(font, "RUFFY", panelX + 43, panelY + 8, WHITE, false);
        g.drawString(font, "VISUALS", panelX + 43, panelY + 21, primary, false);
        g.drawString(font, "CLIENT", panelX + 103, panelY + 15, DIM, false);
        g.fill(panelX + 142, panelY + 11, panelX + 143, panelY + 29,
                withAlpha(secondary, 90));
        g.drawString(font, panelW < 420 ? "CONTROL" : "VISUAL CONTROL CENTER",
                panelX + 151, panelY + 16, MUTED, false);

        fillRounded(g, panelX + panelW - 76, panelY + 8, 64, 23,
                0xA412060A, 8);
        outlineRounded(g, panelX + panelW - 76, panelY + 8, 64, 23,
                withAlpha(primary, 125), 8);
        g.drawCenteredString(font, "ESC  ×", panelX + panelW - 44,
                panelY + 15, MUTED);
    }

    private void drawNavigation(GuiGraphics g, int mouseX, int mouseY) {
        int y = panelY + 40;
        g.fill(panelX, y, panelX + panelW, y + 30, 0x80100C17);
        float navScale = Math.min(1.0F, (panelW - 20) / 384.0F);
        int x = panelX + 10;
        for (Tab item : Tab.values()) {
            int itemWidth = Math.max(26, Math.round(item.width * navScale));
            boolean selected = item == tab;
            boolean hovered = inside(mouseX, mouseY, x, y + 3, itemWidth - 3, 24);
            float active = RuffyAnimation.animate("gui.tab." + item.name(), selected ? 1.0F : 0.0F);
            float hover = RuffyAnimation.animate("gui.tab.hover." + item.name(), hovered ? 1.0F : 0.0F);

            int background = RuffyAnimation.blendColor(
                    withAlpha(0xFF2A1119, 0), withAlpha(RuffyConfig.primaryColor(), 48), active);
            background = RuffyAnimation.blendColor(background, 0x6C2A1119, hover * (1.0F - active));
            if (active > 0.001F || hover > 0.001F) {
                fillRounded(g, x, y + 3, itemWidth - 3, 24, background, 8);
                outlineRounded(g, x, y + 3, itemWidth - 3, 24,
                        withAlpha(RuffyConfig.primaryColor(), Math.round(95 * active + 35 * hover)), 8);
            }

            int underlineWidth = Math.max(0, Math.round((itemWidth - 15) * active));
            if (underlineWidth > 0) {
                g.fill(x + 6, y + 25, x + 6 + underlineWidth, y + 27,
                        RuffyConfig.primaryColor());
            }
            int iconColor = RuffyAnimation.blendColor(DIM, RuffyConfig.primaryColor(), active);
            int labelColor = RuffyAnimation.blendColor(MUTED, WHITE, active);
            labelColor = RuffyAnimation.blendColor(labelColor, WHITE, hover * 0.25F);
            g.drawString(font, tabIcon(item), x + 7, y + 10, iconColor, false);
            g.drawString(font, item.label, x + 22, y + 10, labelColor, false);
            x += itemWidth;
        }
        g.fill(panelX, y + 29, panelX + panelW, y + 30, BORDER);
    }

    private String tabIcon(Tab item) {
        return switch (item) {
            case VISUALS -> "✧";
            case PRESETS -> "▱";
            case WORLD -> "◇";
            case HUD -> "▣";
            case STYLE -> "◈";
            case CONFIGS -> "⚙";
            case SETUP -> "⌘";
        };
    }

    private void drawModuleBrowser(GuiGraphics g, int mouseX, int mouseY,
                                   RuffyFeature.Section requiredSection) {
        drawFilterBar(g, mouseX, mouseY, requiredSection);

        List<RuffyFeature> features = filteredFeatures(requiredSection);
        int pages = Math.max(1, (features.size() + 7) / 8);
        modulePage = Math.max(0, Math.min(modulePage, pages - 1));

        int rowW = leftW;
        for (int i = 0; i < 8; i++) {
            int index = modulePage * 8 + i;
            if (index >= features.size()) break;
            RuffyFeature feature = features.get(index);
            int y = listTop + i * rowHeight;
            drawModuleRow(g, mouseX, mouseY, feature, leftX, y, rowW, rowHeight - 2);
        }

        drawModuleFooter(g, features.size(), pages);
        drawInspector(g, mouseX, mouseY);
    }

    private void drawFilterBar(GuiGraphics g, int mouseX, int mouseY,
                               RuffyFeature.Section requiredSection) {
        int y = panelY + 77;
        boolean showFilters = requiredSection == null;
        if (showFilters) {
            int x = leftX;
            for (Filter item : Filter.values()) {
                boolean selected = item == filter;
                boolean hovered = inside(mouseX, mouseY, x, y, item.width, 22);
                if (selected || hovered) {
                    fillRounded(g, x, y, item.width, 22,
                            selected ? withAlpha(RuffyConfig.primaryColor(), 58) : 0x5C2A1018, 7);
                    if (selected) {
                        outlineRounded(g, x, y, item.width, 22,
                                withAlpha(RuffyConfig.primaryColor(), 130), 7);
                        g.fill(x + 3, y + 20, x + item.width - 3, y + 22,
                                RuffyConfig.primaryColor());
                    }
                }
                g.drawCenteredString(font, item.label, x + item.width / 2,
                        y + 7, selected ? WHITE : MUTED);
                x += item.width + 2;
            }

            if (searchBox != null) {
                fillRounded(g, searchX - 4, searchY + 1, searchW + 8, 22,
                        0xB012060A, 9);
                outlineRounded(g, searchX - 4, searchY + 1, searchW + 8, 22,
                        withAlpha(RuffyConfig.primaryColor(), 95), 9);
                g.drawString(font, "⌕", searchX + 4, searchY + 7, DIM, false);
                searchBox.setVisible(true);
            }
        } else {
            String title = requiredSection == RuffyFeature.Section.WORLD ? "WORLD MODULES" : "HUD MODULES";
            g.drawString(font, title, leftX + 2, y + 7, RuffyConfig.primaryColor(), false);
            if (searchBox != null) searchBox.setVisible(false);
        }
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

    private void drawModuleRow(GuiGraphics g, int mouseX, int mouseY,
                               RuffyFeature feature, int x, int y, int w, int h) {
        boolean selected = feature.key().equals(selectedKey);
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        boolean enabled = RuffyConfig.get(feature.key());
        boolean favorite = RuffyConfig.get("favorite_" + feature.key());
        int primary = RuffyConfig.primaryColor();

        float selectedMotion = RuffyAnimation.animate(
                "gui.row.selected." + feature.key(), selected ? 1.0F : 0.0F);
        float hoverMotion = RuffyAnimation.animate(
                "gui.row.hover." + feature.key(), hovered ? 1.0F : 0.0F);
        int rowFill = RuffyAnimation.blendColor(CARD, CARD_HOVER, hoverMotion);
        rowFill = RuffyAnimation.blendColor(rowFill, 0xD02B111A, selectedMotion);
        int rowBorder = RuffyAnimation.blendColor(BORDER, withAlpha(primary, 95), hoverMotion);
        rowBorder = RuffyAnimation.blendColor(rowBorder, withAlpha(primary, 220), selectedMotion);
        fillRounded(g, x, y, w, h, rowFill, 7);
        outlineRounded(g, x, y, w, h, rowBorder, 7);
        float enabledMotion = RuffyAnimation.animate(
                "gui.row.enabled." + feature.key(), enabled ? 1.0F : 0.0F);
        if (selectedMotion > 0.001F || enabledMotion > 0.001F) {
            g.fill(x, y + 4, x + 2, y + h - 4,
                    RuffyAnimation.blendColor(withAlpha(primary, 0), primary,
                            Math.max(selectedMotion, enabledMotion)));
        }

        int iconX = x + 7;
        int iconY = y + Math.max(2, (h - 16) / 2);
        fillRounded(g, iconX, iconY, 16, 16,
                selected || enabled ? withAlpha(primary, 40) : 0x8A27121A, 5);
        outlineRounded(g, iconX, iconY, 16, 16,
                withAlpha(enabled ? primary : 0xFF8B8193, enabled ? 125 : 65), 5);
        g.drawCenteredString(font, iconFor(feature), iconX + 8, iconY + 4,
                enabled ? primary : DIM);
        g.drawString(font, feature.title(), x + 29, y + (h - 8) / 2,
                selected ? WHITE : 0xFFE7E0EB, false);

        // Row actions mirror the reference: favorite, settings/edit, then active switch.
        int starX = x + w - 78;
        int editX = x + w - 55;
        boolean starHover = inside(mouseX, mouseY, starX - 2, y + 2, 18, h - 4);
        boolean editHover = inside(mouseX, mouseY, editX - 2, y + 2, 18, h - 4);
        float starMotion = RuffyAnimation.animate("gui.row.star." + feature.key(), starHover ? 1.0F : 0.0F);
        float editMotion = RuffyAnimation.animate("gui.row.edit." + feature.key(), editHover ? 1.0F : 0.0F);
        if (starMotion > 0.01F) {
            fillRounded(g, starX - 2, y + 2, 18, h - 4,
                    withAlpha(primary, Math.round(38 * starMotion)), 6);
        }
        if (editMotion > 0.01F) {
            fillRounded(g, editX - 2, y + 2, 18, h - 4,
                    withAlpha(primary, Math.round(38 * editMotion)), 6);
        }
        g.drawCenteredString(font, favorite ? "★" : "☆", starX + 6,
                y + (h - 8) / 2, favorite ? 0xFFFFD47A
                        : RuffyAnimation.blendColor(DIM, primary, starMotion));
        g.drawCenteredString(font, editHover ? "⚙" : "✎", editX + 6,
                y + (h - 8) / 2, RuffyAnimation.blendColor(MUTED, primary, editMotion));
        drawSwitch(g, x + w - 30, y + (h - 15) / 2, enabled, 25, 15,
                "module." + feature.key());
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
        int y = panelY + panelH - 32;
        g.drawString(font, count + " MODULES · " + enabled + " ON",
                leftX + 2, y + 8, MUTED, false);

        int mid = leftX + leftW - 78;
        drawSmallButton(g, mid, y, 20, 20, "‹", modulePage > 0);
        g.drawCenteredString(font, (modulePage + 1) + "/" + pages,
                mid + 48, y + 7, WHITE);
        drawSmallButton(g, mid + 76, y, 20, 20, "›", modulePage < pages - 1);
    }

    private void drawInspector(GuiGraphics g, int mouseX, int mouseY) {
        int x = rightX;
        int y = listTop;
        int w = rightW;
        int h = panelY + panelH - 38 - y;
        fillRounded(g, x, y, w, h, 0x7A10080D, 8);
        outlineRounded(g, x, y, w, h, BORDER, 8);
        fillRounded(g, x + 3, y + 2, Math.max(0, w - 6), 2,
                withAlpha(RuffyConfig.primaryColor(), 120), 1);

        RuffyFeature selected = RuffyFeature.ALL.stream()
                .filter(feature -> feature.key().equals(selectedKey)).findFirst().orElse(null);
        if (selected == null) {
            g.drawCenteredString(font, "SELECT A MODULE", x + w / 2,
                    y + h / 2, MUTED);
            return;
        }

        int pad = 11;
        int primary = RuffyConfig.primaryColor();
        boolean compact = h < 180;
        g.drawString(font, "MODULE SETTINGS", x + pad, y + 13, DIM, false);
        fillRounded(g, x + pad, y + 32, 27, 27, withAlpha(primary, 35), 8);
        outlineRounded(g, x + pad, y + 32, 27, 27, withAlpha(primary, 140), 8);
        g.drawCenteredString(font, iconFor(selected), x + pad + 13, y + 41, primary);
        g.drawString(font, selected.title(), x + pad + 34, y + 35, WHITE, false);
        g.drawString(font, selected.section().title().toUpperCase(Locale.ROOT),
                x + pad + 34, y + 48, primary, false);

        drawWrapped(g, selected.description(), x + pad, y + 69,
                w - pad * 2, MUTED, 11);

        int toggleY = y + (compact ? 86 : 104);
        fillRounded(g, x + pad, toggleY, w - pad * 2, 39, 0x641E0D14, 8);
        outlineRounded(g, x + pad, toggleY, w - pad * 2, 39, BORDER, 8);
        g.drawString(font, "MODULE STATE", x + pad + 8, toggleY + 7, MUTED, false);
        boolean active = RuffyConfig.get(selected.key());
        g.drawString(font, active ? "ENABLED" : "DISABLED",
                x + pad + 8, toggleY + 21, active ? primary : DIM, false);
        drawSwitch(g, x + w - pad - 38, toggleY + 11, active, 29, 16,
                "inspector." + selected.key());

        int detailY = toggleY + 49;
        if (compact) {
            g.drawString(font, "CLIENT-SIDE VISUAL", x + pad, detailY, DIM, false);
            g.drawString(font, "STYLE TAB: COLORS / OPACITY", x + pad,
                    detailY + 13, DIM, false);
        } else {
            g.drawString(font, "CLIENT-SIDE VISUAL", x + pad, detailY, DIM, false);
            if (selected.section() == RuffyFeature.Section.HUD) {
                int sliderY = detailY + 27;
                g.drawString(font, "HUD OPACITY  " + RuffyConfig.getInt("hud_opacity") + "%",
                        x + pad, sliderY, MUTED, false);
                drawSlider(g, x + pad, sliderY + 16, w - pad * 2,
                        RuffyConfig.getInt("hud_opacity"), 12, 90);
            } else {
                g.drawString(font, "NO GAMEPLAY MODIFICATION", x + pad,
                        detailY + 25, DIM, false);
                g.drawString(font, "RIGHT SHIFT TO CLOSE", x + pad,
                        detailY + 40, DIM, false);
            }
        }
    }

    private void drawPresets(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX + 18;
        int y = panelY + 82;
        g.drawString(font, "QUICK PROFILES", x, y, WHITE, false);
        String[] names = {"CRIMSON", "CINEMATIC", "PERFORMANCE", "CLEAN"};
        String[] desc = {"Crimson glow and trails", "Soft pink glow and trails",
                "Reduced particle load", "Minimal overlay"};
        int[] palettes = {0, 3, 4, 9};
        int w = (panelW - 50) / 2;
        for (int i = 0; i < 4; i++) {
            int cx = x + (i % 2) * (w + 10);
            int cy = y + 24 + (i / 2) * 84;
            int c = palettePrimary(palettes[i]);
            boolean hover = inside(mouseX, mouseY, cx, cy, w, 74);
            fillRounded(g, cx, cy, w, 74, hover ? CARD_HOVER : CARD, 9);
            outlineRounded(g, cx, cy, w, 74, hover ? withAlpha(c, 180) : BORDER, 9);
            fillRounded(g, cx + 3, cy + 1, w - 6, 2, c, 1);
            g.drawString(font, names[i], cx + 10, cy + 13, WHITE, false);
            g.drawString(font, desc[i], cx + 10, cy + 30, MUTED, false);
            g.drawString(font, "APPLY  →", cx + 10, cy + 51, c, false);
        }
    }

    private void drawStyle(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX + 18;
        int y = panelY + 72;
        g.drawString(font, "ACCENT PALETTE", x, y, WHITE, false);
        int sw = (panelW - 64) / 5;
        for (int i = 0; i < PALETTE_NAMES.length; i++) {
            int cx = x + (i % 5) * (sw + 6);
            int cy = y + 18 + (i / 5) * 32;
            int primary = palettePrimary(i);
            boolean selected = RuffyConfig.getInt("palette") == i;
            boolean hover = inside(mouseX, mouseY, cx, cy, sw, 27);
            float active = RuffyAnimation.animate("style.palette." + i, selected ? 1.0F : 0.0F);
            float hovered = RuffyAnimation.animate("style.palette.hover." + i, hover ? 1.0F : 0.0F);
            int fill = RuffyAnimation.blendColor(CARD, CARD_HOVER, hovered);
            fill = RuffyAnimation.blendColor(fill, withAlpha(primary, 48), active);
            fillRounded(g, cx, cy, sw, 27, fill, 7);
            int outline = RuffyAnimation.blendColor(BORDER, withAlpha(primary, 220), active);
            outlineRounded(g, cx, cy, sw, 27, outline, 7);
            fillRounded(g, cx + 6, cy + 6, 10, 10, primary, 5);
            fillRounded(g, cx + 19, cy + 6, 10, 10, paletteSecondary(i), 5);
            g.drawString(font, PALETTE_NAMES[i],
                    cx + 34, cy + 9, RuffyAnimation.blendColor(MUTED, WHITE, active), false);
        }

        int sliderY = y + 83;
        g.drawString(font, "HUD OPACITY  " + RuffyConfig.getInt("hud_opacity") + "%",
                x, sliderY, MUTED, false);
        drawSlider(g, x, sliderY + 10, panelW - 42,
                RuffyConfig.getInt("hud_opacity"), 12, 90);

        int menuY = sliderY + 22;
        g.drawString(font, "MENU OPACITY  " + RuffyConfig.getInt("menu_opacity") + "%",
                x, menuY, MUTED, false);
        drawSlider(g, x, menuY + 10, panelW - 42,
                RuffyConfig.getInt("menu_opacity"), 60, 100);

        int qualityY = menuY + 22;
        g.drawString(font, "EFFECT QUALITY  " + qualityName(RuffyConfig.getInt("effect_quality")),
                x, qualityY, MUTED, false);
        drawSlider(g, x, qualityY + 10, panelW - 42,
                RuffyConfig.getInt("effect_quality"), 0, 2);

        int smoothY = qualityY + 29;
        boolean smoothEnabled = RuffyConfig.get("smooth_animations");
        g.drawString(font, "SMOOTH ANIMATIONS", x, smoothY, WHITE, false);
        g.drawString(font, smoothEnabled ? "ON" : "OFF",
                x + panelW - 87, smoothY, smoothEnabled ? RuffyConfig.primaryColor() : DIM, false);
        drawSwitch(g, x + panelW - 49, smoothY - 1,
                smoothEnabled, 31, 16, "style.global_smooth");

        int strengthY = smoothY + 18;
        g.drawString(font, "SMOOTHNESS  " + RuffyConfig.getInt("animation_smoothness") + "%",
                x, strengthY, MUTED, false);
        drawSlider(g, x, strengthY + 10, panelW - 42,
                RuffyConfig.getInt("animation_smoothness"), 0, 100);
    }

    private void drawHudSettings(GuiGraphics g, int x, int y, int w) {
        g.drawString(font, "TRANSPARENCY", x, y, MUTED, false);
        drawSlider(g, x, y + 14, w, RuffyConfig.getInt("hud_opacity"), 12, 90);
    }

    private void drawConfigs(GuiGraphics g, int mouseX, int mouseY) {
        int x = panelX + 18;
        int y = panelY + 76;
        g.drawString(font, "CONFIGURATION", x, y, WHITE, false);
        g.drawString(font, "Options are saved automatically.", x, y + 16, MUTED, false);
        drawActionCard(g, mouseX, mouseY, x, y + 38, panelW - 36, 44,
                "RESET MODULES", "Restore default module states");
        drawActionCard(g, mouseX, mouseY, x, y + 89, panelW - 36, 44,
                "RESET STYLE", "Ten palettes and rounded neon controls");
        drawActionCard(g, mouseX, mouseY, x, y + 140, panelW - 36, 44,
                "RESET EVERYTHING", "Restore all client options");
    }

    private void drawSetup(GuiGraphics g) {
        int x = panelX + 18;
        int y = panelY + 86;
        g.drawString(font, "CONTROLS", x, y, WHITE, false);
        drawInfoRow(g, x, y + 24, "RIGHT SHIFT", "Open or close this menu");
        drawInfoRow(g, x, y + 56, "LEFT CLICK", "Toggle modules, favorites and settings");
        drawInfoRow(g, x, y + 88, "ESC", "Close the menu");
        drawInfoRow(g, x, y + 120, "CONFIG", "Saved in config/ruffyclient.properties");
        drawInfoRow(g, x, y + 152, "VERSION", "Minecraft 1.21.11 · Fabric · Java 21");
    }

    private void drawActionCard(GuiGraphics g, int mouseX, int mouseY,
                                int x, int y, int w, int h, String title, String subtitle) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        float hover = RuffyAnimation.animate("gui.action." + title, hovered ? 1.0F : 0.0F);
        int fill = RuffyAnimation.blendColor(CARD, CARD_HOVER, hover);
        fillRounded(g, x, y, w, h, fill, 9);
        outlineRounded(g, x, y, w, h,
                RuffyAnimation.blendColor(BORDER, withAlpha(RuffyConfig.primaryColor(), 185), hover), 9);
        fillRounded(g, x + 1, y + 5, 3, h - 10, RuffyConfig.primaryColor(), 2);
        g.drawString(font, title, x + 12, y + 8, WHITE, false);
        g.drawString(font, subtitle, x + 12, y + 25, MUTED, false);
    }

    private void drawInfoRow(GuiGraphics g, int x, int y, String left, String right) {
        fillRounded(g, x, y, panelW - 36, 25, 0x571A0D13, 7);
        outlineRounded(g, x, y, panelW - 36, 25, BORDER, 7);
        g.drawString(font, left, x + 8, y + 8, RuffyConfig.primaryColor(), false);
        g.drawString(font, right, x + 112, y + 8, MUTED, false);
    }

    private void drawSlider(GuiGraphics g, int x, int y, int w, int value, int min, int max) {
        int barY = y + 5;
        float ratio = (value - min) / (float) (max - min);
        float animated = RuffyAnimation.animate("gui.slider." + x + "." + y, ratio);
        int fill = Math.max(0, Math.min(w, Math.round(animated * w)));
        fillRounded(g, x, barY, w, 4, 0xFF39232D, 2);
        fillRounded(g, x, barY, Math.max(0, fill), 4, RuffyConfig.primaryColor(), 2);
        fillRounded(g, x + fill - 3, barY - 3, 7, 10, WHITE, 4);
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean enabled,
                            int w, int h, String animationKey) {
        float progress = RuffyAnimation.animate("gui.switch." + animationKey,
                enabled ? 1.0F : 0.0F);
        int track = RuffyAnimation.blendColor(0xFF34313B,
                withAlpha(RuffyConfig.primaryColor(), 218), progress);
        int outline = RuffyAnimation.blendColor(0xFF625B68,
                withAlpha(RuffyConfig.secondaryColor(), 225), progress);
        int knobColor = RuffyAnimation.blendColor(0xFFAAA3B0, WHITE, progress);
        if (progress > 0.01F) {
            fillRounded(g, x - 4, y - 3, w + 8, h + 6,
                    withAlpha(RuffyConfig.primaryColor(), Math.round(38 * progress)), h / 2 + 3);
        }
        fillRounded(g, x, y, w, h, track, h / 2);
        outlineRounded(g, x, y, w, h, outline, h / 2);
        int knob = Math.max(7, h - 6);
        int travel = Math.max(0, w - knob - 6);
        int knobX = x + 3 + Math.round(travel * progress);
        fillRounded(g, knobX, y + 3, knob, h - 6, knobColor, (h - 6) / 2);
    }

    private void drawSmallButton(GuiGraphics g, int x, int y, int w, int h,
                                 String label, boolean active) {
        fillRounded(g, x, y, w, h, active
                ? withAlpha(RuffyConfig.primaryColor(), 65) : 0x5520141A, Math.min(8, h / 2));
        outlineRounded(g, x, y, w, h,
                active ? withAlpha(RuffyConfig.primaryColor(), 190) : BORDER, Math.min(8, h / 2));
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, active ? WHITE : DIM);
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
        g.fill(panelX, y, panelX + 88, y + 1, RuffyConfig.primaryColor());
        g.drawString(font, "RUFFY VISUALS", panelX + 13,
                panelY + panelH - 17, DIM, false);
        g.drawString(font, "RIGHT SHIFT", panelX + panelW - 75,
                panelY + panelH - 17, DIM, false);
    }

    private static String qualityName(int quality) {
        return switch (quality) {
            case 0 -> "LOW";
            case 1 -> "MEDIUM";
            default -> "HIGH";
        };
    }

    private static int palettePrimary(int palette) {
        int[] colors = {0xFFFF365B, 0xFFFF4D8D, 0xFFEC4CFF, 0xFF995CFF,
                0xFF38BDF8, 0xFF7DD3FC, 0xFF41D9B0, 0xFF45C779,
                0xFFFFB547, 0xFFFFE16A};
        return colors[Math.max(0, Math.min(colors.length - 1, palette))];
    }

    private static int paletteSecondary(int palette) {
        int[] colors = {0xFF9E1837, 0xFFA92254, 0xFF7A2FA3, 0xFF4E2A9C,
                0xFF17678C, 0xFF326A90, 0xFF1D826A, 0xFF21764B,
                0xFFA7651E, 0xFFA98A2E};
        return colors[Math.max(0, Math.min(colors.length - 1, palette))];
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
        applyFrameLayout();
        int mx = (int) event.x();
        int my = (int) event.y();

        if (handleNavigation(mx, my)) return true;

        if (tab == Tab.VISUALS || tab == Tab.WORLD || tab == Tab.HUD) {
            if (tab == Tab.VISUALS && handleFilters(mx, my)) return true;
            if (handleModuleListClick(mx, my)) return true;
            if (handleInspectorClick(mx, my)) return true;
            if (handlePagination(mx, my)) return true;
        }

        if (tab == Tab.PRESETS && handlePresetClick(mx, my)) return true;
        if (tab == Tab.STYLE && handleStyleClick(mx, my)) return true;
        if (tab == Tab.CONFIGS && handleConfigClick(mx, my)) return true;
        return true;
    }

    private boolean handleNavigation(int mx, int my) {
        int y = panelY + 40;
        if (my < y + 3 || my > y + 27) return false;
        float navScale = Math.min(1.0F, (panelW - 20) / 384.0F);
        int x = panelX + 10;
        for (Tab item : Tab.values()) {
            int itemWidth = Math.max(26, Math.round(item.width * navScale));
            if (inside(mx, my, x, y + 3, itemWidth - 3, 24)) {
                tab = item;
                modulePage = 0;
                if (searchBox != null) searchBox.setVisible(tab == Tab.VISUALS);
                return true;
            }
            x += itemWidth;
        }
        return false;
    }

    private boolean handleFilters(int mx, int my) {
        int y = panelY + 77;
        int x = leftX;
        for (Filter item : Filter.values()) {
            if (inside(mx, my, x, y, item.width, 22)) {
                filter = item;
                modulePage = 0;
                return true;
            }
            x += item.width + 2;
        }
        return false;
    }

    private boolean handleModuleListClick(int mx, int my) {
        RuffyFeature.Section section = tab == Tab.HUD ? RuffyFeature.Section.HUD
                : tab == Tab.WORLD ? RuffyFeature.Section.WORLD : null;
        List<RuffyFeature> features = filteredFeatures(section);

        for (int i = 0; i < 8; i++) {
            int index = modulePage * 8 + i;
            if (index >= features.size()) break;
            RuffyFeature feature = features.get(index);
            int y = listTop + i * rowHeight;
            if (!inside(mx, my, leftX, y, leftW, rowHeight - 2)) continue;

            if (mx >= leftX + leftW - 80 && mx < leftX + leftW - 62) {
                String key = "favorite_" + feature.key();
                RuffyConfig.set(key, !RuffyConfig.get(key));
                return true;
            }
            if (mx >= leftX + leftW - 58 && mx < leftX + leftW - 34) {
                selectedKey = feature.key();
                return true;
            }
            if (mx >= leftX + leftW - 32) {
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
        int y = listTop;
        RuffyFeature selected = RuffyFeature.ALL.stream()
                .filter(feature -> feature.key().equals(selectedKey)).findFirst().orElse(null);
        if (selected == null) return false;

        int inspectorHeight = panelY + panelH - 38 - y;
        boolean compact = inspectorHeight < 180;
        int toggleY = y + (compact ? 86 : 104);
        if (inside(mx, my, x + 8, toggleY, rightW - 16, 39)) {
            RuffyConfig.set(selected.key(), !RuffyConfig.get(selected.key()));
            return true;
        }

        if (!compact && selected.section() == RuffyFeature.Section.HUD) {
            int sliderY = toggleY + 49 + 27 + 16;
            if (inside(mx, my, x + 10, sliderY, rightW - 20, 16)) {
                int value = 12 + Math.round((mx - (x + 10)) * 78.0F / Math.max(1, rightW - 20));
                RuffyConfig.setInt("hud_opacity", value);
                return true;
            }
        }
        return false;
    }

    private boolean handlePagination(int mx, int my) {
        int y = panelY + panelH - 32;
        int mid = leftX + leftW - 78;
        if (!inside(mx, my, mid, y, 96, 20)) return false;
        List<RuffyFeature> features = filteredFeatures(
                tab == Tab.HUD ? RuffyFeature.Section.HUD
                        : tab == Tab.WORLD ? RuffyFeature.Section.WORLD : null
        );
        int pages = Math.max(1, (features.size() + 7) / 8);
        if (mx < mid + 22) modulePage = Math.max(0, modulePage - 1);
        else if (mx > mid + 74) modulePage = Math.min(pages - 1, modulePage + 1);
        return true;
    }

    private boolean handlePresetClick(int mx, int my) {
        int x = panelX + 18;
        int y = panelY + 106;
        int w = (panelW - 50) / 2;
        for (int i = 0; i < 4; i++) {
            int cx = x + (i % 2) * (w + 10);
            int cy = y + (i / 2) * 84;
            if (inside(mx, my, cx, cy, w, 74)) {
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
        RuffyConfig.setInt("effect_quality", preset == 2 || preset == 3 ? 0 : 2);

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
        RuffyConfig.set("palette_sparks", preset != 3);
    }

    private boolean handleStyleClick(int mx, int my) {
        int x = panelX + 18;
        int y = panelY + 72;
        int sw = (panelW - 64) / 5;
        for (int i = 0; i < PALETTE_NAMES.length; i++) {
            int cx = x + (i % 5) * (sw + 6);
            int cy = y + 18 + (i / 5) * 32;
            if (inside(mx, my, cx, cy, sw, 27)) {
                RuffyConfig.setInt("palette", i);
                return true;
            }
        }

        int sliderY = y + 83;
        if (inside(mx, my, x, sliderY + 7, panelW - 42, 17)) {
            RuffyConfig.setInt("hud_opacity",
                    12 + Math.round((mx - x) * 78.0F / (panelW - 42)));
            return true;
        }

        int menuY = sliderY + 22;
        if (inside(mx, my, x, menuY + 7, panelW - 42, 17)) {
            RuffyConfig.setInt("menu_opacity",
                    60 + Math.round((mx - x) * 40.0F / (panelW - 42)));
            return true;
        }

        int qualityY = menuY + 22;
        if (inside(mx, my, x, qualityY + 7, panelW - 42, 17)) {
            RuffyConfig.setInt("effect_quality",
                    Math.round((mx - x) * 2.0F / (panelW - 42)));
            return true;
        }

        int smoothY = qualityY + 29;
        if (inside(mx, my, x + panelW - 55, smoothY - 3, 55, 22)) {
            RuffyConfig.set("smooth_animations", !RuffyConfig.get("smooth_animations"));
            return true;
        }

        int strengthY = smoothY + 18;
        if (inside(mx, my, x, strengthY + 7, panelW - 42, 17)) {
            RuffyConfig.setInt("animation_smoothness",
                    Math.round((mx - x) * 100.0F / (panelW - 42)));
            return true;
        }
        return false;
    }

    private boolean handleConfigClick(int mx, int my) {
        int x = panelX + 18;
        int y = panelY + 114;
        if (inside(mx, my, x, y, panelW - 36, 44)) {
            for (RuffyFeature feature : RuffyFeature.ALL) {
                RuffyConfig.set(feature.key(), defaultModuleState(feature.key()));
            }
            return true;
        }
        if (inside(mx, my, x, y + 51, panelW - 36, 44)) {
            RuffyConfig.setInt("palette", 0);
            RuffyConfig.setInt("hud_opacity", 44);
            RuffyConfig.setInt("menu_opacity", 94);
            RuffyConfig.setInt("effect_quality", 2);
            RuffyConfig.set("smooth_animations", true);
            RuffyConfig.setInt("animation_smoothness", 78);
            return true;
        }
        if (inside(mx, my, x, y + 102, panelW - 36, 44)) {
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
                    "damage_pulse", "underwater_bubbles", "palette_sparks", "health_hud",
                    "prism_core", "three_d_slash" -> true;
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
