package de.ruffy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ruffy Visuals 2.7 — Cosmic desktop studio with a non-game-space interface.
 *
 * The layout follows an app-shell model: persistent task navigation at the
 * left, one clear content workspace, compact two-column module cards and a
 * small number of distinct action styles. All interaction and drawing remain
 * client-side and use Ruffy's own rendering primitives.
 */
public final class RuffyClickGui extends Screen {
    private static final int WHITE = 0xFFF5F4FF;
    private static final int MUTED = 0xFFB6B4C9;
    private static final int DIM = 0xFF77778F;
    private static final int SURFACE = 0xE90C1020;
    private static final int SURFACE_SOFT = 0xA8151A2C;
    private static final int BORDER = 0x3C8790B8;
    private static final int CARD = 0xAA14192B;
    private static final int CARD_HOVER = 0xD31D2340;

    private enum Tab {
        HOME("Home", "Your Galaxy dashboard", "⌂"),
        VISUALS("Modules", "Visual effects and client modules", "✦"),
        WORLD("World", "Atmosphere and world-side effects", "◇"),
        HUD("HUD", "Your in-game information layout", "▣"),
        PRESETS("Presets", "Ready-made visual profiles", "▤"),
        COSMETICS("Cosmetics", "Auras, companions and movement trails", "✧"),
        STYLE("Visuals", "Colors, motion and interface tuning", "◈"),
        MUSIC("Spotify", "Music and lyrics", "♫"),
        CONFIGS("Settings", "Configuration and reset options", "⚙"),
        SETUP("About", "Ruffy Visuals information", "ⓘ");

        private final String label;
        private final String subtitle;
        private final String icon;

        Tab(String label, String subtitle, String icon) {
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon;
        }
    }

    private enum Filter {
        ALL("All", 38),
        ENTITIES("Entities", 62),
        MOTION("Motion", 54),
        WORLD("World", 50),
        STARRED("Favorites", 64);

        private final String label;
        private final int width;

        Filter(String label, int width) {
            this.label = label;
            this.width = width;
        }
    }

    private enum ButtonStyle {
        PRIMARY, TONAL, OUTLINE, GHOST, SEGMENT
    }

    private static final String[] PALETTE_NAMES = {
            "Nebula", "Rose", "Magenta", "Violet", "Azure",
            "Ice", "Mint", "Emerald", "Amber", "Solar"
    };

    private Tab tab = Tab.HOME;
    private Filter filter = Filter.ALL;
    private EditBox searchBox;
    private String searchText = "";
    private String selectedKey = "watermark";
    private int modulePage;

    private int panelX, panelY, panelW, panelH;
    private int sidebarX, sidebarY, sidebarW, sidebarH;
    private int mainX, mainW;
    private int searchX, searchY, searchW;
    private int filterY, listTop, rowHeight, cardGap, cardWidth;

    // Responsive dashboard regions: hero/player above three lower cards.
    private int homeCenterX, homeCenterW, homeRightX, homeRightW;
    private int homeHeroY, homeHeroH, homeModulesY, homeModulesH;
    private int homeSpotifyY, homeSpotifyH, homeAppearanceY, homeAppearanceH;
    private int homeQuickX, homeQuickW, homeLyricsX, homeLyricsW, homeModulesW;
    private int homeLyricsY, homeLyricsH;
    private int homeBottomY, homePerfW, homeVersionX, homeVersionW, homeSocialX;

    public RuffyClickGui() {
        super(Component.literal("Ruffy Visuals Control Center"));
    }

