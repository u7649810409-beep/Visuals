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
        STYLE("Appearance", "Colors, motion and interface tuning", "◈"),
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
        panelW = Math.min(1180, Math.max(660, width - 28));
        panelW = Math.min(panelW, width - 12);
        panelH = Math.min(680, Math.max(410, height - 20));
        panelH = Math.min(panelH, height - 12);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        sidebarX = panelX + 12;
        sidebarY = panelY + 12;
        sidebarW = Math.max(132, Math.min(170, Math.round(panelW * 0.18F)));
        sidebarH = panelH - 24;
        mainX = sidebarX + sidebarW + 14;
        mainW = panelX + panelW - 14 - mainX;

        searchW = Math.min(226, Math.max(126, mainW * 34 / 100));
        searchX = mainX + (mainW - searchW) / 2;
        searchY = panelY + 14;

        filterY = panelY + 82;
        listTop = panelY + 121;
        cardGap = 9;
        cardWidth = (mainW - cardGap) / 2;
        int available = Math.max(150, panelY + panelH - 55 - listTop);
        rowHeight = Math.max(44, Math.min(62, (available - cardGap * 3) / 4));

        if (searchBox != null) {
            searchBox.setPosition(searchX + 25, searchY + 3);
            searchBox.setWidth(Math.max(72, searchW - 33));
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
        // Opaque, abstract nebula canvas: the live game scene is fully hidden.
        RuffyGalaxyUi.draw(g, width, height);
        g.fill(0, 0, width, height, 0x26040212);

        double time = RuffyAnimation.timeSeconds();
        for (int i = 0; i < 34; i++) {
            float pulse = RuffyAnimation.breathe(time, 0.075 + (i % 5) * 0.016,
                    i * 1.27F);
            int x = Math.floorMod(i * 127 + (int) (Math.sin(time * 0.055 + i) * 14.0),
                    Math.max(1, width));
            int y = Math.floorMod(i * 71 + (int) (Math.cos(time * 0.048 + i * 0.7) * 10.0),
                    Math.max(1, height));
            int color = i % 4 == 0 ? 0xFFFFD7FF : RuffyConfig.primaryColor();
            int alpha = Math.round(18 + pulse * 38);
            int size = i % 9 == 0 ? 3 : 2;
            fillRounded(g, x, y, size, size, withAlpha(color, alpha), size / 2);
        }

        // Sparse orbit dust in the corners adds depth without any game-world motifs.
        for (int ring = 0; ring < 2; ring++) {
            double rx = width * (ring == 0 ? 0.27 : 0.19);
            double ry = height * (ring == 0 ? 0.20 : 0.13);
            double cx = width * (ring == 0 ? 0.80 : 0.16);
            double cy = height * (ring == 0 ? 0.14 : 0.84);
            for (int i = 0; i < 54; i++) {
                double angle = (i / 54.0) * Math.PI * 2.0
                        + time * (ring == 0 ? 0.018 : -0.014);
                int px = (int) Math.round(cx + Math.cos(angle) * rx);
                int py = (int) Math.round(cy + Math.sin(angle) * ry);
                if (px < 0 || px >= width || py < 0 || py >= height) continue;
                fillRounded(g, px, py, 1, 1,
                        withAlpha(RuffyConfig.secondaryColor(), 25), 1);
            }
        }
    }

    private void drawWindow(GuiGraphics g) {
        int accent = RuffyConfig.primaryColor();
        int bodyAlpha = 218 + RuffyConfig.getInt("menu_opacity") / 7;
        int body = (Math.min(245, bodyAlpha) << 24) | 0x000B0F1D;

        RuffyRoundedUi.shadow(g, panelX - 2, panelY, panelW + 4, panelH + 4,
                RuffyRoundedUi.RADIUS_WINDOW, 42);
        fillRounded(g, panelX - 3, panelY - 3, panelW + 6, panelH + 6,
                withAlpha(RuffyConfig.secondaryColor(), 17), RuffyRoundedUi.RADIUS_WINDOW);
        fillRounded(g, panelX, panelY, panelW, panelH, body,
                RuffyRoundedUi.RADIUS_WINDOW);
        outlineRounded(g, panelX, panelY, panelW, panelH,
                withAlpha(accent, 125), RuffyRoundedUi.RADIUS_WINDOW);
        fillRounded(g, panelX + 28, panelY + 1, panelW - 56, 1,
                withAlpha(0xFFFFFFFF, 24), 1);
    }

    private void drawSidebar(GuiGraphics g, int mouseX, int mouseY) {
        RuffyRoundedUi.shadow(g, sidebarX, sidebarY, sidebarW, sidebarH,
                RuffyRoundedUi.RADIUS_PANEL, 28);
        fillRounded(g, sidebarX, sidebarY, sidebarW, sidebarH,
                0xEC080713, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, sidebarX, sidebarY, sidebarW, sidebarH,
                0x7D8C58D8, RuffyRoundedUi.RADIUS_PANEL);

        int logoX = sidebarX + (sidebarW - 48) / 2;
        int logoY = sidebarY + 14;
        fillRounded(g, logoX - 5, logoY - 4, 58, 58,
                withAlpha(RuffyConfig.primaryColor(), 24), RuffyRoundedUi.RADIUS_PANEL);
        fillRounded(g, logoX, logoY, 48, 48,
                withAlpha(RuffyConfig.primaryColor(), 58), RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, logoX, logoY, 48, 48,
                withAlpha(RuffyConfig.primaryColor(), 135), RuffyRoundedUi.RADIUS_PANEL);
        g.drawCenteredString(font, "R", logoX + 24, logoY + 13, WHITE);
        fillRounded(g, logoX + 7, logoY + 34, 34, 2,
                withAlpha(0xFFE8C9FF, 125), 1);
        g.drawCenteredString(font, "RUFFY VISUALS", sidebarX + sidebarW / 2,
                sidebarY + 68, WHITE);
        g.drawCenteredString(font, "GALAXY EDITION", sidebarX + sidebarW / 2,
                sidebarY + 81, RuffyConfig.primaryColor());

        int navX = sidebarX + 8;
        int navW = sidebarW - 16;
        drawSidebarLabel(g, "WORKSPACE", sidebarX + 12, panelY + 104);
        drawNavItem(g, Tab.HOME, navX, panelY + 116, navW, 24, mouseX, mouseY);
        drawNavItem(g, Tab.VISUALS, navX, panelY + 142, navW, 24, mouseX, mouseY);
        drawNavItem(g, Tab.STYLE, navX, panelY + 168, navW, 24, mouseX, mouseY);
        drawNavItem(g, Tab.COSMETICS, navX, panelY + 194, navW, 24, mouseX, mouseY);
        drawNavItem(g, Tab.HUD, navX, panelY + 220, navW, 24, mouseX, mouseY);

        drawSidebarLabel(g, "CONNECT", sidebarX + 12, panelY + 257);
        drawNavItem(g, Tab.MUSIC, navX, panelY + 269, navW, 24, mouseX, mouseY);

        drawSidebarLabel(g, "PREFERENCES", sidebarX + 12, panelY + 306);
        drawNavItem(g, Tab.CONFIGS, navX, panelY + 318, navW, 24, mouseX, mouseY);
        drawNavItem(g, Tab.SETUP, navX, panelY + 344, navW, 24, mouseX, mouseY);

        int bottomY = sidebarY + sidebarH - 41;
        fillRounded(g, sidebarX + 8, bottomY, navW, 33,
                0x9A111424, RuffyRoundedUi.RADIUS_CONTROL);
        fillRounded(g, sidebarX + 16, bottomY + 9, 6, 6,
                0xFF45DB91, 3);
        g.drawString(font, "SYSTEM ONLINE", sidebarX + 29,
                bottomY + 6, WHITE, false);
        g.drawString(font, "LOCAL PROFILE", sidebarX + 16,
                bottomY + 19, DIM, false);
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
            g.drawString(font, "Ruffy", mainX, panelY + 12, WHITE, false);
            g.drawString(font, "Visuals", mainX + font.width("Ruffy ") - 1,
                    panelY + 12, RuffyConfig.primaryColor(), false);
            g.drawString(font, "GALAXY STUDIO  ·  2.7", mainX, panelY + 28,
                    MUTED, false);
        } else {
            String title = switch (tab) {
                case HOME -> "Home";
                case VISUALS -> "Modules";
                case WORLD -> "World effects";
                case HUD -> "HUD studio";
                case PRESETS -> "Visual profiles";
                case COSMETICS -> "Cosmetics";
                case STYLE -> "Appearance";
                case MUSIC -> "Spotify";
                case CONFIGS -> "Settings";
                case SETUP -> "About";
            };
            g.drawString(font, "RUFFY VISUALS  /  GALAXY EDITION",
                    mainX, panelY + 15, DIM, false);
            g.drawString(font, title, mainX, panelY + 29, WHITE, false);
            g.drawString(font, tab.subtitle, mainX, panelY + 42, MUTED, false);
        }

        fillRounded(g, searchX, searchY, searchW, 25,
                0xC6111225, RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, searchX, searchY, searchW, 25,
                0x666D67A8, RuffyRoundedUi.RADIUS_CONTROL);
        g.drawString(font, "⌕", searchX + 8, searchY + 8, DIM, false);
        if (searchBox != null) {
            searchBox.setVisible(true);
            searchBox.setPosition(searchX + 25, searchY + 3);
            searchBox.setWidth(Math.max(72, searchW - 33));
        }

        int controlsX = mainX + mainW - 45;
        int controlsY = panelY + 14;
        for (int i = 0; i < 3; i++) {
            int cx = controlsX + i * 15;
            int controlColor = i == 2 ? 0xFFFF6487 : 0xFF8E78C5;
            fillRounded(g, cx, controlsY, 12, 12, 0xC20D0B1B, 6);
            outlineRounded(g, cx, controlsY, 12, 12,
                    withAlpha(controlColor, i == 2 ? 172 : 94), 6);
        }
        g.drawCenteredString(font, "−", controlsX + 6, controlsY + 1, MUTED);
        g.drawCenteredString(font, "□", controlsX + 21, controlsY + 1, MUTED);
        g.drawCenteredString(font, "×", controlsX + 36, controlsY + 1, 0xFFFFA8BD);
    }


    private void prepareHomeLayout() {
        int gap = 9;
        homeCenterX = mainX;
        homeCenterW = Math.max(230, (mainW - gap) * 63 / 100);
        homeRightX = homeCenterX + homeCenterW + gap;
        homeRightW = Math.max(150, mainX + mainW - homeRightX);

        homeHeroY = panelY + 64;
        int availableHeight = Math.max(160, panelH - 124);
        homeHeroH = Math.max(126, Math.min(178, Math.round(availableHeight * 0.43F)));
        homeModulesY = homeHeroY + homeHeroH + 8;
        homeBottomY = panelY + panelH - 49;
        homeModulesH = Math.max(92, homeBottomY - homeModulesY - 4);

        homeSpotifyY = homeHeroY;
        homeSpotifyH = homeHeroH;
        int lowerWidth = Math.max(180, mainW - gap * 2);
        homeModulesW = lowerWidth * 45 / 100;
        homeQuickW = lowerWidth * 30 / 100;
        homeLyricsW = lowerWidth - homeModulesW - homeQuickW;
        homeQuickX = mainX + homeModulesW + gap;
        homeLyricsX = homeQuickX + homeQuickW + gap;
        homeAppearanceY = homeModulesY;
        homeAppearanceH = homeModulesH;

        homePerfW = Math.max(130, Math.min(220, mainW * 30 / 100));
        homeSocialX = mainX + mainW - 4 * 27 - 3 * 5;
        homeVersionX = mainX + homePerfW + 8;
        homeVersionW = Math.max(95, homeSocialX - homeVersionX - 8);
    }

    private void drawHomeDashboard(GuiGraphics g, int mouseX, int mouseY) {
        prepareHomeLayout();
        drawHomeHero(g, mouseX, mouseY);
        drawHomeModules(g, mouseX, mouseY);
        drawHomeSpotify(g, mouseX, mouseY);
        drawHomeAppearance(g, mouseX, mouseY);
        drawHomeLyrics(g, mouseX, mouseY);
        drawHomeBottom(g, mouseX, mouseY);
    }

    private void drawHomeHero(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeCenterX;
        int y = homeHeroY;
        int w = homeCenterW;
        int h = homeHeroH;

        fillRounded(g, x, y, w, h, 0xE10B071D, RuffyRoundedUi.RADIUS_PANEL);
        RuffyDashboardArt.drawHero(g, x + 1, y + 1, w - 2, h - 2);
        fillRounded(g, x + 1, y + 1, Math.max(80, w / 2), h - 2,
                0x56100925, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h,
                withAlpha(RuffyConfig.primaryColor(), 170), RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x + 2, y + 2, w - 4, h - 4,
                withAlpha(0xFFE8C9FF, 48), RuffyRoundedUi.RADIUS_PANEL);

        int textX = x + 18;
        g.drawString(font, "Welcome back!", textX, y + 22, WHITE, false);
        g.drawString(font, "Your visual universe.", textX, y + 42, MUTED, false);
        g.drawString(font, "Make it yours with Ruffy Visuals.", textX,
                y + 56, 0xFFE8C9FF, false);

        int buttonY = y + h - 34;
        int buttonW = Math.min(132, Math.max(106, w / 2));
        drawButton(g, textX, buttonY, buttonW, 24,
                "Explore Modules  →", ButtonStyle.PRIMARY,
                inside(mouseX, mouseY, textX, buttonY, buttonW, 24), false);
    }

    private void drawHomeModules(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeCenterX;
        int y = homeModulesY;
        int w = homeModulesW;
        int h = homeModulesH;
        fillRounded(g, x, y, w, h, 0xE10A0818, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, 0x765C3F9D, RuffyRoundedUi.RADIUS_PANEL);
        g.drawString(font, "✦  Featured Modules", x + 12, y + 10, WHITE, false);
        g.drawString(font, "Your visual toolkit", x + 12, y + 23, MUTED, false);
        drawButton(g, x + w - 70, y + 8, 58, 20, "See all  →",
                ButtonStyle.GHOST,
                inside(mouseX, mouseY, x + w - 70, y + 8, 58, 20), false);

        String[] keys = {"prism_core", "nebula_particles", "orbit_particles", "swing_animation"};
        String[] titles = {"Crystal Core", "Nebula", "Orbit Rings", "Swing Motion"};
        String[] descriptions = {"3D centerpiece", "Ambient particles", "Orbiting accents", "Smooth motion"};
        String[] icons = {"◇", "✧", "◎", "⌁"};
        int gap = 7;
        int left = x + 11;
        int top = y + 37;
        int tileW = Math.max(68, (w - 22 - gap) / 2);
        int tileH = Math.max(42, (h - 47 - gap) / 2);

        for (int i = 0; i < keys.length; i++) {
            int column = i % 2;
            int row = i / 2;
            int tx = left + column * (tileW + gap);
            int ty = top + row * (tileH + gap);
            boolean enabled = RuffyConfig.get(keys[i]);
            boolean hovered = inside(mouseX, mouseY, tx, ty, tileW, tileH);
            float hover = RuffyAnimation.animate("home.feature.hover." + keys[i],
                    hovered ? 1.0F : 0.0F);
            int background = RuffyAnimation.blendColor(0xAA171329, 0xE3231B3B, hover * 0.9F);
            fillRounded(g, tx, ty, tileW, tileH, background,
                    RuffyRoundedUi.RADIUS_CONTROL);
            outlineRounded(g, tx, ty, tileW, tileH,
                    withAlpha(enabled ? RuffyConfig.primaryColor() : 0xFF877AAB,
                            enabled ? 150 : 52), RuffyRoundedUi.RADIUS_CONTROL);

            fillRounded(g, tx + 8, ty + 8, 24, 24,
                    withAlpha(RuffyConfig.primaryColor(), enabled ? 58 : 24),
                    RuffyRoundedUi.RADIUS_CONTROL);
            g.drawCenteredString(font, icons[i], tx + 20, ty + 15,
                    enabled ? 0xFFEAD8FF : MUTED);

            int textX = tx + 39;
            int textW = Math.max(32, tileW - 80);
            g.drawString(font, trimToWidth(titles[i], textW), textX,
                    ty + 8, WHITE, false);
            g.drawString(font, trimToWidth(descriptions[i], textW), textX,
                    ty + 21, MUTED, false);

            int switchX = tx + tileW - 38;
            int switchY = ty + 8;
            drawSwitch(g, switchX, switchY, enabled, 30, 16,
                    "home.feature." + keys[i]);
        }
    }

    private void drawHomeSpotify(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeRightX;
        int y = homeSpotifyY;
        int w = homeRightW;
        int h = homeSpotifyH;
        RuffyRoundedUi.shadow(g, x, y, w, h, RuffyRoundedUi.RADIUS_PANEL, 24);
        fillRounded(g, x, y, w, h, 0xE0080917, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, 0x865E45AF, RuffyRoundedUi.RADIUS_PANEL);

        fillRounded(g, x + 11, y + 9, 22, 22, 0xFF1ED760, 11);
        g.drawCenteredString(font, "≋", x + 22, y + 14, 0xFF092010);
        g.drawString(font, "Spotify", x + 40, y + 11, WHITE, false);
        boolean connected = RuffySpotify.isConnected();
        int statusColor = connected ? 0xFF42E69A : 0xFFFFC078;
        String statusLabel = connected ? "Connected" : "Connect";
        int statusX = x + w - font.width(statusLabel) - 22;
        fillRounded(g, statusX - 10, y + 17, 5, 5, statusColor, 3);
        g.drawString(font, statusLabel, statusX, y + 12, statusColor, false);
        drawIconButton(g, x + w - 22, y + 9, 16, 17, "⚙", MUTED,
                inside(mouseX, mouseY, x + w - 22, y + 9, 16, 17));

        int artSize = Math.min(44, Math.max(34, h / 3));
        int artX = x + 12;
        int artY = y + 37;
        drawAlbumArt(g, artX, artY, artSize);
        int infoX = artX + artSize + 10;
        int infoW = Math.max(40, x + w - 12 - infoX);
        g.drawString(font, trimToWidth(RuffySpotify.getTrackName(), infoW),
                infoX, artY + 5, WHITE, false);
        g.drawString(font, trimToWidth(RuffySpotify.getArtistName(), infoW),
                infoX, artY + 19, MUTED, false);
        if (RuffyConfig.get("music_visualizer")) {
            drawVisualizerBars(g, infoX, artY + 18, infoW);
        } else {
            g.drawString(font, connected ? "PLAYBACK READY" : "LINK YOUR ACCOUNT",
                    infoX, artY + 31, DIM, false);
        }

        int progressY = y + h - 47;
        int progressX = x + 12;
        int progressW = w - 24;
        long duration = RuffySpotify.getDurationMs();
        long position = RuffySpotify.getProgressMs();
        float ratio = duration <= 0L ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, position / (float) duration));
        fillRounded(g, progressX, progressY, progressW, 3, 0xFF332C46, 2);
        fillRounded(g, progressX, progressY, Math.round(progressW * ratio), 3,
                RuffyConfig.primaryColor(), 2);
        g.drawString(font, formatTime(position), progressX, progressY + 5, DIM, false);
        String total = formatTime(duration);
        g.drawString(font, total, progressX + progressW - font.width(total),
                progressY + 5, DIM, false);

        int controlsY = y + h - 25;
        int center = x + w / 2;
        drawIconButton(g, center - 36, controlsY, 22, 20, "|◀", WHITE,
                inside(mouseX, mouseY, center - 36, controlsY, 22, 20));
        fillRounded(g, center - 11, controlsY - 1, 23, 22,
                RuffyConfig.primaryColor(), 11);
        fillRounded(g, center - 13, controlsY - 3, 27, 26,
                withAlpha(RuffyConfig.primaryColor(), 28), 13);
        g.drawCenteredString(font, RuffySpotify.isPlaying() ? "Ⅱ" : "▶",
                center, controlsY + 6, WHITE);
        drawIconButton(g, center + 15, controlsY, 22, 20, "▶|", WHITE,
                inside(mouseX, mouseY, center + 15, controlsY, 22, 20));
    }

    private void drawAlbumArt(GuiGraphics g, int x, int y, int size) {
        fillRounded(g, x, y, size, size, 0xFF26104F, RuffyRoundedUi.RADIUS_CONTROL);
        g.fill(x + 1, y + size / 2, x + size - 1, y + size - 1, 0xFF4A1684);
        g.fill(x + 4, y + size / 3, x + size - 4, y + size / 2, 0xFF8B30D2);
        g.fill(x + size / 2 - 4, y + 6, x + size / 2 + 5, y + size - 6,
                withAlpha(RuffyConfig.primaryColor(), 165));
        for (int i = 0; i < 7; i++) {
            int sx = x + 4 + Math.floorMod(i * 13, Math.max(1, size - 8));
            int sy = y + 4 + Math.floorMod(i * 17, Math.max(1, size - 8));
            fillRounded(g, sx, sy, 2, 2, 0xFFEBD8FF, 1);
        }
        outlineRounded(g, x, y, size, size, 0x778A5CE6, RuffyRoundedUi.RADIUS_CONTROL);
    }

    private void drawVisualizerBars(GuiGraphics g, int x, int y, int width) {
        double time = RuffyAnimation.timeSeconds();
        int bars = Math.max(6, Math.min(18, width / 4));
        int gap = 2;
        int barW = Math.max(1, (width - (bars - 1) * gap) / bars);
        for (int i = 0; i < bars; i++) {
            float pulse = 0.25F + 0.75F * RuffyAnimation.breathe(time,
                    0.55 + (i % 4) * 0.12, i * 0.7F);
            int height = Math.max(3, Math.round(5 + pulse * 15));
            fillRounded(g, x + i * (barW + gap), y + 19 - height,
                    barW, height, withAlpha(RuffyConfig.primaryColor(),
                            150 + Math.round(pulse * 105)), 1);
        }
    }

    private void drawHomeAppearance(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeQuickX;
        int y = homeAppearanceY;
        int w = homeQuickW;
        fillRounded(g, x, y, w, homeAppearanceH, 0xE1090918, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, homeAppearanceH, 0x765B3D96, RuffyRoundedUi.RADIUS_PANEL);

        g.drawString(font, "⚙  Quick Settings", x + 11, y + 10, WHITE, false);
        g.drawString(font, "WAVE", x + w - 79, y + 12, MUTED, false);
        drawSwitch(g, x + w - 42, y + 8, RuffyConfig.get("music_visualizer"),
                32, 18, "home.music.visualizer");

        int themeY = y + 29;
        g.drawString(font, "Theme", x + 12, themeY + 6, MUTED, false);
        int themeW = Math.min(88, w - 78);
        int themeX = x + w - themeW - 10;
        fillRounded(g, themeX, themeY, themeW, 21, 0xA81E1734,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, themeX, themeY, themeW, 21,
                0x756C50A8, RuffyRoundedUi.RADIUS_CONTROL);
        fillRounded(g, themeX + 7, themeY + 7, 7, 7, RuffyConfig.primaryColor(), 4);
        g.drawString(font, trimToWidth(paletteName(RuffyConfig.getInt("palette")),
                Math.max(28, themeW - 30)), themeX + 19, themeY + 6, WHITE, false);
        g.drawString(font, "⌄", themeX + themeW - 11, themeY + 6, MUTED, false);

        int opacityY = y + 51;
        g.drawString(font, "Opacity", x + 12, opacityY + 4, MUTED, false);
        int sliderX = x + 66;
        int sliderW = Math.max(32, w - 111);
        drawSlider(g, sliderX, opacityY, sliderW,
                RuffyConfig.getInt("menu_opacity"), 60, 100);
        String opacity = RuffyConfig.getInt("menu_opacity") + "%";
        g.drawString(font, opacity, x + w - font.width(opacity) - 9,
                opacityY + 3, WHITE, false);

        int qualityY = y + 73;
        g.drawString(font, "FX Quality", x + 12, qualityY + 5, MUTED, false);
        int qGap = 3;
        int qW = Math.max(31, Math.min(39, (w - 112) / 3));
        int qX = x + w - (qW * 3 + qGap * 2) - 9;
        String[] qualityLabels = {"LOW", "MID", "HIGH"};
        for (int i = 0; i < 3; i++) {
            boolean selected = RuffyConfig.getInt("effect_quality") == i;
            int bx = qX + i * (qW + qGap);
            drawButton(g, bx, qualityY, qW, 20, qualityLabels[i],
                    selected ? ButtonStyle.SEGMENT : ButtonStyle.GHOST,
                    inside(mouseX, mouseY, bx, qualityY, qW, 20), selected);
        }
    }

    private void drawHomeLyrics(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeLyricsX;
        int y = homeAppearanceY;
        int w = homeLyricsW;
        int h = homeAppearanceH;
        fillRounded(g, x, y, w, h, 0xE1090919, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, 0x785C3F9D, RuffyRoundedUi.RADIUS_PANEL);
        g.drawString(font, "♫  Lyrics", x + 11, y + 10,
                RuffyConfig.primaryColor(), false);
        g.drawString(font, "NOW PLAYING", x + 11, y + 23, DIM, false);

        int artY = y + 32;
        int artH = Math.min(48, Math.max(30, h / 4));
        RuffyDashboardArt.drawHero(g, x + 9, artY, w - 18, artH);
        String source = RuffySpotify.getLyrics();
        String[] lines = source == null ? new String[0] : source.split("\\R");
        int textY = artY + artH + 6;
        if (lines.length == 0 || lines[0].isBlank()) {
            String hint = RuffySpotify.isConnected()
                    ? "Lyrics not available for this track."
                    : "Connect Spotify to view lyrics.";
            g.drawCenteredString(font, trimToWidth(hint, w - 18),
                    x + w / 2, textY, MUTED);
        } else {
            int maxShown = Math.min(3, Math.max(1, (h - 91) / 13));
            maxShown = Math.min(maxShown, lines.length);
            for (int i = 0; i < maxShown; i++) {
                g.drawCenteredString(font, trimToWidth(lines[i], w - 20),
                        x + w / 2, textY + i * 13, i == 0 ? WHITE : MUTED);
            }
        }
        String track = trimToWidth(RuffySpotify.getTrackName(), w - 20);
        g.drawString(font, track, x + 10, y + h - 14, DIM, false);
        if (inside(mouseX, mouseY, x, y, w, h)) {
            outlineRounded(g, x + 1, y + 1, w - 2, h - 2,
                    withAlpha(RuffyConfig.primaryColor(), 65), RuffyRoundedUi.RADIUS_PANEL);
        }
    }

    private void drawHomeBottom(GuiGraphics g, int mouseX, int mouseY) {
        int y = homeBottomY + 7;
        int h = 32;

        fillRounded(g, mainX, y, homePerfW, h, 0xE1090818,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, mainX, y, homePerfW, h, BORDER,
                RuffyRoundedUi.RADIUS_CONTROL);
        String fps = Minecraft.getInstance().getFps() + " FPS";
        g.drawString(font, "ϟ", mainX + 10, y + 8,
                RuffyConfig.primaryColor(), false);
        g.drawString(font, "Performance", mainX + 25, y + 6, WHITE, false);
        g.drawString(font, fps, mainX + 25, y + 18,
                RuffyConfig.primaryColor(), false);

        fillRounded(g, homeVersionX, y, homeVersionW, h, 0xE1090819,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, homeVersionX, y, homeVersionW, h,
                withAlpha(RuffyConfig.primaryColor(), 140), RuffyRoundedUi.RADIUS_CONTROL);
        g.drawString(font, "♛", homeVersionX + 9, y + 8, 0xFFFFD45E, false);
        g.drawString(font, trimToWidth("Ruffy Visuals 2.7.0",
                Math.max(1, homeVersionW - 34)),
                homeVersionX + 24, y + 6, WHITE, false);
        g.drawString(font, trimToWidth("A cleaner visual universe.",
                Math.max(1, homeVersionW - 34)),
                homeVersionX + 24, y + 18, MUTED, false);

        String[] actions = {"⌂", "♫", "⚙", "ⓘ"};
        for (int i = 0; i < actions.length; i++) {
            int sx = homeSocialX + i * 29;
            boolean hover = inside(mouseX, mouseY, sx, y + 4, 24, 24);
            fillRounded(g, sx, y + 4, 24, 24,
                    hover ? 0xE3261A42 : 0xD20D1020, 12);
            outlineRounded(g, sx, y + 4, 24, 24,
                    withAlpha(RuffyConfig.primaryColor(), hover ? 185 : 105), 12);
            g.drawCenteredString(font, actions[i], sx + 12, y + 12,
                    i == 1 ? 0xFF7DE5B1 : WHITE);
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
        int controlsX = mainX + mainW - 45;
        int controlsY = panelY + 14;
        if (inside(mx, my, controlsX + 30, controlsY, 12, 12)) {
            onClose();
            return true;
        }

        int heroButtonX = homeCenterX + 18;
        int heroButtonY = homeHeroY + homeHeroH - 34;
        int heroButtonW = Math.min(132, Math.max(106, homeCenterW / 2));
        if (inside(mx, my, heroButtonX, heroButtonY, heroButtonW, 24)) {
            tab = Tab.VISUALS;
            modulePage = 0;
            return true;
        }

        int moduleSeeX = homeCenterX + homeModulesW - 70;
        if (inside(mx, my, moduleSeeX, homeModulesY + 8, 58, 20)) {
            tab = Tab.VISUALS;
            modulePage = 0;
            return true;
        }

        String[] keys = {"prism_core", "nebula_particles", "orbit_particles", "swing_animation"};
        int gap = 7;
        int left = homeCenterX + 11;
        int top = homeModulesY + 37;
        int tileW = Math.max(68, (homeModulesW - 22 - gap) / 2);
        int tileH = Math.max(42, (homeModulesH - 47 - gap) / 2);
        for (int i = 0; i < keys.length; i++) {
            int column = i % 2;
            int row = i / 2;
            int tx = left + column * (tileW + gap);
            int ty = top + row * (tileH + gap);
            int switchX = tx + tileW - 38;
            int switchY = ty + 8;
            if (inside(mx, my, switchX - 2, switchY - 2, 34, 20)) {
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
        int controlsYPlayer = spotifyY + homeSpotifyH - 25;
        if (inside(mx, my, center - 36, controlsYPlayer, 22, 20)) {
            RuffySpotify.previousTrack();
            return true;
        }
        if (inside(mx, my, center - 13, controlsYPlayer - 3, 27, 26)) {
            RuffySpotify.togglePlayback();
            return true;
        }
        if (inside(mx, my, center + 15, controlsYPlayer, 22, 20)) {
            RuffySpotify.nextTrack();
            return true;
        }
        if (inside(mx, my, spotifyX + spotifyW - 24, spotifyY + 7, 18, 20)
                || inside(mx, my, spotifyX, spotifyY, spotifyW, 32)) {
            if (minecraft != null) minecraft.setScreen(new RuffySpotifyScreen(this));
            return true;
        }

        // Appearance / quick settings card.
        int quickX = homeQuickX;
        int quickY = homeAppearanceY;
        int quickW = homeQuickW;
        int themeY = quickY + 35;
        int themeW = Math.min(88, quickW - 78);
        int themeX = quickX + quickW - themeW - 10;
        if (inside(mx, my, themeX, themeY, themeW, 21)) {
            RuffyConfig.setInt("palette", (RuffyConfig.getInt("palette") + 1) % 10);
            return true;
        }

        int sliderX = quickX + 66;
        int sliderW = Math.max(32, quickW - 111);
        int opacityY = quickY + 51;
        if (inside(mx, my, sliderX - 4, opacityY, sliderW + 8, 16)) {
            int value = 60 + Math.round((mx - sliderX) * 40.0F / Math.max(1, sliderW));
            RuffyConfig.setInt("menu_opacity", value);
            return true;
        }

        int qualityY = quickY + 73;
        int qGap = 3;
        int qW = Math.max(31, Math.min(39, (quickW - 112) / 3));
        int qX = quickX + quickW - (qW * 3 + qGap * 2) - 9;
        for (int i = 0; i < 3; i++) {
            int bx = qX + i * (qW + qGap);
            if (inside(mx, my, bx, qualityY, qW, 20)) {
                RuffyConfig.setInt("effect_quality", i);
                return true;
            }
        }

        int visualizerY = quickY + 8;
        if (inside(mx, my, quickX + quickW - 44, visualizerY - 2, 36, 22)) {
            RuffyConfig.set("music_visualizer", !RuffyConfig.get("music_visualizer"));
            return true;
        }

        // Lyrics opens the full player and lyrics screen.
        if (inside(mx, my, homeLyricsX, homeAppearanceY,
                homeLyricsW, homeAppearanceH)) {
            if (minecraft != null) minecraft.setScreen(new RuffySpotifyScreen(this));
            return true;
        }

        // Footer shortcuts are real navigation destinations.
        int footerY = homeBottomY + 7;
        for (int i = 0; i < 4; i++) {
            int sx = homeSocialX + i * 29;
            if (!inside(mx, my, sx, footerY + 4, 24, 24)) continue;
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
        g.drawString(font, "Control playback and keep lyrics inside Minecraft.",
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
        int x = sidebarX + 8;
        int w = sidebarW - 16;
        int[] ys = {
                panelY + 116, panelY + 142, panelY + 168,
                panelY + 194, panelY + 220, panelY + 269,
                panelY + 318, panelY + 344
        };
        Tab[] tabs = {
                Tab.HOME, Tab.VISUALS, Tab.COSMETICS, Tab.STYLE,
                Tab.HUD, Tab.MUSIC, Tab.CONFIGS, Tab.SETUP
        };
        for (int i = 0; i < tabs.length; i++) {
            if (!inside(mx, my, x, ys[i], w, 26)) continue;
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
