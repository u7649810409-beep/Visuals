package de.ruffy.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * High quality rounded GUI primitives for Minecraft 1.21.11.
 *
 * A 4x supersampled alpha mask is rendered as a nine-slice texture, so corner
 * radii stay constant when a small pill or a wide panel is drawn. The mask is
 * bilinearly filtered by the GPU and its outline is a separate anti-aliased
 * ring instead of a pixel staircase made from one-pixel rectangles.
 */
public final class RuffyRoundedUi {
    private static final int SIZE = 256;
    private static final int SUPERSAMPLE = 4;
    private static final int MAX_RADIUS = 24;
    private static final float OUTLINE_WIDTH = 1.15F;

    private static final Map<Integer, Identifier> FILLS = new HashMap<>();
    private static final Map<Integer, Identifier> OUTLINES = new HashMap<>();

    private RuffyRoundedUi() {}

    public static void fill(GuiGraphics graphics, int x, int y, int width, int height,
                            int color, int radius) {
        drawNineSlice(graphics, x, y, width, height, color, radius, false);
    }

    public static void outline(GuiGraphics graphics, int x, int y, int width, int height,
                               int color, int radius) {
        drawNineSlice(graphics, x, y, width, height, color, radius, true);
    }

    private static void drawNineSlice(GuiGraphics graphics, int x, int y,
                                      int width, int height, int color,
                                      int requestedRadius, boolean outline) {
        if (width <= 0 || height <= 0 || ((color >>> 24) & 0xFF) == 0) return;

        int radius = Math.max(1, Math.min(MAX_RADIUS,
                Math.min(requestedRadius, Math.min(width, height) / 2)));
        Identifier texture = texture(radius, outline);
        int sourceCorner = radius * SUPERSAMPLE;
        int sourceMiddle = SIZE - sourceCorner * 2;
        int destCorner = radius;
        int destMiddleW = Math.max(0, width - destCorner * 2);
        int destMiddleH = Math.max(0, height - destCorner * 2);

        // Top row.
        blit(graphics, texture, x, y, 0, 0,
                destCorner, destCorner, sourceCorner, sourceCorner, color);
        blit(graphics, texture, x + destCorner, y, sourceCorner, 0,
                destMiddleW, destCorner, sourceMiddle, sourceCorner, color);
        blit(graphics, texture, x + width - destCorner, y, SIZE - sourceCorner, 0,
                destCorner, destCorner, sourceCorner, sourceCorner, color);

        // Middle row.
        blit(graphics, texture, x, y + destCorner, 0, sourceCorner,
                destCorner, destMiddleH, sourceCorner, sourceMiddle, color);
        blit(graphics, texture, x + destCorner, y + destCorner,
                sourceCorner, sourceCorner, destMiddleW, destMiddleH,
                sourceMiddle, sourceMiddle, color);
        blit(graphics, texture, x + width - destCorner, y + destCorner,
                SIZE - sourceCorner, sourceCorner, destCorner, destMiddleH,
                sourceCorner, sourceMiddle, color);

        // Bottom row.
        blit(graphics, texture, x, y + height - destCorner, 0, SIZE - sourceCorner,
                destCorner, destCorner, sourceCorner, sourceCorner, color);
        blit(graphics, texture, x + destCorner, y + height - destCorner,
                sourceCorner, SIZE - sourceCorner, destMiddleW, destCorner,
                sourceMiddle, sourceCorner, color);
        blit(graphics, texture, x + width - destCorner, y + height - destCorner,
                SIZE - sourceCorner, SIZE - sourceCorner,
                destCorner, destCorner, sourceCorner, sourceCorner, color);
    }

    private static void blit(GuiGraphics graphics, Identifier texture,
                             int x, int y, int u, int v,
                             int width, int height, int sourceWidth, int sourceHeight,
                             int color) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y, (float) u, (float) v,
                width, height, sourceWidth, sourceHeight,
                SIZE, SIZE, color);
    }

    private static Identifier texture(int radius, boolean outline) {
        Map<Integer, Identifier> cache = outline ? OUTLINES : FILLS;
        return cache.computeIfAbsent(radius, key -> createTexture(key, outline));
    }

    private static Identifier createTexture(int radius, boolean outline) {
        Minecraft minecraft = Minecraft.getInstance();
        int corner = radius * SUPERSAMPLE;
        float stroke = OUTLINE_WIDTH * SUPERSAMPLE;
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, SIZE, SIZE, false);

        for (int py = 0; py < SIZE; py++) {
            for (int px = 0; px < SIZE; px++) {
                int covered = 0;
                for (int sy = 0; sy < SUPERSAMPLE; sy++) {
                    for (int sx = 0; sx < SUPERSAMPLE; sx++) {
                        float sampleX = px + (sx + 0.5F) / SUPERSAMPLE;
                        float sampleY = py + (sy + 0.5F) / SUPERSAMPLE;
                        boolean outer = insideRounded(sampleX, sampleY,
                                SIZE * 0.5F, SIZE * 0.5F,
                                SIZE * 0.5F, SIZE * 0.5F, corner);
                        if (!outer) continue;

                        if (!outline || !insideRounded(sampleX, sampleY,
                                SIZE * 0.5F, SIZE * 0.5F,
                                SIZE * 0.5F - stroke, SIZE * 0.5F - stroke,
                                Math.max(1.0F, corner - stroke))) {
                            covered++;
                        }
                    }
                }

                int alpha = Math.round(covered * 255.0F
                        / (SUPERSAMPLE * SUPERSAMPLE));
                image.setPixel(px, py, (alpha << 24) | 0x00FFFFFF);
            }
        }

        Identifier id = Identifier.fromNamespaceAndPath("ruffyclient",
                "gui/" + (outline ? "rounded_outline_" : "rounded_fill_") + radius);
        DynamicTexture dynamic = new DynamicTexture(
                () -> "Ruffy anti-aliased rounded " + (outline ? "outline " : "fill ") + radius,
                image);
        minecraft.getTextureManager().register(id, dynamic);
        dynamic.upload();
        return id;
    }

    /** Signed-distance test sampled 4x4 times per final UI pixel. */
    private static boolean insideRounded(float px, float py, float centerX, float centerY,
                                          float halfWidth, float halfHeight, float radius) {
        float dx = Math.abs(px - centerX) - (halfWidth - radius);
        float dy = Math.abs(py - centerY) - (halfHeight - radius);
        float outsideX = Math.max(dx, 0.0F);
        float outsideY = Math.max(dy, 0.0F);
        float signedDistance = (float) Math.sqrt(outsideX * outsideX + outsideY * outsideY)
                + Math.min(Math.max(dx, dy), 0.0F) - radius;
        return signedDistance <= 0.0F;
    }
}