    @Override
    protected void init() {
        super.init();
        RuffyAnimation.set("gui.open", 0.0F);
        calculateLayout();

        searchBox = new EditBox(font, searchX + 26, searchY + 3,
                Math.max(72, searchW - 34), 18, Component.literal("Search modules"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(36);
        searchBox.setTextColor(0xFFF1F0FF);
        searchBox.setHint(Component.literal("Search modules"));
        searchBox.setValue(searchText);
        searchBox.setResponder(value -> {
            searchText = value.toLowerCase(Locale.ROOT);
            modulePage = 0;
            if (!value.isBlank() && !isModuleTab()) tab = Tab.VISUALS;
        });
        searchBox.setVisible(true);
        addRenderableWidget(searchBox);
    }

    private boolean isModuleTab() {
        return tab == Tab.VISUALS || tab == Tab.WORLD || tab == Tab.HUD;
    }

    private void calculateLayout() {
        // App-sized floating shell with breathing room around the window.
        panelW = Math.min(1180, Math.max(420, Math.round(width * 0.815F)));
        panelW = Math.min(panelW, width - 16);
        panelH = Math.min(680, Math.max(220, Math.round(height * 0.745F)));
        panelH = Math.min(panelH, height - 14);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2 - Math.round(height * 0.035F);
        panelY = Math.max(5, Math.min(height - panelH - 5, panelY));

        sidebarX = panelX + Math.max(3, Math.round(panelW * 0.006F));
        sidebarY = panelY + Math.round(panelH * 0.095F);
        sidebarW = Math.max(58, Math.min(170, Math.round(panelW * 0.145F)));
        sidebarH = panelH - Math.round(panelH * 0.12F);
        mainX = sidebarX + sidebarW + Math.max(5, Math.round(panelW * 0.012F));
        mainW = Math.max(170, panelX + panelW - Math.max(5, Math.round(panelW * 0.008F)) - mainX);

        searchW = Math.min(260, Math.max(90, Math.round(mainW * 0.28F)));
        searchX = mainX + Math.round((mainW - searchW) * 0.50F)
                + Math.round(mainW * 0.02F);
        searchY = panelY + Math.max(5, Math.round(panelH * 0.035F));

        filterY = panelY + Math.round(panelH * 0.17F);
        listTop = panelY + Math.round(panelH * 0.25F);
        cardGap = Math.max(5, Math.round(panelW * 0.008F));
        cardWidth = (mainW - cardGap) / 2;
        int available = Math.max(90, panelY + panelH - Math.round(panelH * 0.12F) - listTop);
        rowHeight = Math.max(28, Math.min(62, (available - cardGap * 3) / 4));

        if (searchBox != null) {
            searchBox.setPosition(searchX + 25, searchY + 3);
            searchBox.setWidth(Math.max(56, searchW - 33));
        }
    }

    private void applyFrameLayout() {
        calculateLayout();
        float opening = RuffyAnimation.animate("gui.open", 1.0F);
        int lift = Math.round((1.0F - opening) * 8.0F);
        panelY += lift;
        sidebarY += lift;
        searchY += lift;
        filterY += lift;
        listTop += lift;
        if (searchBox != null) searchBox.setPosition(searchX + 25, searchY + 3);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        applyFrameLayout();
        drawBackdrop(g);
        drawWindow(g);
        drawSidebar(g, mouseX, mouseY);
        drawHeader(g, mouseX, mouseY);

        switch (tab) {
            case HOME -> drawHomeDashboard(g, mouseX, mouseY);
            case VISUALS -> drawModuleBrowser(g, mouseX, mouseY, null);
            case WORLD -> drawModuleBrowser(g, mouseX, mouseY, RuffyFeature.Section.WORLD);
            case HUD -> drawModuleBrowser(g, mouseX, mouseY, RuffyFeature.Section.HUD);
            case PRESETS -> drawPresets(g, mouseX, mouseY);
            case COSMETICS -> drawCosmetics(g, mouseX, mouseY);
            case STYLE -> drawStyle(g, mouseX, mouseY);
            case CONFIGS -> drawConfigs(g, mouseX, mouseY);
            case SETUP -> drawSetup(g);
            case MUSIC -> drawMusicLanding(g, mouseX, mouseY);
        }

        drawFooter(g);
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawBackdrop(GuiGraphics g) {
        // Leave the live world visible around the floating application window,
        // exactly as in the in-game reference. Only a very light scrim is added.
        g.fill(0, 0, width, height, 0x24060918);

        double time = RuffyAnimation.timeSeconds();
        int glintCount = Math.max(0, 26 * RuffyConfig.getInt("particle_density") / 100);
        for (int i = 0; i < glintCount; i++) {
            int x = Math.floorMod(i * 173
                    + (int) (Math.sin(time * 0.045 + i) * 18.0), Math.max(1, width));
            int y = Math.floorMod(i * 97
                    + (int) (Math.cos(time * 0.038 + i * 0.7) * 12.0), Math.max(1, height));
            int alpha = Math.round(8 + RuffyAnimation.breathe(time, 0.06, i) * 18);
            fillRounded(g, x, y, i % 8 == 0 ? 2 : 1, i % 8 == 0 ? 2 : 1,
                    withAlpha(RuffyConfig.primaryColor(), alpha), 1);
        }
    }

    private void drawWindow(GuiGraphics g) {
        int accent = RuffyConfig.primaryColor();
        int bodyAlpha = 228 + RuffyConfig.getInt("menu_opacity") / 8;
        int body = (Math.min(250, bodyAlpha) << 24) | 0x00070A18;

        RuffyRoundedUi.shadow(g, panelX - 4, panelY - 2, panelW + 8, panelH + 8,
                RuffyRoundedUi.RADIUS_WINDOW, 64);
        fillRounded(g, panelX - 5, panelY - 5, panelW + 10, panelH + 10,
                withAlpha(accent, 22), RuffyRoundedUi.RADIUS_WINDOW);
        outlineRounded(g, panelX - 2, panelY - 2, panelW + 4, panelH + 4,
                withAlpha(accent, 80), RuffyRoundedUi.RADIUS_WINDOW);
        fillRounded(g, panelX, panelY, panelW, panelH, body,
                RuffyRoundedUi.RADIUS_WINDOW);
        outlineRounded(g, panelX, panelY, panelW, panelH,
                withAlpha(accent, 205), RuffyRoundedUi.RADIUS_WINDOW);
        outlineRounded(g, panelX + 2, panelY + 2, panelW - 4, panelH - 4,
                withAlpha(0xFFDFB4FF, 65), RuffyRoundedUi.RADIUS_WINDOW);
        fillRounded(g, panelX + 28, panelY + 1, panelW - 56, 1,
                withAlpha(0xFFFFFFFF, 38), 1);
    }

    private void drawSidebar(GuiGraphics g, int mouseX, int mouseY) {
        boolean compact = compactSidebar();
        RuffyRoundedUi.shadow(g, sidebarX, sidebarY, sidebarW, sidebarH,
                RuffyRoundedUi.RADIUS_PANEL, 28);
        fillRounded(g, sidebarX, sidebarY, sidebarW, sidebarH,
                0xEC080713, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, sidebarX, sidebarY, sidebarW, sidebarH,
                0x7D8C58D8, RuffyRoundedUi.RADIUS_PANEL);

        int logoSize = 54;
        int logoX = sidebarX + (sidebarW - logoSize) / 2;
        int logoY = sidebarY + 8;
        double logoTime = RuffyAnimation.timeSeconds();
        // Small orbiting glints create the luminous emblem seen in the reference.
        for (int i = 0; i < 44; i++) {
            double angle = Math.PI * 2.0 * i / 44.0 + logoTime * 0.10;
            int px = logoX + logoSize / 2 + (int) Math.round(Math.cos(angle) * 34);
            int py = logoY + logoSize / 2 + (int) Math.round(Math.sin(angle) * 19);
            int alpha = 105 + Math.round(100 * RuffyAnimation.breathe(logoTime, 0.12, i));
            fillRounded(g, px, py, i % 8 == 0 ? 3 : 2, i % 8 == 0 ? 3 : 2,
                    withAlpha(RuffyConfig.primaryColor(), alpha), 2);
        }
        fillRounded(g, logoX - 7, logoY - 7, 68, 68,
                withAlpha(RuffyConfig.primaryColor(), 34), RuffyRoundedUi.RADIUS_PANEL);
        fillRounded(g, logoX - 3, logoY - 3, 60, 60,
                withAlpha(RuffyConfig.secondaryColor(), 84), RuffyRoundedUi.RADIUS_PANEL);
        fillRounded(g, logoX, logoY, logoSize, logoSize,
                withAlpha(RuffyConfig.primaryColor(), 92), RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, logoX - 1, logoY - 1, logoSize + 2, logoSize + 2,
                withAlpha(RuffyConfig.primaryColor(), 245), RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, logoX + 3, logoY + 3, logoSize - 6, logoSize - 6,
                withAlpha(0xFFEACBFF, 95), RuffyRoundedUi.RADIUS_PANEL);
        g.drawCenteredString(font, "R", logoX + logoSize / 2, logoY + 17,
                0xFFF4DFFF);
        fillRounded(g, logoX + 9, logoY + 43, 36, 2,
                withAlpha(0xFFE8C9FF, 190), 1);

        int navX = sidebarX + Math.max(4, Math.round(sidebarW * 0.06F));
        int navW = sidebarW - Math.max(8, Math.round(sidebarW * 0.12F));
        int[] ys = navigationYPositions();
        int itemHeight = navigationItemHeight();
        Tab[] firstGroup = {Tab.HOME, Tab.VISUALS, Tab.STYLE, Tab.HUD};
        for (int i = 0; i < firstGroup.length; i++) {
            drawNavItem(g, firstGroup[i], navX, ys[i], navW, itemHeight, mouseX, mouseY);
        }

        drawNavItem(g, Tab.MUSIC, navX, ys[4], navW, itemHeight, mouseX, mouseY);
        drawNavItem(g, Tab.CONFIGS, navX, ys[5], navW, itemHeight, mouseX, mouseY);
        drawNavItem(g, Tab.SETUP, navX, ys[6], navW, itemHeight, mouseX, mouseY);

        if (compact) {
            int bottomY = sidebarY + sidebarH - 30;
            fillRounded(g, sidebarX + 6, bottomY, sidebarW - 12, 26,
                    0xB5111426, RuffyRoundedUi.RADIUS_CONTROL);
            outlineRounded(g, sidebarX + 6, bottomY, sidebarW - 12, 26,
                    withAlpha(RuffyConfig.primaryColor(), 98), RuffyRoundedUi.RADIUS_CONTROL);
            fillRounded(g, sidebarX + 12, bottomY + 7, 6, 6, 0xFF45DB91, 3);
            g.drawString(font, "ONLINE", sidebarX + 22, bottomY + 5, WHITE, false);
            g.drawString(font, "1.21.11", sidebarX + 12, bottomY + 15, DIM, false);
        } else {
            int bottomY = sidebarY + sidebarH - 54;
            fillRounded(g, sidebarX + 8, bottomY, navW, 46,
                    0xB5111426, RuffyRoundedUi.RADIUS_CONTROL);
            outlineRounded(g, sidebarX + 8, bottomY, navW, 46,
                    withAlpha(RuffyConfig.primaryColor(), 105), RuffyRoundedUi.RADIUS_CONTROL);
            fillRounded(g, sidebarX + 16, bottomY + 11, 7, 7,
                    0xFF45DB91, 4);
            g.drawString(font, "Online", sidebarX + 29,
                    bottomY + 8, WHITE, false);
            g.drawString(font, "1.21.11", sidebarX + 16,
                    bottomY + 25, DIM, false);
        }
    }

    private boolean compactSidebar() {
        return panelH < 450;
    }

    private int navigationItemHeight() {
        return compactSidebar() ? Math.max(16, Math.round(sidebarH * 0.09F)) : 24;
    }

    private int[] navigationYPositions() {
        int h = sidebarH;
        return new int[]{
                sidebarY + Math.round(h * 0.18F),
                sidebarY + Math.round(h * 0.29F),
                sidebarY + Math.round(h * 0.40F),
                sidebarY + Math.round(h * 0.49F),
                sidebarY + Math.round(h * 0.58F),
                sidebarY + Math.round(h * 0.67F),
                sidebarY + Math.round(h * 0.74F)
        };
    }

    private void drawSidebarLabel(GuiGraphics g, String value, int x, int y) {
        g.drawString(font, value, x, y, 0xFF777F98, false);
    }

    private void drawNavItem(GuiGraphics g, Tab item,
                             int x, int y, int w, int h,
                             int mouseX, int mouseY) {
        boolean selected = tab == item;
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        float active = RuffyAnimation.animate("gui.nav.active." + item.name(),
                selected ? 1.0F : 0.0F);
        float hover = RuffyAnimation.animate("gui.nav.hover." + item.name(),
                hovered ? 1.0F : 0.0F);

        int background = RuffyAnimation.blendColor(0x00000000,
                withAlpha(RuffyConfig.primaryColor(), 35), active);
        background = RuffyAnimation.blendColor(background, 0x3BFFFFFF,
                hover * (1.0F - active));
        if (active > 0.002F || hover > 0.002F) {
            fillRounded(g, x, y, w, h, background, RuffyRoundedUi.RADIUS_CONTROL);
        }
        if (active > 0.01F) {
            fillRounded(g, x + 1, y + 5, 2, h - 10,
                    withAlpha(RuffyConfig.primaryColor(), 235), 1);
        }

        int iconColor = RuffyAnimation.blendColor(DIM, RuffyConfig.primaryColor(), active);
        int textColor = RuffyAnimation.blendColor(MUTED, WHITE, Math.max(active, hover * 0.3F));
        g.drawString(font, item.icon, x + 9, y + 8, iconColor, false);
        g.drawString(font, item.label, x + 27, y + 8, textColor, false);
        if (item == Tab.MUSIC && RuffySpotify.isConnected()) {
            fillRounded(g, x + w - 12, y + 10, 4, 4, 0xFF5DD6A1, 2);
        }
    }

    private void drawHeader(GuiGraphics g, int mouseX, int mouseY) {
        if (tab == Tab.HOME) {
            int brandX = mainX + Math.max(8, Math.round(mainW * 0.055F));
            int brandY = panelY + Math.max(12, Math.round(panelH * 0.035F));
            g.drawString(font, "Ruffy", brandX, brandY, WHITE, false);
            g.drawString(font, "Visuals", brandX + font.width("Ruffy ") - 1,
                    brandY, RuffyConfig.primaryColor(), false);
            g.drawString(font, "GALAXY STUDIO  ·  2.7", brandX,
                    panelY + Math.max(36, Math.round(panelH * 0.092F)), MUTED, false);
        } else {
            String title = switch (tab) {
                case HOME -> "Home";
                case VISUALS -> "Modules";
                case WORLD -> "World effects";
                case HUD -> "HUD studio";
                case PRESETS -> "Visual profiles";
                case COSMETICS -> "Cosmetics";
                case STYLE -> "Visuals";
                case MUSIC -> "Spotify";
                case CONFIGS -> "Settings";
                case SETUP -> "About";
            };
            g.drawString(font, "RUFFY VISUALS  /  COSMIC STUDIO",
                    mainX, panelY + 15, DIM, false);
            g.drawString(font, title, mainX, panelY + 29, WHITE, false);
            g.drawString(font, tab.subtitle, mainX, panelY + 42, MUTED, false);
        }

        fillRounded(g, searchX - 2, searchY - 2, searchW + 4, 29,
                withAlpha(RuffyConfig.primaryColor(), 22), RuffyRoundedUi.RADIUS_CONTROL);
        fillRounded(g, searchX, searchY, searchW, 25,
                0xE40B0C1C, RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, searchX, searchY, searchW, 25,
                withAlpha(RuffyConfig.secondaryColor(), 155), RuffyRoundedUi.RADIUS_CONTROL);
        g.drawString(font, "⌕", searchX + 8, searchY + 8, 0xFFE8D9FF, false);
        if (searchBox != null) {
            searchBox.setVisible(true);
            searchBox.setPosition(searchX + 25, searchY + 3);
            searchBox.setWidth(Math.max(72, searchW - 33));
        }

        int controlsX = mainX + mainW - 92;
        int controlsY = panelY + Math.max(5, Math.round(panelH * 0.028F));
        for (int i = 0; i < 3; i++) {
            int cx = controlsX + i * 34;
            int controlColor = i == 2 ? 0xFFFF6487 : 0xFFBBA2F2;
            fillRounded(g, cx - 2, controlsY - 2, 30, 30,
                    withAlpha(controlColor, 22), 15);
            fillRounded(g, cx, controlsY, 26, 26, 0xE50C0B1A, 13);
            outlineRounded(g, cx, controlsY, 26, 26,
                    withAlpha(controlColor, i == 2 ? 215 : 145), 13);
        }
        g.drawCenteredString(font, "−", controlsX + 13, controlsY + 8, MUTED);
        g.drawCenteredString(font, "□", controlsX + 47, controlsY + 8, MUTED);
        g.drawCenteredString(font, "×", controlsX + 81, controlsY + 8, 0xFFFFA8BD);
    }


    private void prepareHomeLayout() {
        int gap = Math.max(5, Math.round(panelW * 0.009F));
        homeCenterX = mainX;
        homeCenterW = Math.max(100, (mainW - gap) * 67 / 100);
        homeRightX = homeCenterX + homeCenterW + gap;
        homeRightW = Math.max(70, mainX + mainW - homeRightX);

        homeHeroY = panelY + Math.round(panelH * 0.145F);
        homeHeroH = Math.max(58, Math.round(panelH * 0.355F));
        homeModulesY = homeHeroY + homeHeroH + Math.max(5, Math.round(panelH * 0.020F));
        homeBottomY = panelY + panelH - Math.round(panelH * 0.150F);
        homeModulesH = Math.max(48, homeBottomY - homeModulesY - Math.max(8, gap));

        homeSpotifyY = panelY + Math.round(panelH * 0.060F);
        homeSpotifyH = Math.round(panelH * 0.375F);
        homeLyricsY = homeSpotifyY + homeSpotifyH - Math.max(2, gap / 3);
        homeLyricsH = Math.max(25, Math.round(panelH * 0.112F));

        int lowerWidth = Math.max(140, mainW - gap);
        homeModulesW = lowerWidth * 65 / 100;
        homeQuickX = homeRightX;
        homeQuickW = homeRightW;
        homeLyricsX = homeRightX;
        homeLyricsW = homeRightW;
        homeAppearanceY = homeModulesY + Math.max(7, Math.round(homeModulesH * 0.13F));
        homeAppearanceH = Math.max(34, homeModulesH - Math.max(8, Math.round(homeModulesH * 0.09F)));

        homePerfW = Math.max(90, Math.min(300, mainW * 37 / 100));
        homeSocialX = mainX + mainW - 4 * 44 - 3 * 14;
        homeVersionX = mainX + homePerfW + gap;
        homeVersionW = Math.max(80, homeSocialX - homeVersionX - gap);
    }

    private void drawHomeDashboard(GuiGraphics g, int mouseX, int mouseY) {
        prepareHomeLayout();
        drawHomeHero(g, mouseX, mouseY);
        drawHomeModules(g, mouseX, mouseY);
        drawHomeSpotify(g, mouseX, mouseY);
        drawHomeLyrics(g, mouseX, mouseY);
        drawHomeAppearance(g, mouseX, mouseY);
        drawHomeBottom(g, mouseX, mouseY);
    }

    private void drawHomeHero(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeCenterX;
        int y = homeHeroY;
        int w = homeCenterW;
        int h = homeHeroH;

        RuffyRoundedUi.shadow(g, x, y, w, h, RuffyRoundedUi.RADIUS_PANEL, 32);
        fillRounded(g, x, y, w, h, 0xEB08051B, RuffyRoundedUi.RADIUS_PANEL);
        RuffyDashboardArt.drawHero(g, x + 1, y + 1, w - 2, h - 2);
        fillRounded(g, x + 1, y + 1, Math.max(80, w * 52 / 100), h - 2,
                0x8A100827, RuffyRoundedUi.RADIUS_PANEL);
        fillRounded(g, x + 7, y + h - 3, w - 14, 2,
                withAlpha(RuffyConfig.primaryColor(), 115), 1);
        outlineRounded(g, x, y, w, h,
                withAlpha(RuffyConfig.primaryColor(), 230), RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x + 2, y + 2, w - 4, h - 4,
                withAlpha(0xFFE8C9FF, 86), RuffyRoundedUi.RADIUS_PANEL);

        int textX = x + Math.max(10, w * 4 / 100);
        int titleY = y + Math.max(12, h * 23 / 100);
        g.drawString(font, "Welcome back!", textX, titleY, WHITE, false);
        g.drawString(font, "Your visual universe.", textX,
                titleY + Math.max(16, h * 18 / 100), MUTED, false);
        g.drawString(font, "Make it yours with Ruffy Visuals.", textX,
                titleY + Math.max(28, h * 31 / 100), 0xFFE8C9FF, false);

        int buttonH = Math.max(19, Math.min(28, h / 5));
        int buttonY = y + h - buttonH - Math.max(12, h * 11 / 100);
        int buttonW = Math.min(190, Math.max(90, w * 28 / 100));
        drawButton(g, textX, buttonY, buttonW, buttonH,
                "Explore Modules  →", ButtonStyle.PRIMARY,
                inside(mouseX, mouseY, textX, buttonY, buttonW, buttonH), false);
    }

    private void drawHomeModules(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeCenterX;
        int y = homeModulesY;
        int w = homeModulesW;
        int h = homeModulesH;
        RuffyRoundedUi.shadow(g, x, y, w, h, RuffyRoundedUi.RADIUS_PANEL, 28);
        fillRounded(g, x, y, w, h, 0xF008091B, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, withAlpha(RuffyConfig.secondaryColor(), 190),
                RuffyRoundedUi.RADIUS_PANEL);
        g.drawString(font, "✦  Modules", x + 12, y + 9, WHITE, false);
        g.drawString(font, "Manage your modules", x + 12, y + 22, MUTED, false);
        drawButton(g, x + w - 70, y + 7, 58, 20, "See all  →",
                ButtonStyle.GHOST,
                inside(mouseX, mouseY, x + w - 70, y + 7, 58, 20), false);

        String[] keys = {"prism_core", "nebula_particles", "orbit_particles", "swing_animation"};
        String[] titles = {"3D Prism", "Glints", "Orbit FX", "Animations"};
        String[] descriptions = {"Visual core", "Ambient light", "Orbit rings", "Smooth motion"};
        String[] icons = {"✥", "✧", "◉", "★"};
        int gap = Math.max(4, Math.round(w * 0.012F));
        int left = x + 10;
        int top = y + Math.max(16, Math.round(panelH * 0.100F));
        int tileW = Math.max(38, (w - 20 - gap * 3) / 4);
        int tileH = Math.max(24, h - (top - y) - Math.max(4, Math.round(panelH * 0.02F)));

        for (int i = 0; i < keys.length; i++) {
            int tx = left + i * (tileW + gap);
            int ty = top;
            boolean enabled = RuffyConfig.get(keys[i]);
            boolean hovered = inside(mouseX, mouseY, tx, ty, tileW, tileH);
            float hover = RuffyAnimation.animate("home.feature.hover." + keys[i],
                    hovered ? 1.0F : 0.0F);
            int background = RuffyAnimation.blendColor(0xD70B1022, 0xF12C1744, hover * 0.9F);
            RuffyRoundedUi.shadow(g, tx, ty, tileW, tileH, RuffyRoundedUi.RADIUS_CONTROL, 13);
            fillRounded(g, tx, ty, tileW, tileH, background, RuffyRoundedUi.RADIUS_CONTROL);
            outlineRounded(g, tx, ty, tileW, tileH,
                    withAlpha(enabled ? RuffyConfig.primaryColor() : 0xFF877AAB,
                            enabled ? 205 : 78), RuffyRoundedUi.RADIUS_CONTROL);

            int iconSize = Math.max(14, Math.min(22, tileW / 4));
            fillRounded(g, tx + 7, ty + 14, iconSize, iconSize,
                    withAlpha(RuffyConfig.primaryColor(), enabled ? 74 : 34),
                    RuffyRoundedUi.RADIUS_CONTROL);
            g.drawCenteredString(font, icons[i], tx + 7 + iconSize / 2, ty + 18,
                    enabled ? 0xFFF1DFFF : MUTED);

            int switchW = Math.max(18, Math.min(28, tileW / 4));
            int switchH = Math.max(11, Math.min(16, tileH / 4));
            int switchX = tx + tileW - switchW - 6;
            int switchY = ty + 22;
            int textX = tx + 7;
            int textW = Math.max(15, tileW - 14);
            int titleY = ty + Math.max(15, tileH * 43 / 100);
            g.drawString(font, trimToWidth(titles[i], textW), textX,
                    titleY, WHITE, false);
            if (tileH >= 52) {
                g.drawString(font, trimToWidth(descriptions[i], textW), textX,
                        ty + tileH - 28, MUTED, false);
            }
            drawSwitch(g, switchX, switchY, enabled, switchW, switchH,
                    "home.feature." + keys[i]);
        }
    }

    private void drawHomeSpotify(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeRightX;
        int y = homeSpotifyY;
        int w = homeRightW;
        int h = homeSpotifyH;
        boolean compact = h < 130;
        RuffyRoundedUi.shadow(g, x, y, w, h, RuffyRoundedUi.RADIUS_PANEL, 32);
        fillRounded(g, x, y, w, h, 0xF0080919, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, withAlpha(RuffyConfig.secondaryColor(), 215),
                RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x + 2, y + 2, w - 4, h - 4,
                withAlpha(RuffyConfig.primaryColor(), 32), RuffyRoundedUi.RADIUS_PANEL);

        int headerY = y + (compact ? 7 : 17);
        int spotifyMark = compact ? 22 : 25;
        fillRounded(g, x + 11, headerY, spotifyMark, spotifyMark,
                0xFF1ED760, spotifyMark / 2);
        g.drawCenteredString(font, "≋", x + 11 + spotifyMark / 2,
                headerY + Math.max(5, spotifyMark / 2 - 5), 0xFF092010);
        g.drawString(font, "Spotify", x + 43, headerY + 5, WHITE, false);

        boolean connected = RuffySpotify.isConnected();
        int statusColor = connected ? 0xFF42E69A : 0xFFFFC078;
        String statusLabel = connected ? "Connected" : "Connect";
        int gearX = x + w - 26;
        int statusX = Math.max(x + 105, gearX - font.width(statusLabel) - 22);
        fillRounded(g, statusX - 9, headerY + 9, 6, 6, statusColor, 3);
        g.drawString(font, statusLabel, statusX, headerY + 4, statusColor, false);
        drawIconButton(g, gearX, headerY + 1, 18, 20, "⚙", MUTED,
                inside(mouseX, mouseY, gearX, headerY + 1, 18, 20));

        int artSize = compact ? 40 : 56;
        int artX = x + 12;
        int artY = y + (compact ? 36 : 53);
        drawAlbumArt(g, artX, artY, artSize);

        int infoX = artX + artSize + 10;
        int infoW = Math.max(36, x + w - 12 - infoX);
        int textW = infoW;
        int visualX = infoX;
        int visualW = 0;
        if (RuffyConfig.get("music_visualizer")) {
            visualW = compact ? Math.max(28, infoW * 34 / 100)
                    : Math.min(78, Math.max(42, infoW * 47 / 100));
            visualX = infoX + infoW - visualW;
            textW = Math.max(36, infoW - visualW - 7);
        }

        g.drawString(font, trimToWidth(RuffySpotify.getTrackName(), textW),
                infoX, artY + 6, WHITE, false);
        String artist = RuffySpotify.getArtistName();
        if (!artist.isBlank()) {
            g.drawString(font, trimToWidth(artist, textW),
                    infoX, artY + 20, MUTED, false);
        } else if (!connected) {
            g.drawString(font, "NOT CONNECTED", infoX, artY + 20, DIM, false);
        } else if (!RuffySpotify.isPlaying()) {
            g.drawString(font, "NOTHING PLAYING", infoX, artY + 20, DIM, false);
        }

        if (visualW > 0) {
            drawVisualizerBars(g, visualX, artY + (compact ? 7 : 13), visualW);
        }

        int progressY = y + h - (compact ? 31 : 51);
        int progressX = x + 12;
        int progressW = w - 24;
        long duration = RuffySpotify.getDurationMs();
        long position = RuffySpotify.getProgressMs();
        float ratio = duration <= 0L ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, position / (float) duration));
        fillRounded(g, progressX, progressY, progressW, 3, 0xFF332C46, 2);
        fillRounded(g, progressX, progressY, Math.round(progressW * ratio), 3,
                RuffyConfig.primaryColor(), 2);
        if (!compact) {
            g.drawString(font, formatTime(position), progressX, progressY + 5, DIM, false);
            String total = formatTime(duration);
            g.drawString(font, total, progressX + progressW - font.width(total),
                    progressY + 5, DIM, false);
        }

        int controlsY = y + h - (compact ? 22 : 27);
        int center = x + w / 2;
        drawIconButton(g, center - 38, controlsY, 23, 21, "|◀", WHITE,
                inside(mouseX, mouseY, center - 38, controlsY, 23, 21));
        fillRounded(g, center - 13, controlsY - 3, 26, 26,
                withAlpha(RuffyConfig.primaryColor(), 38), 13);
        fillRounded(g, center - 10, controlsY, 20, 20,
                RuffyConfig.primaryColor(), 10);
        g.drawCenteredString(font, RuffySpotify.isPlaying() ? "Ⅱ" : "▶",
                center, controlsY + 6, WHITE);
        drawIconButton(g, center + 15, controlsY, 23, 21, "▶|", WHITE,
                inside(mouseX, mouseY, center + 15, controlsY, 23, 21));
    }
    private void drawVisualizerBars(GuiGraphics g, int x, int y, int width) {
        double time = RuffyAnimation.timeSeconds();
        int bars = Math.max(6, Math.min(18, width / 4));
        int gap = 2;
        int barW = Math.max(1, (width - (bars - 1) * gap) / bars);
        for (int i = 0; i < bars; i++) {
            float pulse = 0.25F + 0.75F * RuffyAnimation.breathe(time,
                    0.55 + (i % 4) * 0.12, i * 0.7F);
            int height = Math.max(3, Math.round(5 + pulse * 21));
            fillRounded(g, x + i * (barW + gap), y + 25 - height,
                    barW, height, withAlpha(RuffyConfig.primaryColor(),
                            160 + Math.round(pulse * 95)), 1);
        }
    }

    private void drawHomeAppearance(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeQuickX;
        int y = homeAppearanceY;
        int w = homeQuickW;
        int h = homeAppearanceH;
        RuffyRoundedUi.shadow(g, x, y, w, h, RuffyRoundedUi.RADIUS_PANEL, 30);
        fillRounded(g, x, y, w, h, 0xF008091B, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, withAlpha(RuffyConfig.secondaryColor(), 210),
                RuffyRoundedUi.RADIUS_PANEL);
        g.drawString(font, "◉  Appearance", x + 11, y + 7, WHITE, false);
        g.drawString(font, "Customize your look", x + 11, y + 20, MUTED, false);

        int thumbX = x + 10;
        int thumbY = y + 48;
        int thumbW = Math.min(84, Math.max(58, w * 33 / 100));
        int thumbH = Math.max(28, h - 56);
        fillRounded(g, thumbX - 2, thumbY - 2, thumbW + 4, thumbH + 4,
                withAlpha(RuffyConfig.primaryColor(), 34), RuffyRoundedUi.RADIUS_CONTROL);
        RuffyDashboardArt.drawHero(g, thumbX, thumbY, thumbW, thumbH);
        outlineRounded(g, thumbX, thumbY, thumbW, thumbH,
                withAlpha(RuffyConfig.primaryColor(), 155), RuffyRoundedUi.RADIUS_CONTROL);

        int controlsX = thumbX + thumbW + 11;
        int right = x + w - 10;
        int themeY = y + 52;
        g.drawString(font, "Theme", controlsX, themeY + 6, MUTED, false);
        int themeW = Math.min(88, Math.max(58, right - controlsX - 42));
        int themeX = right - themeW;
        fillRounded(g, themeX, themeY, themeW, 20, 0xC51D1736,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, themeX, themeY, themeW, 20,
                withAlpha(RuffyConfig.primaryColor(), 155), RuffyRoundedUi.RADIUS_CONTROL);
        fillRounded(g, themeX + 7, themeY + 6, 7, 7, RuffyConfig.primaryColor(), 4);
        g.drawString(font, trimToWidth(paletteName(RuffyConfig.getInt("palette")),
                Math.max(18, themeW - 30)), themeX + 19, themeY + 5, WHITE, false);
        g.drawString(font, "⌄", themeX + themeW - 11, themeY + 5, MUTED, false);

        int labelW = font.width("Particles") + 5;
        int sliderX = controlsX + labelW;
        int sliderW = Math.max(20, right - sliderX - 30);
        int opacityY = y + 83;
        g.drawString(font, "Opacity", controlsX, opacityY + 4, MUTED, false);
        drawSlider(g, sliderX, opacityY, sliderW,
                RuffyConfig.getInt("menu_opacity"), 60, 100);
        String opacity = RuffyConfig.getInt("menu_opacity") + "%";
        g.drawString(font, opacity, right - font.width(opacity),
                opacityY + 3, WHITE, false);

        int particlesY = y + 109;
        g.drawString(font, "Particles", controlsX, particlesY + 4, MUTED, false);
        drawSlider(g, sliderX, particlesY, sliderW,
                RuffyConfig.getInt("particle_density"), 0, 100);
        String particles = RuffyConfig.getInt("particle_density") + "%";
        g.drawString(font, particles, right - font.width(particles),
                particlesY + 3, WHITE, false);
    }

        private void drawAlbumArt(GuiGraphics g, int x, int y, int size) {
        int radius = size / 2;
        fillRounded(g, x, y, size, size, 0xFF100A25, radius);
        fillRounded(g, x + 2, y + 3, size - 4, size - 6,
                0xFF24104D, radius);
        fillRounded(g, x + 4, y + size / 2 - 4, size - 8,
                Math.max(6, size / 4), 0xFF8B42D0, Math.max(3, size / 8));
        fillRounded(g, x + size / 4, y + size / 5,
                size / 2, size / 2, 0xFF6330A6, size / 4);
        fillRounded(g, x + size / 3, y + size / 4,
                Math.max(4, size / 4), Math.max(4, size / 4),
                0xFFC89AFF, Math.max(2, size / 8));
        fillRounded(g, x + 5, y + size / 2 + 4, size - 10,
                Math.max(3, size / 10), 0xFFD78BFF, Math.max(2, size / 12));
        fillRounded(g, x + size / 3, y + size / 4,
                Math.max(2, size / 10), Math.max(2, size / 10),
                0xFFFFF4FF, Math.max(1, size / 20));
        outlineRounded(g, x, y, size, size, 0x998A5CE6, radius);
    }

    private void drawHomeLyrics(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeLyricsX;
        int y = homeLyricsY;
        int w = homeLyricsW;
        int h = homeLyricsH;
        RuffyRoundedUi.shadow(g, x, y, w, h, RuffyRoundedUi.RADIUS_PANEL, 20);
        fillRounded(g, x, y, w, h, 0xF0090B1D, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, withAlpha(RuffyConfig.secondaryColor(), 175),
                RuffyRoundedUi.RADIUS_PANEL);
        g.drawString(font, "♫  Lyrics", x + 10, y + 5,
                RuffyConfig.primaryColor(), false);
        String source = RuffySpotify.getLyrics();
        String[] lines = source == null ? new String[0] : source.split("\\R");
        int textY = y + Math.max(13, h * 35 / 100);
        if (lines.length == 0 || lines[0].isBlank()) {
            String hint = RuffySpotify.isConnected()
                    ? "Lyrics not available for this track."
                    : "Connect Spotify to view lyrics.";
            g.drawCenteredString(font, trimToWidth(hint, w - 18),
                    x + w / 2, textY, MUTED);
        } else {
            int maxShown = Math.min(2, lines.length);
            for (int i = 0; i < maxShown; i++) {
                g.drawCenteredString(font, trimToWidth(lines[i], w - 20),
                        x + w / 2, textY + i * Math.max(7, h * 25 / 100),
                        i == 0 ? WHITE : MUTED);
            }
        }
        if (inside(mouseX, mouseY, x, y, w, h)) {
            outlineRounded(g, x + 1, y + 1, w - 2, h - 2,
                    withAlpha(RuffyConfig.primaryColor(), 90), RuffyRoundedUi.RADIUS_PANEL);
        }
    }

    private void drawHomeBottom(GuiGraphics g, int mouseX, int mouseY) {
        int y = homeBottomY + 6;
        int h = Math.max(48, Math.min(66, Math.round(panelH * 0.135F)));

        fillRounded(g, mainX, y, homePerfW, h, 0xE1090818,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, mainX, y, homePerfW, h, BORDER,
                RuffyRoundedUi.RADIUS_CONTROL);
        String fps = Minecraft.getInstance().getFps() + " FPS";
        g.drawString(font, "ϟ", mainX + 12, y + 15,
                RuffyConfig.primaryColor(), false);
        g.drawString(font, "Performance", mainX + 30, y + 12, WHITE, false);
        g.drawString(font, "Smooth as ever before", mainX + 30, y + 25, MUTED, false);
        int gaugeX = mainX + homePerfW - 44;
        int gaugeY = y + h / 2;
        double gaugeTime = RuffyAnimation.timeSeconds();
        for (int i = 0; i < 30; i++) {
            double a = Math.PI * (1.12 + i / 29.0 * 0.76);
            int px = gaugeX + (int) Math.round(Math.cos(a) * 17);
            int py = gaugeY + (int) Math.round(Math.sin(a) * 17);
            int alpha = 80 + Math.round(145 * RuffyAnimation.breathe(gaugeTime, 0.08, i));
            fillRounded(g, px, py, 2, 2,
                    withAlpha(RuffyConfig.primaryColor(), alpha), 1);
        }
        g.drawCenteredString(font, Integer.toString(Minecraft.getInstance().getFps()),
                gaugeX, gaugeY - 2, WHITE);
        g.drawCenteredString(font, "FPS", gaugeX, gaugeY + 9, MUTED);

        fillRounded(g, homeVersionX, y, homeVersionW, h, 0xE1090819,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, homeVersionX, y, homeVersionW, h,
                withAlpha(RuffyConfig.primaryColor(), 140), RuffyRoundedUi.RADIUS_CONTROL);
        g.drawString(font, "♛", homeVersionX + 12, y + Math.max(12, h / 2 - 5),
                0xFFFFD45E, false);
        g.drawString(font, trimToWidth("Ruffy Visuals 2.7.0",
                Math.max(1, homeVersionW - 42)),
                homeVersionX + 31, y + Math.max(10, h / 2 - 10), WHITE, false);
        g.drawString(font, trimToWidth("The best visuals. Better than ever.",
                Math.max(1, homeVersionW - 42)),
                homeVersionX + 31, y + Math.max(24, h / 2 + 5), MUTED, false);

        String[] actions = {"◉", "▶", "⌘", "◎"};
        for (int i = 0; i < actions.length; i++) {
            int sx = homeSocialX + i * 58;
            boolean hover = inside(mouseX, mouseY, sx, y + (h - 42) / 2, 44, 42);
            fillRounded(g, sx, y + (h - 42) / 2, 42, 42,
                    hover ? 0xE3261A42 : 0xD20D1020, 21);
            outlineRounded(g, sx, y + (h - 42) / 2, 42, 42,
                    withAlpha(RuffyConfig.primaryColor(), hover ? 220 : 150), 21);
            g.drawCenteredString(font, actions[i], sx + 21,
                    y + (h - 42) / 2 + 16, i == 1 ? 0xFF7DE5B1 : WHITE);
        }
    }

    private String paletteName(int palette) {
        return switch (Math.max(0, Math.min(9, palette))) {
            case 1 -> "Rose";
            case 2 -> "Magenta";
            case 3 -> "Violet";
            case 4 -> "Azure";
            case 5 -> "Ice";
            case 6 -> "Mint";
            case 7 -> "Emerald";
            case 8 -> "Amber";
            case 9 -> "Solar";
            default -> "Galaxy";
        };
    }

    private RuffyFeature findFeature(String key) {
        for (RuffyFeature feature : RuffyFeature.ALL) {
            if (feature.key().equals(key)) return feature;
        }
        return null;
    }

    private String formatTime(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    private boolean handleHomeClick(int mx, int my) {
        prepareHomeLayout();

        // Window close action matches the rightmost circular title-bar control.
        int controlsX = mainX + mainW - 92;
        int controlsY = panelY + Math.max(5, Math.round(panelH * 0.028F));
        if (inside(mx, my, controlsX + 68, controlsY, 26, 26)) {
            onClose();
            return true;
        }

        int heroButtonX = homeCenterX + Math.max(10, homeCenterW * 4 / 100);
        int heroButtonH = Math.max(19, Math.min(28, homeHeroH / 5));
        int heroButtonY = homeHeroY + homeHeroH - heroButtonH
                - Math.max(12, homeHeroH * 11 / 100);
        int heroButtonW = Math.min(190, Math.max(90, homeCenterW * 28 / 100));
        if (inside(mx, my, heroButtonX, heroButtonY, heroButtonW, heroButtonH)) {
            tab = Tab.VISUALS;
            modulePage = 0;
            return true;
        }

        int moduleSeeX = homeCenterX + homeModulesW - 70;
        if (inside(mx, my, moduleSeeX, homeModulesY + 7, 58, 20)) {
            tab = Tab.VISUALS;
            modulePage = 0;
            return true;
        }

        String[] keys = {"prism_core", "nebula_particles", "orbit_particles", "swing_animation"};
        int gap = Math.max(4, Math.round(homeModulesW * 0.012F));
        int left = homeCenterX + 10;
        int top = homeModulesY + Math.max(16, Math.round(panelH * 0.100F));
        int tileW = Math.max(38, (homeModulesW - 20 - gap * 3) / 4);
        int tileH = Math.max(24, homeModulesH - (top - homeModulesY)
                - Math.max(4, Math.round(panelH * 0.02F)));
        for (int i = 0; i < keys.length; i++) {
            int tx = left + i * (tileW + gap);
            int ty = top;
            int switchW = Math.max(20, Math.min(28, tileW / 4));
            int switchH = Math.max(12, Math.min(16, tileH / 5));
            int switchX = tx + tileW - switchW - 7;
            int switchY = ty + 22;
            if (inside(mx, my, switchX - 2, switchY - 2, switchW + 4, switchH + 4)) {
                RuffyConfig.set(keys[i], !RuffyConfig.get(keys[i]));
                return true;
            }
            if (inside(mx, my, tx, ty, tileW, tileH)) {
                tab = Tab.VISUALS;
                selectedKey = keys[i];
                modulePage = 0;
                return true;
            }
        }

        // Spotify player controls and album/header open action.
        int spotifyX = homeRightX;
        int spotifyY = homeSpotifyY;
        int spotifyW = homeRightW;
        int center = spotifyX + spotifyW / 2;
        int controlsYPlayer = spotifyY + homeSpotifyH - (homeSpotifyH < 130 ? 22 : 27);
        if (inside(mx, my, center - 38, controlsYPlayer, 23, 21)) {
            RuffySpotify.previousTrack();
            return true;
        }
        if (inside(mx, my, center - 13, controlsYPlayer - 3, 27, 26)) {
            RuffySpotify.togglePlayback();
            return true;
        }
        if (inside(mx, my, center + 15, controlsYPlayer, 23, 21)) {
            RuffySpotify.nextTrack();
            return true;
        }
        if (inside(mx, my, spotifyX + spotifyW - 27, spotifyY + 12, 20, 22)
                || inside(mx, my, spotifyX, spotifyY, spotifyW, 34)) {
            if (minecraft != null) minecraft.setScreen(new RuffySpotifyScreen(this));
            return true;
        }

        // Appearance card: theme, glass opacity and actual particle density.
        int quickX = homeQuickX;
        int quickY = homeAppearanceY;
        int quickW = homeQuickW;
        int thumbW = Math.min(84, Math.max(58, quickW * 33 / 100));
        int appearanceControlsX = quickX + 10 + thumbW + 11;
        int right = quickX + quickW - 10;
        int themeY = quickY + 52;
        int themeW = Math.min(88, Math.max(58, right - appearanceControlsX - 42));
        int themeX = right - themeW;
        if (inside(mx, my, themeX, themeY, themeW, 20)) {
            RuffyConfig.setInt("palette", (RuffyConfig.getInt("palette") + 1) % 10);
            return true;
        }

        int labelW = font.width("Particles") + 5;
        int sliderX = appearanceControlsX + labelW;
        int sliderW = Math.max(20, right - sliderX - 30);
        int opacityY = quickY + 83;
        if (inside(mx, my, sliderX - 4, opacityY - 3, sliderW + 8, 12)) {
            int value = 60 + Math.round((mx - sliderX) * 40.0F / Math.max(1, sliderW));
            RuffyConfig.setInt("menu_opacity", value);
            return true;
        }

        int particlesY = quickY + 109;
        if (inside(mx, my, sliderX - 4, particlesY - 3, sliderW + 8, 12)) {
            int value = Math.round((mx - sliderX) * 100.0F / Math.max(1, sliderW));
            RuffyConfig.setInt("particle_density", value);
            return true;
        }

        // Lyrics opens the full player and lyrics screen.
        if (inside(mx, my, homeLyricsX, homeLyricsY,
                homeLyricsW, homeLyricsH)) {
            if (minecraft != null) minecraft.setScreen(new RuffySpotifyScreen(this));
            return true;
        }

        // Footer shortcuts are real navigation destinations.
        int footerY = homeBottomY + 6;
        int footerH = Math.max(48, Math.min(66, Math.round(panelH * 0.135F)));
        for (int i = 0; i < 4; i++) {
            int sx = homeSocialX + i * 58;
            if (!inside(mx, my, sx, footerY + (footerH - 42) / 2, 42, 42)) continue;
            switch (i) {
                case 0 -> tab = Tab.HOME;
                case 1 -> {
                    tab = Tab.MUSIC;
                    if (minecraft != null) minecraft.setScreen(new RuffySpotifyScreen(this));
                }
                case 2 -> tab = Tab.CONFIGS;
                case 3 -> tab = Tab.SETUP;
                default -> { }
            }
            return true;
        }
        return false;
    }

    private int enabledCount() {
        int count = 0;
        for (RuffyFeature feature : RuffyFeature.ALL) {
            if (RuffyConfig.get(feature.key())) count++;
        }
        return count;
    }

    private void drawModuleBrowser(GuiGraphics g, int mouseX, int mouseY,
                                   RuffyFeature.Section requiredSection) {
        drawFilterBar(g, mouseX, mouseY, requiredSection);
        List<RuffyFeature> features = filteredFeatures(requiredSection);
        int pages = Math.max(1, (features.size() + 7) / 8);
        modulePage = Math.max(0, Math.min(modulePage, pages - 1));

        if (features.isEmpty()) {
            String emptyTitle = searchText.isBlank() ? "NO MODULES IN THIS VIEW" : "NO MATCHES FOUND";
            String emptyHint = searchText.isBlank()
                    ? "Choose another section or filter to explore more effects."
                    : "Try a shorter search or clear the active filter.";
            int centerY = listTop + Math.max(30, (rowHeight * 2) / 2);
            g.drawCenteredString(font, emptyTitle, mainX + mainW / 2,
                    centerY, WHITE);
            g.drawCenteredString(font, trimToWidth(emptyHint, mainW - 28),
                    mainX + mainW / 2, centerY + 16, MUTED);
        }

        for (int i = 0; i < 8; i++) {
            int index = modulePage * 8 + i;
            if (index >= features.size()) break;
            int column = i % 2;
            int row = i / 2;
            int x = mainX + column * (cardWidth + cardGap);
            int y = listTop + row * (rowHeight + cardGap);
            drawModuleCard(g, mouseX, mouseY, features.get(index),
                    x, y, cardWidth, rowHeight);
        }

        drawModulePagination(g, features.size(), pages, mouseX, mouseY);
    }

    private void drawFilterBar(GuiGraphics g, int mouseX, int mouseY,
                               RuffyFeature.Section requiredSection) {
        if (requiredSection == null) {
            int x = mainX;
            for (Filter item : Filter.values()) {
                boolean selected = filter == item;
                boolean hovered = inside(mouseX, mouseY, x, filterY, item.width, 22);
                drawButton(g, x, filterY, item.width, 22, item.label,
                        selected ? ButtonStyle.SEGMENT : ButtonStyle.GHOST,
                        hovered, selected);
                x += item.width + 5;
            }
        } else {
            String text = requiredSection == RuffyFeature.Section.WORLD
                    ? "WORLD COLLECTION" : "HUD COLLECTION";
            fillRounded(g, mainX, filterY + 2, font.width(text) + 18, 19,
                    0x401D2640, RuffyRoundedUi.RADIUS_CONTROL);
            g.drawString(font, text, mainX + 9, filterY + 8,
                    RuffyConfig.primaryColor(), false);
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
                if (filter == Filter.WORLD && feature.section() != RuffyFeature.Section.WORLD) continue;
                if (filter == Filter.STARRED
                        && !RuffyConfig.get("favorite_" + feature.key())) continue;
            }

            if (!searchText.isBlank()) {
                String term = searchText.toLowerCase(Locale.ROOT);
                if (!feature.title().toLowerCase(Locale.ROOT).contains(term)
                        && !feature.description().toLowerCase(Locale.ROOT).contains(term)) {
                    continue;
                }
            }
            result.add(feature);
        }
        return result;
    }

    private void drawModuleCard(GuiGraphics g, int mouseX, int mouseY,
                                RuffyFeature feature, int x, int y, int w, int h) {
        boolean enabled = RuffyConfig.get(feature.key());
        boolean selected = feature.key().equals(selectedKey);
        boolean favorite = RuffyConfig.get("favorite_" + feature.key());
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);

        float hover = RuffyAnimation.animate("studio.card.hover." + feature.key(),
                hovered ? 1.0F : 0.0F);
        float active = RuffyAnimation.animate("studio.card.active." + feature.key(),
                enabled ? 1.0F : 0.0F);
        int background = RuffyAnimation.blendColor(CARD, CARD_HOVER, hover * 0.85F);
        background = RuffyAnimation.blendColor(background,
                withAlpha(RuffyConfig.primaryColor(), 28), selected ? 1.0F : 0.0F);
        int edge = RuffyAnimation.blendColor(BORDER,
                withAlpha(RuffyConfig.primaryColor(), 132),
                Math.max(hover * 0.5F, selected ? 1.0F : 0.0F));

        fillRounded(g, x, y, w, h, background, RuffyRoundedUi.RADIUS_CARD);
        outlineRounded(g, x, y, w, h, edge, RuffyRoundedUi.RADIUS_CARD);

        int iconX = x + 10;
        int iconY = y + (h - 28) / 2;
        int iconColor = feature.section() == RuffyFeature.Section.HUD
                ? 0xFF4EA8DE : feature.section() == RuffyFeature.Section.WORLD
                ? 0xFF52C7B0 : RuffyConfig.primaryColor();
        fillRounded(g, iconX, iconY, 28, 28,
                withAlpha(iconColor, 32), RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, iconX, iconY, 28, 28,
                withAlpha(iconColor, 92), RuffyRoundedUi.RADIUS_CONTROL);
        g.drawCenteredString(font, iconFor(feature), iconX + 14, iconY + 10,
                withAlpha(iconColor, 245));

        int switchX = x + w - 47;
        int switchY = y + h - 22;
        int textX = x + 47;
        int textMax = Math.max(40, switchX - textX - 8);
        g.drawString(font, trimToWidth(feature.title(), textMax),
                textX, y + 8, selected ? WHITE : 0xFFE8E8F4, false);
        g.drawString(font, trimToWidth(feature.description(), textMax),
                textX, y + 22, MUTED, false);

        int starX = x + w - 18;
        int starY = y + 4;
        drawIconButton(g, starX - 8, starY, 16, 14,
                favorite ? "★" : "☆", favorite ? 0xFFFFD17D : DIM,
                inside(mouseX, mouseY, starX - 8, starY, 16, 14));
        drawSwitch(g, switchX, switchY, enabled, 38, 18,
                "card." + feature.key());

        if (active > 0.01F) {
            fillRounded(g, x + 9, y + h - 3, Math.min(26, w - 18), 1,
                    withAlpha(iconColor, Math.round(110 * active)), 1);
        }
    }

