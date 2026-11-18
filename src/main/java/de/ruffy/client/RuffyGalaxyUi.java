package de.ruffy.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Random;

/** Procedural Nebula background generated once and drawn as a GPU texture. */
public final class RuffyGalaxyUi {
    private static final int TEX_WIDTH = 1024;
    private static final int TEX_HEIGHT = 576;
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("ruffyclient", "gui/nebula_backdrop");
    private static boolean uploaded;

    private RuffyGalaxyUi() {}

    public static void draw(GuiGraphics graphics, int width, int height) {
        ensureTexture();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                0, 0, 0.0F, 0.0F,
                width, height, TEX_WIDTH, TEX_HEIGHT,
                TEX_WIDTH, TEX_HEIGHT, 0xF2FFFFFF);
    }

    private static void ensureTexture() {
        if (uploaded) return;

        NativeImage image = new NativeImage(NativeImage.Format.RGBA,
                TEX_WIDTH, TEX_HEIGHT, false);

        // Precompute deterministic star glow so texture generation needs no image readback.
        float[] stars = new float[TEX_WIDTH * TEX_HEIGHT];
        Random random = new Random(0x525546465947414CL);
        for (int i = 0; i < 1050; i++) {
            int sx = random.nextInt(TEX_WIDTH);
            int sy = random.nextInt(TEX_HEIGHT);
            int radius = random.nextInt(3) == 0 ? 3 : 1;
            float strength = (70 + random.nextInt(185)) / 255.0F;
            for (int oy = -radius; oy <= radius; oy++) {
                for (int ox = -radius; ox <= radius; ox++) {
                    int px = sx + ox;
                    int py = sy + oy;
                    if (px < 0 || py < 0 || px >= TEX_WIDTH || py >= TEX_HEIGHT) continue;
                    double distance = Math.sqrt(ox * ox + oy * oy);
                    if (distance > radius + 0.25) continue;
                    float glow = (float) (strength * (1.0 - distance / (radius + 0.75)));
                    int index = py * TEX_WIDTH + px;
                    stars[index] = Math.max(stars[index], glow);
                }
            }
        }

        // Generate soft, overlapping nebula clouds with no tiled or pixel-noise pattern.
        for (int y = 0; y < TEX_HEIGHT; y++) {
            double v = y / (double) TEX_HEIGHT;
            for (int x = 0; x < TEX_WIDTH; x++) {
                double u = x / (double) TEX_WIDTH;
                double wave = Math.sin(u * 8.4 + v * 3.2)
                        * 0.45 + Math.sin(u * 18.0 - v * 7.0) * 0.18;
                double ridge = v - (0.49 + 0.085 * Math.sin(u * 5.4 + 0.4)
                        + 0.035 * Math.sin(u * 14.0));
                double band = Math.exp(-(ridge * ridge) / 0.0075);

                double dx1 = (u - 0.30) / 0.30;
                double dy1 = (v - 0.49) / 0.20;
                double cloudOne = Math.exp(-(dx1 * dx1 + dy1 * dy1) * 1.45);

                double dx2 = (u - 0.72) / 0.24;
                double dy2 = (v - 0.43) / 0.16;
                double cloudTwo = Math.exp(-(dx2 * dx2 + dy2 * dy2) * 1.9);

                double dx3 = (u - 0.53) / 0.19;
                double dy3 = (v - 0.61) / 0.10;
                double cloudThree = Math.exp(-(dx3 * dx3 + dy3 * dy3) * 2.0);

                double turbulence = 0.78
                        + 0.13 * Math.sin(u * 37.0 + v * 19.0)
                        + 0.08 * Math.sin(u * 71.0 - v * 43.0)
                        + 0.045 * Math.sin(u * 129.0 + v * 83.0);
                double nebula = clamp01((band * 0.56 + cloudOne * 0.46
                        + cloudTwo * 0.50 + cloudThree * 0.48) * turbulence
                        + Math.max(0.0, wave) * band * 0.15);

                float star = stars[y * TEX_WIDTH + x];
                int red = clampColor(4 + nebula * 94 + cloudThree * 36 + star * 118);
                int green = clampColor(3 + nebula * 13 + cloudTwo * 10 + star * 92);
                int blue = clampColor(13 + nebula * 108 + cloudOne * 32
                        + Math.max(0.0, wave) * band * 24 + star * 138);
                image.setPixel(x, y, 0xFF000000 | (red << 16) | (green << 8) | blue);
            }
        }
        DynamicTexture texture = new DynamicTexture(() -> "Ruffy procedural nebula", image);
        Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
        texture.upload();
        uploaded = true;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static int clampColor(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }
}
