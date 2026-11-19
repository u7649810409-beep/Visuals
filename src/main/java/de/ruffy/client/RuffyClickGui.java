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
 * Ruffy Visuals 2.5 — Galaxy dashboard inspired by the approved visual reference.
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

    // Responsive home-dashboard geometry, derived from the same app shell.
    private int homeCenterX, homeCenterW, homeRightX, homeRightW;
    private int homeHeroY, homeHeroH, homeModulesY, homeModulesH;
    private int homeSpotifyY, homeSpotifyH, homeAppearanceY, homeAppearanceH;
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
        panelW = Math.min(720, Math.max(540, width - 24));
        panelW = Math.min(panelW, width - 12);
        panelH = Math.min(430, Math.max(380, height - 24));
        panelH = Math.min(panelH, height - 12);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        sidebarX = panelX + 12;
        sidebarY = panelY + 12;
        sidebarW = Math.max(112, Math.min(126, panelW / 5));
        sidebarH = panelH - 24;
        mainX = sidebarX + sidebarW + 16;
        mainW = panelX + panelW - 16 - mainX;

        searchW = Math.min(158, Math.max(112, mainW - 210));
        searchX = mainX + mainW - searchW - 42;
        searchY = panelY + 13;

        filterY = panelY + 80;
        listTop = panelY + 119;
        cardGap = 8;
        cardWidth = (mainW - cardGap) / 2;
        int available = Math.max(150, panelY + panelH - 48 - listTop);
        rowHeight = Math.max(44, Math.min(58, (available - cardGap * 3) / 4));

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
            case STYLE -> drawStyle(g, mouseX, mouseY);
            case CONFIGS -> drawConfigs(g, mouseX, mouseY);
            case SETUP -> drawSetup(g);
            case MUSIC -> drawMusicLanding(g, mouseX, mouseY);
        }

        drawFooter(g);
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawBackdrop(GuiGraphics g) {
        RuffyGalaxyUi.draw(g, width, height);
        g.fill(0, 0, width, height, 0x66060913);
        drawMinecraftBackdrop(g);

        double time = RuffyAnimation.timeSeconds();
        for (int i = 0; i < 16; i++) {
            float pulse = RuffyAnimation.breathe(time, 0.10 + (i % 4) * 0.018,
                    i * 1.41F);
            int x = Math.floorMod(i * 149 + (int) (Math.sin(time * 0.07 + i) * 12.0),
                    Math.max(1, width));
            int y = Math.floorMod(i * 89 + (int) (Math.cos(time * 0.06 + i * 0.6) * 10.0),
                    Math.max(1, height));
            int color = i % 4 == 0 ? RuffyConfig.secondaryColor() : RuffyConfig.primaryColor();
            int alpha = Math.round(18 + pulse * 38);
            fillRounded(g, x, y, 2, 2, withAlpha(color, alpha), 1);
        }
    }

    /** Small Minecraft-style block landscape and water reflections behind the dashboard. */
    private void drawMinecraftBackdrop(GuiGraphics g) {
        int horizon = height * 61 / 100;
        int waterLine = height * 72 / 100;
        g.fill(0, horizon, width, height, 0xB5081020);
        g.fill(0, waterLine, width, height, 0x50213B71);

        // Distant forest silhouettes.
        for (int i = 0; i < 18; i++) {
            int px = Math.floorMod(i * 97 + 31, Math.max(1, width));
            int trunkW = 5 + (i % 3) * 2;
            int trunkH = 15 + (i % 5) * 7;
            int base = horizon + 5 + (i % 4) * 3;
            int color = i % 2 == 0 ? 0xB5070D20 : 0xB50B1028;
            g.fill(px, base - trunkH, px + trunkW, height, color);
            g.fill(px - 8, base - trunkH - 11, px + trunkW + 8,
                    base - trunkH + 5, color);
            g.fill(px - 5, base - trunkH - 18, px + trunkW + 5,
                    base - trunkH - 7, color);
        }

        // Dark grass blocks at the foreground edges leave the water visible in the middle.
        int groundY = height * 88 / 100;
        g.fill(0, groundY, width, height, 0xD0060B16);
        for (int i = 0; i < 30; i++) {
            int px = Math.floorMod(i * 73 + 9, Math.max(1, width));
            int py = groundY - Math.floorMod(i * 17, Math.max(1, height / 14));
            int size = 4 + i % 4;
            g.fill(px, py, Math.min(width, px + size), Math.min(height, py + size),
                    i % 3 == 0 ? 0xA21B2544 : 0xA20D1830);
        }

        // Violet reflections on the lake.
        for (int i = 0; i < 22; i++) {
            int rw = 18 + (i % 5) * 11;
            int rx = Math.floorMod(i * 83 + 23, Math.max(1, width));
            int ry = waterLine + 3 + Math.floorMod(i * 13, Math.max(1, height / 6));
            int alpha = 18 + (i % 4) * 10;
            g.fill(rx, ry, Math.min(width, rx + rw), Math.min(height, ry + 2),
                    withAlpha(RuffyConfig.primaryColor(), alpha));
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
        fillRounded(g, sidebarX, sidebarY, sidebarW, sidebarH,
                0xD00A0B1A, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, sidebarX, sidebarY, sidebarW, sidebarH,
                0x557A55C6, RuffyRoundedUi.RADIUS_PANEL);

        int logoX = sidebarX + (sidebarW - 42) / 2;
        int logoY = sidebarY + 13;
        fillRounded(g, logoX, logoY, 42, 42,
                withAlpha(RuffyConfig.primaryColor(), 48), RuffyRoundedUi.RADIUS_PANEL);
        fillRounded(g, logoX + 5, logoY + 5, 32, 32,
                withAlpha(RuffyConfig.primaryColor(), 205), RuffyRoundedUi.RADIUS_PANEL);
        g.drawCenteredString(font, "R", logoX + 21, logoY + 15, WHITE);
        g.drawCenteredString(font, "RUFFY VISUALS", sidebarX + sidebarW / 2,
                sidebarY + 61, WHITE);
        g.drawCenteredString(font, "GALAXY EDITION", sidebarX + sidebarW / 2,
                sidebarY + 73, RuffyConfig.primaryColor());

        int navX = sidebarX + 8;
        int navW = sidebarW - 16;
        drawSidebarLabel(g, "MENU", sidebarX + 12, panelY + 99);
        drawNavItem(g, Tab.HOME, navX, panelY + 111, navW, 23, mouseX, mouseY);
        drawNavItem(g, Tab.VISUALS, navX, panelY + 136, navW, 23, mouseX, mouseY);
        drawNavItem(g, Tab.STYLE, navX, panelY + 161, navW, 23, mouseX, mouseY);
        drawNavItem(g, Tab.HUD, navX, panelY + 186, navW, 23, mouseX, mouseY);

        drawSidebarLabel(g, "CONNECT", sidebarX + 12, panelY + 217);
        drawNavItem(g, Tab.MUSIC, navX, panelY + 229, navW, 23, mouseX, mouseY);

        drawSidebarLabel(g, "PREFERENCES", sidebarX + 12, panelY + 260);
        drawNavItem(g, Tab.CONFIGS, navX, panelY + 272, navW, 23, mouseX, mouseY);
        drawNavItem(g, Tab.SETUP, navX, panelY + 297, navW, 23, mouseX, mouseY);

        int bottomY = sidebarY + sidebarH - 38;
        fillRounded(g, sidebarX + 8, bottomY, navW, 31,
                0x6A171B30, RuffyRoundedUi.RADIUS_CONTROL);
        fillRounded(g, sidebarX + 16, bottomY + 9, 6, 6,
                0xFF45DB91, 3);
        g.drawString(font, "ONLINE", sidebarX + 29,
                bottomY + 6, WHITE, false);
        g.drawString(font, "1.21.11  ·  FABRIC", sidebarX + 16,
                bottomY + 18, DIM, false);
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
            g.drawString(font, "RUFFY VISUALS", mainX, panelY + 15, WHITE, false);
            g.drawString(font, "GALAXY EDITION  ✦", mainX, panelY + 29,
                    RuffyConfig.primaryColor(), false);
        } else {
            String title = switch (tab) {
                case HOME -> "Home";
                case VISUALS -> "Modules";
                case WORLD -> "World effects";
                case HUD -> "HUD studio";
                case PRESETS -> "Visual profiles";
                case STYLE -> "Visuals";
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

        int controlsX = mainX + mainW - 36;
        int controlsY = panelY + 14;
        for (int i = 0; i < 3; i++) {
            int cx = controlsX + i * 13;
            fillRounded(g, cx, controlsY, 11, 11,
                    0xB50D1020, 6);
            outlineRounded(g, cx, controlsY, 11, 11,
                    0x77785EBA, 6);
        }
        g.drawCenteredString(font, "−", controlsX + 5, controlsY + 1, MUTED);
        g.drawCenteredString(font, "□", controlsX + 18, controlsY + 1, MUTED);
        g.drawCenteredString(font, "×", controlsX + 31, controlsY + 1, WHITE);
    }


    private void prepareHomeLayout() {
        int gap = 9;
        homeRightW = Math.max(154, Math.min(210, mainW * 38 / 100));
        homeCenterX = mainX;
        homeCenterW = Math.max(160, mainW - homeRightW - gap);
        homeRightX = homeCenterX + homeCenterW + gap;

        homeHeroY = panelY + 67;
        homeHeroH = Math.max(118, Math.min(146, Math.round(panelH * 0.34F)));
        homeModulesY = homeHeroY + homeHeroH + 8;
        homeBottomY = panelY + panelH - 48;
        homeModulesH = Math.max(96, homeBottomY - homeModulesY - 8);

        homeSpotifyY = homeHeroY;
        homeSpotifyH = Math.max(158, Math.min(190, Math.round(panelH * 0.43F)));
        homeAppearanceY = homeSpotifyY + homeSpotifyH + 8;
        homeAppearanceH = Math.max(88, homeBottomY - homeAppearanceY - 8);

        homePerfW = Math.max(126, Math.min(196, mainW * 39 / 100));
        homeSocialX = mainX + mainW - 4 * 24 - 3 * 5;
        homeVersionX = mainX + homePerfW + 8;
        homeVersionW = Math.max(100, homeSocialX - homeVersionX - 8);
    }

    private void drawHomeDashboard(GuiGraphics g, int mouseX, int mouseY) {
        prepareHomeLayout();
        drawHomeHero(g, mouseX, mouseY);
        drawHomeModules(g, mouseX, mouseY);
        drawHomeSpotify(g, mouseX, mouseY);
        drawHomeAppearance(g, mouseX, mouseY);
        drawHomeBottom(g, mouseX, mouseY);
    }

    private void drawHomeHero(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeCenterX;
        int y = homeHeroY;
        int w = homeCenterW;
        int h = homeHeroH;

        fillRounded(g, x, y, w, h, 0xD20B0D22, RuffyRoundedUi.RADIUS_PANEL);
        drawHeroLandscape(g, x + 2, y + 2, w - 4, h - 4);
        outlineRounded(g, x, y, w, h,
                withAlpha(RuffyConfig.primaryColor(), 165), RuffyRoundedUi.RADIUS_PANEL);

        int textX = x + 17;
        g.drawString(font, "Welcome back!", textX, y + 22, WHITE, false);
        g.drawString(font, "Enjoy your game with", textX, y + 43, MUTED, false);
        g.drawString(font, "Ruffy Visuals  ♥", textX, y + 56,
                RuffyConfig.primaryColor(), false);

        int buttonY = y + h - 34;
        drawButton(g, textX, buttonY, Math.min(130, w / 2), 24,
                "Explore Modules  →", ButtonStyle.PRIMARY,
                inside(mouseX, mouseY, textX, buttonY, Math.min(130, w / 2), 24), false);
    }

    private void drawHeroLandscape(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF100B2D);
        g.fill(x, y + h / 3, x + w, y + h, 0xFF211052);
        g.fill(x, y + h / 2, x + w, y + h, 0xFF180D35);

        // Nebula bands and a bright horizon reproduce the purple Minecraft dusk.
        g.fill(x + w / 3, y + 8, x + w - 12, y + h / 2, 0x402F21A0);
        g.fill(x + w / 2, y + h / 3, x + w - 10, y + h / 2 + 12, 0x553C1A8A);
        g.fill(x + w / 3, y + h / 2, x + w - 8, y + h / 2 + 2,
                withAlpha(RuffyConfig.primaryColor(), 115));

        for (int i = 0; i < 19; i++) {
            int sx = x + Math.floorMod(i * 53 + 17, Math.max(1, w));
            int sy = y + Math.floorMod(i * 29 + 7, Math.max(1, h / 2));
            int size = i % 5 == 0 ? 3 : 1;
            fillRounded(g, sx, sy, size, size,
                    withAlpha(i % 3 == 0 ? 0xFFFFD8FF : 0xFFD8C0FF, 115 + (i % 4) * 28), 2);
        }

        int horizon = y + h - 28;
        g.fill(x, horizon, x + w, y + h, 0xFF0A0C20);
        for (int i = 0; i < 14; i++) {
            int bx = x + i * Math.max(1, w / 13);
            int bw = Math.max(7, w / 15);
            int bh = 7 + Math.floorMod(i * 13, 19);
            g.fill(bx, horizon - bh, Math.min(x + w, bx + bw), y + h,
                    i % 2 == 0 ? 0xFF10122B : 0xFF171039);
            if (i % 3 == 0) {
                g.fill(bx + 2, horizon - bh - 9, bx + bw - 1, horizon - bh + 2,
                        0xFF171039);
            }
        }
        g.fill(x + 4, y + h - 7, x + w - 4, y + h - 5,
                withAlpha(RuffyConfig.primaryColor(), 70));

        // Pixel-styled floating crystal and a dark Enderman-like silhouette.
        drawHeroCrystal(g, x + w - 86, y + 19, RuffyConfig.primaryColor());
        drawHeroCharacter(g, x + w - 65, y + h - 78);
    }

    private void drawHeroCrystal(GuiGraphics g, int x, int y, int color) {
        fillRounded(g, x - 8, y + 8, 50, 48, withAlpha(color, 25), 20);
        int[] widths = {8, 16, 22, 26, 22, 16, 8};
        int[] offsets = {9, 5, 2, 0, 2, 5, 9};
        for (int i = 0; i < widths.length; i++) {
            int rowY = y + i * 5;
            g.fill(x + offsets[i], rowY, x + offsets[i] + widths[i], rowY + 5,
                    i < 3 ? withAlpha(0xFFE9D7FF, 230 - i * 18)
                            : withAlpha(color, 235 - i * 16));
        }
        g.fill(x + 10, y + 9, x + 15, y + 27, 0xFFEFE4FF);
        g.fill(x + 16, y + 20, x + 22, y + 33, withAlpha(0xFF9A58FF, 225));
        g.fill(x + 12, y + 39, x + 18, y + 45, withAlpha(0xFFFFFFFF, 155));
    }

    private void drawHeroCharacter(GuiGraphics g, int x, int y) {
        g.fill(x + 8, y, x + 38, y + 31, 0xFF080817);
        g.fill(x + 12, y + 5, x + 34, y + 26, 0xFF111024);
        g.fill(x + 14, y + 13, x + 19, y + 17, 0xFFFF43FF);
        g.fill(x + 27, y + 13, x + 32, y + 17, 0xFFFF43FF);
        g.fill(x + 13, y + 31, x + 34, y + 61, 0xFF090A18);
        g.fill(x + 1, y + 33, x + 13, y + 59, 0xFF0C0D1E);
        g.fill(x + 34, y + 33, x + 45, y + 59, 0xFF0C0D1E);
        g.fill(x + 15, y + 58, x + 23, y + 76, 0xFF080916);
        g.fill(x + 26, y + 58, x + 34, y + 76, 0xFF080916);
        g.fill(x + 18, y + 32, x + 30, y + 51,
                withAlpha(RuffyConfig.primaryColor(), 72));
        g.fill(x + 22, y + 35, x + 26, y + 49,
                withAlpha(RuffyConfig.primaryColor(), 170));
    }

    private void drawHomeModules(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeCenterX;
        int y = homeModulesY;
        int w = homeCenterW;
        int h = homeModulesH;
        fillRounded(g, x, y, w, h, 0xD20A0D1C, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, BORDER, RuffyRoundedUi.RADIUS_PANEL);

        g.drawString(font, "✦  Featured Modules", x + 13, y + 11, WHITE, false);
        g.drawString(font, "Manage your modules", x + 13, y + 24, MUTED, false);
        int seeX = x + w - 62;
        g.drawString(font, "See all  →", seeX, y + 17, RuffyConfig.primaryColor(), false);

        String[] keys = {"prism_core", "nebula_particles", "orbit_particles", "swing_animation"};
        String[] titles = {"3D Prism", "Particles", "Orbit Rings", "Animations"};
        String[] descriptions = {"Crystal core", "Ambient glints", "Player orbit", "Smooth swing"};
        String[] icons = {"◇", "✧", "◌", "★"};
        int gap = 5;
        int left = x + 11;
        int tileY = y + 42;
        int tileH = Math.max(42, h - 53);
        int tileW = Math.max(48, (w - 22 - gap * 3) / 4);

        for (int i = 0; i < keys.length; i++) {
            int tx = left + i * (tileW + gap);
            boolean enabled = RuffyConfig.get(keys[i]);
            boolean hovered = inside(mouseX, mouseY, tx, tileY, tileW, tileH);
            float hover = RuffyAnimation.animate("home.feature.hover." + keys[i],
                    hovered ? 1.0F : 0.0F);
            fillRounded(g, tx, tileY, tileW, tileH,
                    RuffyAnimation.blendColor(0xA9141729, 0xD2202142, hover),
                    RuffyRoundedUi.RADIUS_CONTROL);
            outlineRounded(g, tx, tileY, tileW, tileH,
                    withAlpha(RuffyConfig.primaryColor(), enabled ? 95 : 38),
                    RuffyRoundedUi.RADIUS_CONTROL);
            g.drawString(font, icons[i], tx + 7, tileY + 7,
                    enabled ? RuffyConfig.primaryColor() : DIM, false);
            int switchW = Math.min(24, tileW / 2);
            drawSwitch(g, tx + tileW - switchW - 6, tileY + 7,
                    enabled, switchW, 13, "home.feature." + keys[i]);
            g.drawString(font, trimToWidth(titles[i], tileW - 12),
                    tx + 7, tileY + 25, WHITE, false);
            g.drawString(font, trimToWidth(descriptions[i], tileW - 12),
                    tx + 7, tileY + 37, MUTED, false);
        }
    }

    private void drawHomeSpotify(GuiGraphics g, int mouseX, int mouseY) {
        int x = homeRightX;
        int y = homeSpotifyY;
        int w = homeRightW;
        int h = homeSpotifyH;
        fillRounded(g, x, y, w, h, 0xE0090C19, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, 0x775E4A9F, RuffyRoundedUi.RADIUS_PANEL);

        fillRounded(g, x + 11, y + 10, 22, 22, 0xFF1ED760, 11);
        g.drawCenteredString(font, "≋", x + 22, y + 15, 0xFF092010);
        g.drawString(font, "Spotify", x + 40, y + 12, WHITE, false);
        boolean connected = RuffySpotify.isConnected();
        int statusColor = connected ? 0xFF42E69A : 0xFFFFC078;
        fillRounded(g, x + w - 84, y + 17, 5, 5, statusColor, 3);
        g.drawString(font, connected ? "Connected" : "Connect",
                x + w - 74, y + 13, statusColor, false);
        drawIconButton(g, x + w - 26, y + 9, 18, 18, "⚙", MUTED,
                inside(mouseX, mouseY, x + w - 26, y + 9, 18, 18));

        int artX = x + 12;
        int artY = y + 41;
        drawAlbumArt(g, artX, artY, 43);
        int infoX = artX + 52;
        int infoW = w - 75;
        g.drawString(font, trimToWidth(RuffySpotify.getTrackName(), infoW),
                infoX, artY + 7, WHITE, false);
        g.drawString(font, trimToWidth(RuffySpotify.getArtistName(), infoW),
                infoX, artY + 21, MUTED, false);
        drawVisualizerBars(g, infoX, artY + 23, Math.max(30, infoW - 2));

        int progressY = y + 91;
        int progressX = x + 12;
        int progressW = w - 24;
        long duration = RuffySpotify.getDurationMs();
        long position = RuffySpotify.getProgressMs();
        float ratio = duration <= 0L ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, position / (float) duration));
        fillRounded(g, progressX, progressY, progressW, 3, 0xFF38304D, 2);
        fillRounded(g, progressX, progressY, Math.round(progressW * ratio), 3,
                RuffyConfig.primaryColor(), 2);
        g.drawString(font, formatTime(position), progressX, progressY + 6, DIM, false);
        String total = formatTime(duration);
        g.drawString(font, total, progressX + progressW - font.width(total),
                progressY + 6, DIM, false);

        int controlsY = y + 108;
        int center = x + w / 2;
        drawIconButton(g, center - 37, controlsY, 21, 20, "|◀", WHITE,
                inside(mouseX, mouseY, center - 37, controlsY, 21, 20));
        fillRounded(g, center - 11, controlsY - 1, 23, 22,
                RuffyConfig.primaryColor(), 11);
        g.drawCenteredString(font, RuffySpotify.isPlaying() ? "Ⅱ" : "▶",
                center, controlsY + 6, WHITE);
        drawIconButton(g, center + 17, controlsY, 21, 20, "▶|", WHITE,
                inside(mouseX, mouseY, center + 17, controlsY, 21, 20));

        int lyricsY = y + 134;
        int lyricsH = Math.max(27, h - 143);
        fillRounded(g, x + 9, lyricsY, w - 18, lyricsH,
                0x8F171329, RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, x + 9, lyricsY, w - 18, lyricsH,
                0x4D8D52D5, RuffyRoundedUi.RADIUS_CONTROL);
        g.drawString(font, "♫  Lyrics", x + 17, lyricsY + 7,
                RuffyConfig.primaryColor(), false);
        String[] lyricLines = RuffySpotify.getLyrics().split("\\R");
        String lineOne = lyricLines.length > 0 ? lyricLines[0] : "";
        String lineTwo = lyricLines.length > 1 ? lyricLines[1] : "";
        if (lineOne.isBlank()) lineOne = connected
                ? "Lyrics will appear here when available." : "Connect Spotify to show lyrics.";
        g.drawCenteredString(font, trimToWidth(lineOne, w - 35),
                x + w / 2, lyricsY + 19, WHITE);
        if (lyricsH > 40 && !lineTwo.isBlank()) {
            g.drawCenteredString(font, trimToWidth(lineTwo, w - 35),
                    x + w / 2, lyricsY + 30, MUTED);
        }
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
        int x = homeRightX;
        int y = homeAppearanceY;
        int w = homeRightW;
        int h = homeAppearanceH;
        fillRounded(g, x, y, w, h, 0xDA0A0D1C, RuffyRoundedUi.RADIUS_PANEL);
        outlineRounded(g, x, y, w, h, BORDER, RuffyRoundedUi.RADIUS_PANEL);

        g.drawString(font, "◈  Appearance", x + 11, y + 10, WHITE, false);
        g.drawString(font, "Customize your look", x + 11, y + 23, MUTED, false);

        int themeY = y + 31;
        g.drawString(font, "Theme", x + 12, themeY + 5, MUTED, false);
        int themeX = x + w - 83;
        fillRounded(g, themeX, themeY, 70, 20, 0x8C20203D,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, themeX, themeY, 70, 20,
                0x556A5F98, RuffyRoundedUi.RADIUS_CONTROL);
        fillRounded(g, themeX + 7, themeY + 7, 6, 6,
                RuffyConfig.primaryColor(), 3);
        g.drawString(font, paletteName(RuffyConfig.getInt("palette")),
                themeX + 17, themeY + 6, WHITE, false);
        g.drawString(font, "⌄", themeX + 59, themeY + 5, MUTED, false);

        int opacityY = y + 54;
        g.drawString(font, "Opacity", x + 12, opacityY + 4, MUTED, false);
        int sliderX = x + 72;
        int sliderW = Math.max(38, w - 119);
        drawSlider(g, sliderX, opacityY, sliderW,
                RuffyConfig.getInt("menu_opacity"), 60, 100);
        String opacity = RuffyConfig.getInt("menu_opacity") + "%";
        g.drawString(font, opacity, x + w - font.width(opacity) - 11,
                opacityY + 3, WHITE, false);

        int effectsY = y + 76;
        g.drawString(font, "Particles", x + 12, effectsY + 4, MUTED, false);
        int effectPercent = RuffyConfig.getInt("effect_quality") * 50;
        drawSlider(g, sliderX, effectsY, sliderW, effectPercent, 0, 100);
        String effects = effectPercent + "%";
        g.drawString(font, effects, x + w - font.width(effects) - 11,
                effectsY + 3, WHITE, false);
    }

    private void drawHomeBottom(GuiGraphics g, int mouseX, int mouseY) {
        int y = homeBottomY + 7;
        int h = 32;

        fillRounded(g, mainX, y, homePerfW, h, 0xD20B0D1C,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, mainX, y, homePerfW, h, BORDER,
                RuffyRoundedUi.RADIUS_CONTROL);
        String fps = Minecraft.getInstance().getFps() + " FPS";
        g.drawString(font, "ϟ", mainX + 10, y + 8,
                RuffyConfig.primaryColor(), false);
        if (homePerfW < 160) {
            g.drawString(font, "Performance", mainX + 25, y + 6, WHITE, false);
            g.drawString(font, fps, mainX + 25, y + 18,
                    RuffyConfig.primaryColor(), false);
        } else {
            g.drawString(font, "Performance", mainX + 25, y + 6, WHITE, false);
            g.drawString(font, "Smooth as never before", mainX + 25, y + 18, MUTED, false);
            g.drawString(font, fps, mainX + homePerfW - font.width(fps) - 9,
                    y + 11, RuffyConfig.primaryColor(), false);
        }

        fillRounded(g, homeVersionX, y, homeVersionW, h, 0xD00C0D20,
                RuffyRoundedUi.RADIUS_CONTROL);
        outlineRounded(g, homeVersionX, y, homeVersionW, h,
                withAlpha(RuffyConfig.primaryColor(), 130), RuffyRoundedUi.RADIUS_CONTROL);
        g.drawString(font, "♛", homeVersionX + 9, y + 8, 0xFFFFD45E, false);
        g.drawString(font, trimToWidth("Ruffy Visuals 2.5.0", Math.max(1, homeVersionW - 34)),
                homeVersionX + 24, y + 6, WHITE, false);
        g.drawString(font, trimToWidth("The best visuals. Better than ever.", Math.max(1, homeVersionW - 34)),
                homeVersionX + 24, y + 18, MUTED, false);

        String[] socials = {"D", "▶", "GH", "◎"};
        for (int i = 0; i < socials.length; i++) {
            int sx = homeSocialX + i * 29;
            fillRounded(g, sx, y + 4, 24, 24, 0xD20D1020, 12);
            outlineRounded(g, sx, y + 4, 24, 24, 0x745D4AA3, 12);
            g.drawCenteredString(font, socials[i], sx + 12, y + 12,
                    i == 0 ? RuffyConfig.primaryColor() : WHITE);
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

        // Close control in the top-right of the app bar.
        int closeX = mainX + mainW - 36 + 26;
        int closeY = panelY + 14;
        if (inside(mx, my, closeX - 1, closeY - 1, 13, 13)) {
            onClose();
            return true;
        }

        int heroButtonX = homeCenterX + 17;
        int heroButtonY = homeHeroY + homeHeroH - 34;
        if (inside(mx, my, heroButtonX, heroButtonY,
                Math.min(130, homeCenterW / 2), 24)) {
            tab = Tab.VISUALS;
            modulePage = 0;
            return true;
        }

        int moduleSeeX = homeCenterX + homeCenterW - 62;
        if (inside(mx, my, moduleSeeX, homeModulesY + 7, 58, 24)) {
            tab = Tab.VISUALS;
            return true;
        }

        String[] keys = {"prism_core", "nebula_particles", "orbit_particles", "swing_animation"};
        int gap = 5;
        int left = homeCenterX + 11;
        int tileY = homeModulesY + 42;
        int tileH = Math.max(42, homeModulesH - 53);
        int tileW = Math.max(48, (homeCenterW - 22 - gap * 3) / 4);
        for (int i = 0; i < keys.length; i++) {
            int tx = left + i * (tileW + gap);
            int switchW = Math.min(24, tileW / 2);
            int switchX = tx + tileW - switchW - 6;
            if (inside(mx, my, switchX - 2, tileY + 4, switchW + 4, 20)) {
                RuffyConfig.set(keys[i], !RuffyConfig.get(keys[i]));
                return true;
            }
            if (inside(mx, my, tx, tileY, tileW, tileH)) {
                tab = Tab.VISUALS;
                selectedKey = keys[i];
                return true;
            }
        }

        int spotifyX = homeRightX;
        int spotifyY = homeSpotifyY;
        int spotifyW = homeRightW;
        if (inside(mx, my, spotifyX + spotifyW / 2 - 11, spotifyY + 107, 23, 24)) {
            RuffySpotify.togglePlayback();
            return true;
        }
        int center = spotifyX + spotifyW / 2;
        if (inside(mx, my, center - 40, spotifyY + 108, 23, 22)) {
            RuffySpotify.previousTrack();
            return true;
        }
        if (inside(mx, my, center + 16, spotifyY + 108, 24, 22)) {
            RuffySpotify.nextTrack();
            return true;
        }
        if (inside(mx, my, spotifyX + spotifyW - 30, spotifyY + 7, 24, 22)
                || inside(mx, my, spotifyX, spotifyY, spotifyW, 34)) {
            if (minecraft != null) minecraft.setScreen(new RuffySpotifyScreen(this));
            return true;
        }

        int appearanceX = homeRightX;
        int appearanceY = homeAppearanceY;
        int themeX = appearanceX + homeRightW - 83;
        if (inside(mx, my, themeX, appearanceY + 31, 70, 20)) {
            RuffyConfig.setInt("palette", (RuffyConfig.getInt("palette") + 1) % 10);
            return true;
        }

        int sliderX = appearanceX + 72;
        int sliderW = Math.max(38, homeRightW - 119);
        int opacityY = appearanceY + 54;
        if (inside(mx, my, sliderX - 4, opacityY, sliderW + 8, 16)) {
            int value = 60 + Math.round((mx - sliderX) * 40.0F / Math.max(1, sliderW));
            RuffyConfig.setInt("menu_opacity", value);
            return true;
        }
        int effectsY = appearanceY + 76;
        if (inside(mx, my, sliderX - 4, effectsY, sliderW + 8, 16)) {
            int percent = Math.round((mx - sliderX) * 100.0F / Math.max(1, sliderW));
            RuffyConfig.setInt("effect_quality", Math.round(percent / 50.0F));
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
                "Ruffy Visuals 2.5.0 · Minecraft 1.21.11 · Fabric");
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
            g.drawString(font, "RUFFY VISUALS 2.5  ·  GALAXY EDITION",
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
                panelY + 111, panelY + 136, panelY + 161,
                panelY + 186, panelY + 229, panelY + 272,
                panelY + 297
        };
        Tab[] tabs = {
                Tab.HOME, Tab.VISUALS, Tab.STYLE, Tab.HUD,
                Tab.MUSIC, Tab.CONFIGS, Tab.SETUP
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