    private String trimToWidth(String value, int maxWidth) {
        if (value == null || value.isEmpty()) return "";
        String shown = value;
        while (shown.length() > 1 && font.width(shown + "…") > maxWidth) {
            shown = shown.substring(0, shown.length() - 1);
        }
        return shown.equals(value) ? value
                : (font.width("…") <= maxWidth ? shown + "…" : "");
    }

    private String iconFor(RuffyFeature feature) {
        return switch (feature.section()) {
            case HUD -> "▣";
            case MOTION -> "✧";
            case ENTITIES -> "◇";
            case WORLD -> "◌";
        };
    }

    private void drawModulePagination(GuiGraphics g, int count, int pages,
                                      int mouseX, int mouseY) {
        int y = panelY + panelH - 44;
        String leftLabel = count + " MODULES  ·  " + enabledCount() + " ENABLED";
        g.drawString(font, leftLabel, mainX, y + 8, MUTED, false);

        int right = mainX + mainW - 80;
        drawButton(g, right, y, 22, 21, "‹", ButtonStyle.GHOST,
                inside(mouseX, mouseY, right, y, 22, 21), false);
        String page = (modulePage + 1) + " / " + pages;
        g.drawCenteredString(font, page, right + 40, y + 7, WHITE);
        drawButton(g, right + 58, y, 22, 21, "›", ButtonStyle.GHOST,
                inside(mouseX, mouseY, right + 58, y, 22, 21), false);
    }

