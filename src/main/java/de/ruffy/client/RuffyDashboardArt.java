package de.ruffy.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Random;

/** Original abstract cosmic artwork for Ruffy's desktop-style dashboard. */
public final class RuffyDashboardArt {
    private static final int W = 768;
    private static final int H = 256;
    private static final Identifier HERO =
            Identifier.fromNamespaceAndPath("ruffyclient", "gui/cosmic_dashboard_hero");
    private static boolean uploaded;

    private RuffyDashboardArt() {}

    public static void drawHero(GuiGraphics g, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        ensureUploaded();
        g.blit(RenderPipelines.GUI_TEXTURED, HERO, x, y, 0.0F, 0.0F,
                width, height, W, H, W, H, 0xFFFFFFFF);
    }

    private static void ensureUploaded() {
        if (uploaded) return;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, W, H, false);
        float[] stars = new float[W * H];
        Random random = new Random(0x5255464659434F53L);
        for (int i = 0; i < 620; i++) {
            int sx = random.nextInt(W);
            int sy = random.nextInt(H);
            int radius = random.nextInt(7) == 0 ? 2 : 1;
            float strength = 0.35F + random.nextFloat() * 0.65F;
            for (int oy = -radius; oy <= radius; oy++) {
                for (int ox = -radius; ox <= radius; ox++) {
                    int px = sx + ox, py = sy + oy;
                    if (px < 0 || py < 0 || px >= W || py >= H) continue;
                    double d = Math.sqrt(ox * ox + oy * oy);
                    if (d > radius + 0.15) continue;
                    int index = py * W + px;
                    stars[index] = Math.max(stars[index],
                            (float) (strength * (1.0 - d / (radius + 0.8))));
                }
            }
        }

        final double cx = 0.79, cy = 0.50, planetRadius = 0.255;
        final double angle = -0.34, cos = Math.cos(angle), sin = Math.sin(angle);
        for (int py = 0; py < H; py++) {
            double v = py / (double) H;
            for (int px = 0; px < W; px++) {
                double u = px / (double) W;
                double cloudA = gaussian(u, v, 0.23, 0.49, 0.27, 0.34, 1.5);
                double cloudB = gaussian(u, v, 0.53, 0.33, 0.23, 0.26, 1.8);
                double cloudC = gaussian(u, v, 0.76, 0.65, 0.24, 0.27, 1.5);
                double wave = 0.5 + 0.5 * Math.sin((u * 4.0 + v * 1.7)
                        * Math.PI * 2.0 + Math.sin(v * 8.0) * 0.5);
                double nebula = (cloudA * 0.52 + cloudB * 0.45 + cloudC * 0.35)
                        * (0.68 + wave * 0.34);
                float star = stars[py * W + px];
                int r = color(5 + nebula * 82 + cloudA * 24 + star * 170);
                int gr = color(4 + nebula * 14 + cloudB * 12 + star * 125);
                int b = color(18 + nebula * 128 + cloudA * 22 + cloudC * 18 + star * 205);

                double dx = u - cx, dy = v - cy;
                double rx = dx * cos + dy * sin;
                double ry = -dx * sin + dy * cos;
                double ringDist = Math.sqrt(sq(rx / 0.36) + sq(ry / 0.104));
                boolean ring = ringDist > 0.88 && ringDist < 1.13;
                if (ring && ry < 0.0) {
                    double sheen = 0.5 + 0.5 * Math.cos((ringDist - 1.0) * 24.0);
                    r = mix(r, color(122 + sheen * 84), 0.76);
                    gr = mix(gr, color(39 + sheen * 30), 0.72);
                    b = mix(b, color(214 + sheen * 38), 0.84);
                }

                double nx = dx / planetRadius, ny = dy / planetRadius;
                double d2 = nx * nx + ny * ny;
                if (d2 <= 1.0) {
                    double nz = Math.sqrt(Math.max(0.0, 1.0 - d2));
                    double diffuse = Math.max(0.0, -nx * 0.56 - ny * 0.42 + nz * 0.71);
                    double spec = Math.pow(Math.max(0.0, -nx * 0.62 - ny * 0.46 + nz * 0.63), 15.0);
                    double shade = 0.20 + diffuse * 0.77 + spec * 0.54;
                    r = color((80 + nx * 28 + (1.0 - nz) * 28) * shade);
                    gr = color((35 + ny * 12) * shade);
                    b = color((160 + ny * 16 + (1.0 - nz) * 55) * shade);
                }

                if (ring && ry >= 0.0) {
                    double sheen = 0.5 + 0.5 * Math.cos((ringDist - 1.0) * 23.0);
                    r = mix(r, color(155 + sheen * 64), 0.80);
                    gr = mix(gr, color(56 + sheen * 25), 0.77);
                    b = mix(b, color(226 + sheen * 28), 0.91);
                }

                double flare = gaussian(u, v, 0.60, 0.22, 0.025, 0.14, 2.2);
                r = color(r + flare * 32);
                gr = color(gr + flare * 12);
                b = color(b + flare * 65);

                int alpha = insideRounded(px + 0.5, py + 0.5, W, H, 25) ? 255 : 0;
                image.setPixel(px, py, (alpha << 24) | (r << 16) | (gr << 8) | b);
            }
        }

        DynamicTexture texture = new DynamicTexture(() -> "Ruffy abstract ringed-planet hero", image);
        Minecraft.getInstance().getTextureManager().register(HERO, texture);
        texture.upload();
        uploaded = true;
    }

    private static double gaussian(double x, double y, double cx, double cy,
                                   double sx, double sy, double falloff) {
        double dx = (x - cx) / sx, dy = (y - cy) / sy;
        return Math.exp(-(dx * dx + dy * dy) * falloff);
    }

    private static double sq(double n) { return n * n; }
    private static int mix(int a, int b, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return color(a + (b - a) * t);
    }
    private static int color(double n) {
        return Math.max(0, Math.min(255, (int) Math.round(n)));
    }
    private static boolean insideRounded(double px, double py,
                                         double width, double height, double radius) {
        double dx = Math.abs(px - width * 0.5) - (width * 0.5 - radius);
        double dy = Math.abs(py - height * 0.5) - (height * 0.5 - radius);
        double ox = Math.max(dx, 0.0), oy = Math.max(dy, 0.0);
        return Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(dx, dy), 0.0)
                - radius <= 0.0;
    }
}