    private void drawPresets(GuiGraphics g, int mouseX, int mouseY) {
        int x = mainX;
        int y = panelY + 104;
        String[] names = {"NEBULA", "CINEMATIC", "PERFORMANCE", "CLEAN"};
        String[] desc = {
                "Full galaxy color and orbit atmosphere",
                "Soft rose glow with flowing movement",
                "Reduced geometry for higher FPS",
                "Quiet interface and minimal particles"
        };
        int[] palettes = {0, 1, 4, 5};
        int gap = 10;
        int w = (mainW - gap) / 2;
        int h = 94;

        for (int i = 0; i < 4; i++) {
            int cx = x + (i % 2) * (w + gap);
            int cy = y + (i / 2) * (h + 12);
            int accent = palettePrimary(palettes[i]);
            boolean hovered = inside(mouseX, mouseY, cx, cy, w, h);
            float hover = RuffyAnimation.animate("studio.preset." + i,
                    hovered ? 1.0F : 0.0F);

            fillRounded(g, cx, cy, w, h,
                    RuffyAnimation.blendColor(CARD, CARD_HOVER, hover),
                    RuffyRoundedUi.RADIUS_PANEL);
            outlineRounded(g, cx, cy, w, h,
                    RuffyAnimation.blendColor(BORDER, withAlpha(accent, 175), hover),
                    RuffyRoundedUi.RADIUS_PANEL);
            fillRounded(g, cx + 12, cy + 12, 26, 26,
                    withAlpha(accent, 42), RuffyRoundedUi.RADIUS_CONTROL);
            fillRounded(g, cx + 17, cy + 17, 16, 16,
                    accent, RuffyRoundedUi.RADIUS_CONTROL);
            g.drawString(font, names[i], cx + 48, cy + 13, WHITE, false);
            g.drawString(font, desc[i], cx + 48, cy + 29, MUTED, false);
            drawButton(g, cx + w - 82, cy + h - 31, 70, 22,
                    "APPLY", ButtonStyle.PRIMARY,
                    inside(mouseX, mouseY, cx + w - 82, cy + h - 31, 70, 22), false);
        }
    }


    private void drawCosmetics(GuiGraphics g, int mouseX, int mouseY) {
        int x = mainX;
        int y = panelY + 72;
        int h = 76;
        int gap = 7;

        drawCosmeticCategory(g, mouseX, mouseY, x, y, mainW, h,
                "PLAYER AURA", "A subtle orbit around your character",
                "cosmetic_aura",
                new String[]{"OFF", "NEBULA", "SOLAR", "FROST", "ROSE"},
                new int[]{0xFF666A7A, 0xFFC58CFF, 0xFFFFB34D, 0xFF79E7FF, 0xFFFF70B8},
                RuffyConfig.getInt("cosmetic_aura"));
        y += h + gap;

        drawCosmeticCategory(g, mouseX, mouseY, x, y, mainW, h,
                "COMPANION", "A floating crystal that follows your shoulder",
                "cosmetic_companion",
                new String[]{"NONE", "AMETHYST", "SOLAR", "FROST", "ROSE"},
                new int[]{0xFF666A7A, 0xFFC98BFF, 0xFFFFC15A, 0xFF8CEBFF, 0xFFFF78C6},
                RuffyConfig.getInt("cosmetic_companion"));
        y += h + gap;

        drawCosmeticCategory(g, mouseX, mouseY, x, y, mainW, h,
                "MOVEMENT TRAIL", "A custom trail while walking or sprinting",
                "cosmetic_trail",
                new String[]{"OFF", "NEBULA", "EMBER", "FROST", "PRISM"},
                new int[]{0xFF666A7A, 0xFFC58CFF, 0xFFFF9A42, 0xFF79E7FF, 0xFFCA7BFF},
                RuffyConfig.getInt("cosmetic_trail"));

        int noteY = panelY + panelH - 43;
        g.drawString(font, "COSMETICS ARE CLIENT-SIDE VISUALS ONLY",
                x + 2, noteY + 7, DIM, false);
        String equipped = (RuffyConfig.getInt("cosmetic_aura") > 0 ? 1 : 0)
                + (RuffyConfig.getInt("cosmetic_companion") > 0 ? 1 : 0)
                + (RuffyConfig.getInt("cosmetic_trail") > 0 ? 1 : 0) + " EQUIPPED";
        g.drawString(font, equipped, x + mainW - font.width(equipped),
                noteY + 7, RuffyConfig.primaryColor(), false);
    }

    private void drawCosmeticCategory(GuiGraphics g, int mouseX, int mouseY,
                                      int x, int y, int w, int h,
                                      String title, String subtitle, String key,
                                      String[] names, int[] colors, int selected) {
        fillRounded(g, x, y, w, h, 0xD00A0D1C,
                RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, BORDER,
                RuffyRoundedUi.RADIUS_PANEL);

        g.drawString(font, title, x + 12, y + 9, WHITE, false);
        g.drawString(font, subtitle, x + 12, y + 22, MUTED, false);
        String status = selected == 0 ? "NOT EQUIPPED" : "EQUIPPED";
        int statusColor = selected == 0 ? DIM : 0xFF68E7AC;
        g.drawString(font, status, x + w - font.width(status) - 12,
                y + 10, statusColor, false);

        int chipY = y + 40;
        int innerX = x + 10;
        int chipGap = 5;
        int chipW = (w - 20 - chipGap * 4) / 5;
        for (int i = 0; i < names.length; i++) {
            int cx = innerX + i * (chipW + chipGap);
            boolean active = selected == i;
            boolean hovered = inside(mouseX, mouseY, cx, chipY, chipW, 29);
            float hover = RuffyAnimation.animate("cosmetic.hover." + key + "." + i,
                    hovered ? 1.0F : 0.0F);
            int background = active ? withAlpha(colors[i], 48)
                    : RuffyAnimation.blendColor(0x8014192A, 0xC51E2340, hover);
            fillRounded(g, cx, chipY, chipW, 29, background,
                    RuffyRoundedUi.RADIUS_CONTROL);
            outlineRounded(g, cx, chipY, chipW, 29,
                    active ? withAlpha(colors[i], 200)
                            : withAlpha(0xFF8B91AA, hovered ? 100 : 38),
                    RuffyRoundedUi.RADIUS_CONTROL);
            fillRounded(g, cx + 7, chipY + 10, 8, 8, colors[i], 4);
            String label = trimToWidth(names[i], Math.max(12, chipW - 22));
            g.drawString(font, label, cx + 19, chipY + 11,
                    active ? WHITE : MUTED, false);
            if (active) {
                fillRounded(g, cx + chipW - 8, chipY + 12, 3, 3,
                        colors[i], 2);
            }
        }
    }

    private boolean handleCosmeticsClick(int mx, int my) {
        int x = mainX;
        int y = panelY + 72;
        int h = 82;
        int gap = 9;
        String[] keys = {"cosmetic_aura", "cosmetic_companion", "cosmetic_trail"};
        for (String key : keys) {
            int chipY = y + 43;
            int chipGap = 5;
            int chipW = (mainW - 20 - chipGap * 4) / 5;
            int innerX = x + 10;
            for (int i = 0; i < 5; i++) {
                int cx = innerX + i * (chipW + chipGap);
                if (inside(mx, my, cx, chipY, chipW, 29)) {
                    RuffyConfig.setInt(key, i);
                    return true;
                }
            }
            y += h + gap;
        }
        return false;
    }

    private void drawStyle(GuiGraphics g, int mouseX, int mouseY) {
        int x = mainX;
        int y = panelY + 96;
        g.drawString(font, "COLOR SYSTEM", x, y, WHITE, false);
        drawButton(g, x + mainW - 78, y - 5, 70, 19, "PRESETS",
                ButtonStyle.OUTLINE,
                inside(mouseX, mouseY, x + mainW - 78, y - 5, 70, 19), false);
        drawButton(g, x + mainW - 162, y - 5, 78, 19, "COSMETICS",
                ButtonStyle.OUTLINE,
                inside(mouseX, mouseY, x + mainW - 162, y - 5, 78, 19), false);
        int sw = (mainW - 24) / 5;
        for (int i = 0; i < PALETTE_NAMES.length; i++) {
            int cx = x + (i % 5) * (sw + 6);
            int cy = y + 16 + (i / 5) * 34;
            int primary = palettePrimary(i);
            int secondary = paletteSecondary(i);
            boolean selected = RuffyConfig.getInt("palette") == i;
            boolean hovered = inside(mouseX, mouseY, cx, cy, sw, 28);
            float active = RuffyAnimation.animate("studio.palette." + i,
                    selected ? 1.0F : 0.0F);
            float hover = RuffyAnimation.animate("studio.palette.hover." + i,
                    hovered ? 1.0F : 0.0F);
            int bg = RuffyAnimation.blendColor(CARD, CARD_HOVER, hover);
            bg = RuffyAnimation.blendColor(bg, withAlpha(primary, 46), active);
            fillRounded(g, cx, cy, sw, 28, bg, RuffyRoundedUi.RADIUS_CONTROL);
            outlineRounded(g, cx, cy, sw, 28,
                    RuffyAnimation.blendColor(BORDER, withAlpha(primary, 200), active),
                    RuffyRoundedUi.RADIUS_CONTROL);
            fillRounded(g, cx + 7, cy + 8, 10, 10, primary, 5);
            fillRounded(g, cx + 21, cy + 8, 10, 10, secondary, 5);
            if (active > 0.5F) {
                fillRounded(g, cx + sw - 9, cy + 9, 3, 3, WHITE, 2);
            }
            g.drawString(font, PALETTE_NAMES[i], cx + 36, cy + 10,
                    active > 0.5F ? WHITE : MUTED, false);
        }

        int settingsY = y + 87;
        drawLabeledSlider(g, "HUD OPACITY", RuffyConfig.getInt("hud_opacity") + "%",
                x, settingsY, mainW - 12, RuffyConfig.getInt("hud_opacity"), 12, 90);
        drawLabeledSlider(g, "HUD SCALE", RuffyConfig.getInt("hud_scale") + "%",
                x, settingsY + 42, mainW - 12, RuffyConfig.getInt("hud_scale"), 75, 120);
        drawLabeledSlider(g, "MENU OPACITY", RuffyConfig.getInt("menu_opacity") + "%",
                x, settingsY + 84, mainW - 12, RuffyConfig.getInt("menu_opacity"), 60, 100);
        drawLabeledSlider(g, "EFFECT QUALITY", qualityName(RuffyConfig.getInt("effect_quality")),
                x, settingsY + 126, mainW - 12, RuffyConfig.getInt("effect_quality"), 0, 2);

        int smoothY = settingsY + 173;
        g.drawString(font, "SMOOTH ANIMATIONS", x, smoothY + 5, WHITE, false);
        g.drawString(font, RuffyConfig.get("smooth_animations") ? "ENABLED" : "DISABLED",
                x + mainW - 93, smoothY + 5,
                RuffyConfig.get("smooth_animations") ? RuffyConfig.primaryColor() : DIM, false);
        drawSwitch(g, x + mainW - 43, smoothY, RuffyConfig.get("smooth_animations"),
                34, 18, "style.global");
        drawLabeledSlider(g, "ANIMATION RESPONSE",
                RuffyConfig.getInt("animation_smoothness") + "%",
                x, smoothY + 29, mainW - 12,
                RuffyConfig.getInt("animation_smoothness"), 0, 100);
    }

    private void drawLabeledSlider(GuiGraphics g, String title, String value,
                                   int x, int y, int w, int current, int min, int max) {
        g.drawString(font, title, x, y, MUTED, false);
        String valueLabel = value;
        g.drawString(font, valueLabel, x + w - font.width(valueLabel),
                y, WHITE, false);
        drawSlider(g, x, y + 12, w, current, min, max);
    }

    private void drawConfigs(GuiGraphics g, int mouseX, int mouseY) {
        int x = mainX;
        int y = panelY + 110;
        g.drawString(font, "MAINTENANCE", x, y - 18, WHITE, false);
        drawActionCard(g, mouseX, mouseY, x, y,
                mainW, 50, "RESET MODULES",
                "Restore the original on/off states");
        drawActionCard(g, mouseX, mouseY, x, y + 64,
                mainW, 50, "RESET APPEARANCE",
                "Restore palette, opacity, scale and motion");
        drawActionCard(g, mouseX, mouseY, x, y + 128,
                mainW, 50, "RESET EVERYTHING",
                "Return all client options to their defaults");
    }

    private void drawSetup(GuiGraphics g) {
        int x = mainX;
        int y = panelY + 105;
        g.drawString(font, "QUICK GUIDE", x, y - 17, WHITE, false);
        drawInfoRow(g, x, y, "RIGHT SHIFT", "Open or close the control center");
        drawInfoRow(g, x, y + 36, "LEFT CLICK", "Select a card or switch a module");
        drawInfoRow(g, x, y + 72, "ESC", "Close the current screen");
        drawInfoRow(g, x, y + 108, "CONFIG FILE",
                "config/ruffyclient.properties");
        drawInfoRow(g, x, y + 144, "BUILD",
                "Ruffy Visuals 2.7.0 · Minecraft 1.21.11 · Fabric");
    }

    private void drawMusicLanding(GuiGraphics g, int mouseX, int mouseY) {
        int x = mainX;
        int y = panelY + 112;
        int w = mainW;
        fillRounded(g, x, y, w, 168, CARD, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, 168, BORDER, RuffyRoundedUi.RADIUS_PANEL);
        fillRounded(g, x + 18, y + 18, 48, 48,
                withAlpha(RuffyConfig.primaryColor(), 46), RuffyRoundedUi.RADIUS_PANEL);
        g.drawCenteredString(font, "♫", x + 42, y + 34, RuffyConfig.primaryColor());
        g.drawString(font, "SPOTIFY CONNECT", x + 80, y + 20, WHITE, false);
        g.drawString(font, "Control playback and view lyrics in your studio.",
                x + 80, y + 37, MUTED, false);

        String state = RuffySpotify.isConnected() ? "CONNECTED" : "NOT CONNECTED";
        int stateColor = RuffySpotify.isConnected() ? 0xFF5DD6A1 : 0xFFFFC078;
        fillRounded(g, x + 18, y + 83, font.width(state) + 22, 19,
                withAlpha(stateColor, 35), RuffyRoundedUi.RADIUS_CONTROL);
        fillRounded(g, x + 26, y + 90, 5, 5, stateColor, 3);
        g.drawString(font, state, x + 38, y + 89, stateColor, false);
        g.drawString(font, RuffySpotify.getStatus(),
                x + 18, y + 114, MUTED, false);
        drawButton(g, x + 18, y + 136, 126, 24,
                "OPEN MUSIC", ButtonStyle.PRIMARY,
                inside(mouseX, mouseY, x + 18, y + 136, 126, 24), false);
        g.drawString(font, "LYRICS  ·  PLAYBACK  ·  DEVICE STATUS",
                x + 158, y + 144, DIM, false);
    }

    private void drawActionCard(GuiGraphics g, int mouseX, int mouseY,
                                int x, int y, int w, int h,
                                String title, String subtitle) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        float hover = RuffyAnimation.animate("studio.action." + title,
                hovered ? 1.0F : 0.0F);
        fillRounded(g, x, y, w, h,
                RuffyAnimation.blendColor(CARD, CARD_HOVER, hover),
                RuffyRoundedUi.RADIUS_CARD);
        outlineRounded(g, x, y, w, h,
                RuffyAnimation.blendColor(BORDER,
                        withAlpha(RuffyConfig.primaryColor(), 95), hover),
                RuffyRoundedUi.RADIUS_CARD);
        g.drawString(font, title, x + 13, y + 9, WHITE, false);
        g.drawString(font, subtitle, x + 13, y + 25, MUTED, false);
        drawButton(g, x + w - 91, y + 13, 78, 24,
                "RESET", ButtonStyle.OUTLINE,
                inside(mouseX, mouseY, x + w - 91, y + 13, 78, 24), false);
    }

    private void drawInfoRow(GuiGraphics g, int x, int y, String left, String right) {
        fillRounded(g, x, y, mainW, 28, SURFACE_SOFT,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, x, y, mainW, 28, BORDER,
                RuffyRoundedUi.RADIUS_CONTROL);
        g.drawString(font, left, x + 11, y + 10,
                RuffyConfig.primaryColor(), false);
        int valueX = x + 128;
        int available = x + mainW - 12 - valueX;
        g.drawString(font, trimToWidth(right, available), valueX,
                y + 10, MUTED, false);
    }

    private void drawSlider(GuiGraphics g, int x, int y, int w,
                            int value, int min, int max) {
        if (w <= 0 || max <= min) return;
        float ratio = Math.max(0.0F, Math.min(1.0F,
                (value - min) / (float) (max - min)));
        float animated = RuffyAnimation.animate("studio.slider." + x + "." + y, ratio);
        int fill = Math.max(0, Math.min(w, Math.round(animated * w)));
        fillRounded(g, x, y + 5, w, 4, 0xFF262D40, 2);
        if (fill > 0) {
            fillRounded(g, x, y + 5, fill, 4, RuffyConfig.primaryColor(), 2);
        }
        int thumbX = Math.max(x + 4, Math.min(x + w - 4, x + fill));
        fillRounded(g, thumbX - 4, y + 2, 8, 10, WHITE, 4);
        fillRounded(g, thumbX - 2, y + 4, 4, 6,
                withAlpha(RuffyConfig.primaryColor(), 210), 2);
    }

    private void drawSwitch(GuiGraphics g, int x, int y, boolean enabled,
                            int w, int h, String animationKey) {
        float progress = RuffyAnimation.animate("studio.switch." + animationKey,
                enabled ? 1.0F : 0.0F);
        int track = RuffyAnimation.blendColor(0xFF292E3D,
                withAlpha(RuffyConfig.primaryColor(), 225), progress);
        int border = RuffyAnimation.blendColor(0xFF505A71,
                withAlpha(RuffyConfig.primaryColor(), 210), progress);
        if (progress > 0.05F) {
            fillRounded(g, x - 3, y - 3, w + 6, h + 6,
                    withAlpha(RuffyConfig.primaryColor(),
                            Math.round(24 * progress)), h / 2 + 3);
        }
        fillRounded(g, x, y, w, h, track, h / 2);
        outlineRounded(g, x, y, w, h, border, h / 2);
        int knob = h - 6;
        int travel = Math.max(0, w - knob - 6);
        int knobX = x + 3 + Math.round(travel * progress);
        fillRounded(g, knobX, y + 3, knob, knob,
                progress > 0.5F ? WHITE : 0xFFB4B9C9, knob / 2);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h,
                            String label, ButtonStyle style,
                            boolean hovered, boolean selected) {
        int accent = RuffyConfig.primaryColor();
        int background;
        int border;
        int foreground = WHITE;
        int radius = Math.min(RuffyRoundedUi.RADIUS_CONTROL, h / 2);

        switch (style) {
            case PRIMARY -> {
                background = hovered ? RuffyAnimation.blendColor(accent,
                        0xFFFFFFFF, 0.12F) : accent;
                border = withAlpha(0xFFFFFFFF, hovered ? 88 : 34);
                fillRounded(g, x, y + 1, w, h, 0x29101428, radius);
            }
            case TONAL -> {
                background = hovered ? withAlpha(accent, 62) : withAlpha(accent, 38);
                border = withAlpha(accent, hovered ? 122 : 68);
                foreground = WHITE;
            }
            case OUTLINE -> {
                background = hovered ? withAlpha(accent, 26) : 0x22151A2A;
                border = withAlpha(hovered ? accent : 0xFF8992A8,
                        hovered ? 155 : 76);
                foreground = hovered ? WHITE : MUTED;
            }
            case GHOST -> {
                background = hovered ? 0x3AFFFFFF : 0x00000000;
                border = hovered ? 0x426E7892 : 0x00000000;
                foreground = hovered ? WHITE : MUTED;
            }
            case SEGMENT -> {
                background = selected ? withAlpha(accent, 58)
                        : hovered ? 0x35FFFFFF : 0x21111728;
                border = selected ? withAlpha(accent, 145)
                        : hovered ? 0x426E7892 : 0x00000000;
                foreground = selected ? WHITE : MUTED;
            }
            default -> {
                background = 0x00000000;
                border = 0x00000000;
            }
        }

        fillRounded(g, x, y, w, h, background, radius);
        if ((border >>> 24) != 0) {
            outlineRounded(g, x, y, w, h, border, radius);
        }
        if (style == ButtonStyle.PRIMARY || selected) {
            fillRounded(g, x + 7, y + 1, Math.max(0, w - 14), 1,
                    withAlpha(0xFFFFFFFF, hovered ? 50 : 27), 1);
        }
        g.drawCenteredString(font, label, x + w / 2,
                y + Math.max(2, (h - 8) / 2), foreground);
    }

    private void drawIconButton(GuiGraphics g, int x, int y, int w, int h,
                                String label, int color, boolean hovered) {
        int bg = hovered ? 0x4DFFFFFF : 0x00000000;
        fillRounded(g, x, y, w, h, bg, RuffyRoundedUi.RADIUS_SMALL);
        if (hovered) {
            outlineRounded(g, x, y, w, h, 0x536D7895,
                    RuffyRoundedUi.RADIUS_SMALL);
        }
        g.drawCenteredString(font, label, x + w / 2,
                y + Math.max(2, (h - 8) / 2), color);
    }

    private void drawFooter(GuiGraphics g) {
        int y = panelY + panelH - 1;
        g.fill(mainX + 14, y, mainX + mainW - 14, y + 1, 0x315E6885);
        fillRounded(g, mainX + 14, y, 58, 1,
                withAlpha(RuffyConfig.primaryColor(), 125), 1);
        if (tab != Tab.HOME) {
            g.drawString(font, "RUFFY VISUALS 2.7  ·  GALAXY STUDIO",
                    mainX, panelY + panelH - 18, DIM, false);
            String hint = "RIGHT SHIFT  ·  ESC";
            g.drawString(font, hint, mainX + mainW - font.width(hint),
                    panelY + panelH - 18, DIM, false);
        }
    }

    private static String qualityName(int quality) {
        return switch (quality) {
            case 0 -> "LOW";
            case 1 -> "MEDIUM";
            default -> "HIGH";
        };
    }

    private static int palettePrimary(int palette) {
        int[] colors = {
                0xFFB45CFF, 0xFFFF4D8D, 0xFFEC4CFF, 0xFF995CFF,
                0xFF38BDF8, 0xFF7DD3FC, 0xFF41D9B0, 0xFF45C779,
                0xFFFFB547, 0xFFFFE16A
        };
        return colors[Math.max(0, Math.min(colors.length - 1, palette))];
    }

    private static int paletteSecondary(int palette) {
        int[] colors = {
                0xFF6034AE, 0xFFA92254, 0xFF7A2FA3, 0xFF4E2A9C,
                0xFF17678C, 0xFF326A90, 0xFF1D826A, 0xFF21764B,
                0xFFA7651E, 0xFFA98A2E
        };
        return colors[Math.max(0, Math.min(colors.length - 1, palette))];
    }

    private static void fillRounded(GuiGraphics g, int x, int y, int w, int h,
                                    int color, int radius) {
        RuffyRoundedUi.fill(g, x, y, w, h, color, radius);
    }

    private static void outlineRounded(GuiGraphics g, int x, int y, int w, int h,
                                       int color, int radius) {
        RuffyRoundedUi.outline(g, x, y, w, h, color, radius);
    }

    private static int withAlpha(int color, int alpha) {
        return ((Math.max(0, Math.min(255, alpha)) & 0xFF) << 24)
                | (color & 0x00FFFFFF);
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
        if (tab == Tab.HOME && handleHomeClick(mx, my)) return true;

        if (isModuleTab()) {
            if (handleFilters(mx, my)) return true;
            if (handleModuleCardClick(mx, my)) return true;
            if (handlePagination(mx, my)) return true;
        }
        if (tab == Tab.PRESETS && handlePresetClick(mx, my)) return true;
        if (tab == Tab.COSMETICS && handleCosmeticsClick(mx, my)) return true;
        if (tab == Tab.STYLE && handleStyleClick(mx, my)) return true;
        if (tab == Tab.CONFIGS && handleConfigClick(mx, my)) return true;
        if (tab == Tab.MUSIC && inside(mx, my,
                mainX + 18, panelY + 248, 126, 24)) {
            if (minecraft != null) minecraft.setScreen(new RuffySpotifyScreen(this));
            return true;
        }
        return true;
    }

    private boolean handleNavigation(int mx, int my) {
        int x = sidebarX + Math.max(4, Math.round(sidebarW * 0.06F));
        int w = sidebarW - Math.max(8, Math.round(sidebarW * 0.12F));
        int[] ys = navigationYPositions();
        int itemHeight = navigationItemHeight();
        Tab[] tabs = {
                Tab.HOME, Tab.VISUALS, Tab.STYLE, Tab.HUD,
                Tab.MUSIC, Tab.CONFIGS, Tab.SETUP
        };
        for (int i = 0; i < tabs.length; i++) {
            if (!inside(mx, my, x, ys[i], w, itemHeight)) continue;
            tab = tabs[i];
            modulePage = 0;
            if (searchBox != null) searchBox.setVisible(true);
            if (tab == Tab.MUSIC && minecraft != null) {
                minecraft.setScreen(new RuffySpotifyScreen(this));
            }
            return true;
        }
        return false;
    }

    private boolean handleFilters(int mx, int my) {
        if (tab != Tab.VISUALS) return false;
        int x = mainX;
        for (Filter item : Filter.values()) {
            if (inside(mx, my, x, filterY, item.width, 22)) {
                filter = item;
                modulePage = 0;
                return true;
            }
            x += item.width + 5;
        }
        return false;
    }

    private boolean handleModuleCardClick(int mx, int my) {
        RuffyFeature.Section section = tab == Tab.HUD ? RuffyFeature.Section.HUD
                : tab == Tab.WORLD ? RuffyFeature.Section.WORLD : null;
        List<RuffyFeature> features = filteredFeatures(section);

        for (int i = 0; i < 8; i++) {
            int index = modulePage * 8 + i;
            if (index >= features.size()) break;
            int col = i % 2;
            int row = i / 2;
            int x = mainX + col * (cardWidth + cardGap);
            int y = listTop + row * (rowHeight + cardGap);
            RuffyFeature feature = features.get(index);
            if (!inside(mx, my, x, y, cardWidth, rowHeight)) continue;

            int switchX = x + cardWidth - 47;
            int switchY = y + rowHeight - 22;
            int starX = x + cardWidth - 26;
            if (inside(mx, my, starX - 1, y + 2, 18, 18)) {
                String key = "favorite_" + feature.key();
                RuffyConfig.set(key, !RuffyConfig.get(key));
                return true;
            }

            if (inside(mx, my, switchX - 2, switchY - 2, 42, 22)) {
                RuffyConfig.set(feature.key(), !RuffyConfig.get(feature.key()));
                selectedKey = feature.key();
                return true;
            }

            selectedKey = feature.key();
            return true;
        }
        return false;
    }

    private boolean handlePagination(int mx, int my) {
        int y = panelY + panelH - 44;
        int right = mainX + mainW - 80;
        if (inside(mx, my, right, y, 22, 21)) {
            modulePage = Math.max(0, modulePage - 1);
            return true;
        }
        if (inside(mx, my, right + 58, y, 22, 21)) {
            List<RuffyFeature> features = filteredFeatures(
                    tab == Tab.HUD ? RuffyFeature.Section.HUD
                            : tab == Tab.WORLD ? RuffyFeature.Section.WORLD : null);
            int pages = Math.max(1, (features.size() + 7) / 8);
            modulePage = Math.min(pages - 1, modulePage + 1);
            return true;
        }
        return false;
    }

    private boolean handlePresetClick(int mx, int my) {
        int x = mainX;
        int y = panelY + 104;
        int gap = 10;
        int w = (mainW - gap) / 2;
        int h = 94;
        for (int i = 0; i < 4; i++) {
            int cx = x + (i % 2) * (w + gap);
            int cy = y + (i / 2) * (h + 12);
            if (inside(mx, my, cx, cy, w, h)) {
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
        int x = mainX;
        int y = panelY + 96;
        if (inside(mx, my, x + mainW - 78, y - 5, 70, 19)) {
            tab = Tab.PRESETS;
            return true;
        }
        if (inside(mx, my, x + mainW - 162, y - 5, 78, 19)) {
            tab = Tab.COSMETICS;
            return true;
        }
        int sw = (mainW - 24) / 5;
        for (int i = 0; i < PALETTE_NAMES.length; i++) {
            int cx = x + (i % 5) * (sw + 6);
            int cy = y + 16 + (i / 5) * 34;
            if (inside(mx, my, cx, cy, sw, 28)) {
                RuffyConfig.setInt("palette", i);
                return true;
            }
        }

        int settingsY = y + 87;
        if (inside(mx, my, x, settingsY + 11, mainW - 12, 15)) {
            RuffyConfig.setInt("hud_opacity",
                    12 + Math.round((mx - x) * 78.0F / (mainW - 12)));
            return true;
        }
        if (inside(mx, my, x, settingsY + 53, mainW - 12, 15)) {
            RuffyConfig.setInt("hud_scale",
                    75 + Math.round((mx - x) * 45.0F / (mainW - 12)));
            return true;
        }
        if (inside(mx, my, x, settingsY + 95, mainW - 12, 15)) {
            RuffyConfig.setInt("menu_opacity",
                    60 + Math.round((mx - x) * 40.0F / (mainW - 12)));
            return true;
        }
        if (inside(mx, my, x, settingsY + 137, mainW - 12, 15)) {
            RuffyConfig.setInt("effect_quality",
                    Math.round((mx - x) * 2.0F / (mainW - 12)));
            return true;
        }

        int smoothY = settingsY + 173;
        if (inside(mx, my, x + mainW - 52, smoothY - 2, 48, 24)) {
            RuffyConfig.set("smooth_animations", !RuffyConfig.get("smooth_animations"));
            return true;
        }
        if (inside(mx, my, x, smoothY + 28, mainW - 12, 15)) {
            RuffyConfig.setInt("animation_smoothness",
                    Math.round((mx - x) * 100.0F / (mainW - 12)));
            return true;
        }
        return false;
    }

    private boolean handleConfigClick(int mx, int my) {
        int x = mainX;
        int y = panelY + 110;
        if (inside(mx, my, x, y, mainW, 50)) {
            for (RuffyFeature feature : RuffyFeature.ALL) {
                RuffyConfig.set(feature.key(), defaultModuleState(feature.key()));
            }
            return true;
        }
        if (inside(mx, my, x, y + 64, mainW, 50)) {
            RuffyConfig.setInt("palette", 0);
            RuffyConfig.setInt("hud_opacity", 44);
            RuffyConfig.setInt("hud_scale", 96);
            RuffyConfig.setInt("menu_opacity", 94);
            RuffyConfig.setInt("effect_quality", 1);
            RuffyConfig.set("smooth_animations", true);
            RuffyConfig.setInt("animation_smoothness", 78);
            return true;
        }
        if (inside(mx, my, x, y + 128, mainW, 50)) {
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
