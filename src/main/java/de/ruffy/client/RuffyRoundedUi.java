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
 * Ruffy's rounded-surface renderer for Minecraft 1.21.11.
 *
 * All screens share the same small radius scale. Rounded masks are rendered
 * as nine-slice textures so a card can grow without stretching its corners.
 * The mask cache is quantized to a few common radii, and the 2x coverage mask
 * keeps edges antialiased without doing a million CPU samples per radius.
 */
public final class RuffyRoundedUi {
    private static final int TEXTURE_SIZE = 128;
    private static final int SUPERSAMPLE = 2;
    private static final int MAX_RADIUS = 30;
    private static final float OUTLINE_WIDTH = 1.0F;

    // Shared shape scale — do not invent a new radius for each component.
    public static final int RADIUS_WINDOW = 28;
    public static final int RADIUS_PANEL = 20;
    public static final int RADIUS_CARD = 16;
    public static final int RADIUS_ROW = 12;
    public static final int RADIUS_CONTROL = 10;
    public static final int RADIUS_SMALL = 8;

    private static final int[] RADIUS_STEPS = {2, 4, 8, 10, 12, 14, 16, 20, 24, 28, 30};
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

    /** Draws a restrained two-layer shadow with the same corner profile as its surface. */
    public static void shadow(GuiGraphics graphics, int x, int y, int width, int height,
                              int radius, int strength) {
        if (width <= 0 || height <= 0 || strength <= 0) return;
        int alpha = Math.max(0, Math.min(72, strength));
        fill(graphics, x + 1, y + 4, width, height,
                (alpha << 24), radius);
        fill(graphics, x, y + 2, width, height,
                (Math.max(0, alpha / 2) << 24), radius);
    }

    private static void drawNineSlice(GuiGraphics graphics, int x, int y,
                                      int width, int height, int color,
                                      int requestedRadius, boolean outline) {
        if (width <= 0 || height <= 0 || ((color >>> 24) & 0xFF) == 0) return;

        // A one- or two-pixel line cannot form a useful corner; drawing it
        // directly also avoids double-blending where top/bottom slices overlap.
        if (width <= 2 || height <= 2) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        int limit = Math.min(MAX_RADIUS, Math.min(width, height) / 2);
        int radius = quantizeRadius(Math.max(1, Math.min(requestedRadius, limit)), limit);
        if (radius <= 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }

        Identifier texture = texture(radius, outline);
        int sourceCorner = radius * SUPERSAMPLE;
        int sourceMiddle = TEXTURE_SIZE - sourceCorner * 2;
        int destCorner = radius;
        int destMiddleW = Math.max(0, width - destCorner * 2);
        int destMiddleH = Math.max(0, height - destCorner * 2);

        // Top row.
        blit(graphics, texture, x, y, 0, 0,
                destCorner, destCorner, sourceCorner, sourceCorner, color);
        blit(graphics, texture, x + destCorner, y, sourceCorner, 0,
                destMiddleW, destCorner, sourceMiddle, sourceCorner, color);
        blit(graphics, texture, x + width - destCorner, y, TEXTURE_SIZE - sourceCorner, 0,
                destCorner, destCorner, sourceCorner, sourceCorner, color);

        // Middle row.
        blit(graphics, texture, x, y + destCorner, 0, sourceCorner,
                destCorner, destMiddleH, sourceCorner, sourceMiddle, color);
        blit(graphics, texture, x + destCorner, y + destCorner,
                sourceCorner, sourceCorner, destMiddleW, destMiddleH,
                sourceMiddle, sourceMiddle, color);
        blit(graphics, texture, x + width - destCorner, y + destCorner,
                TEXTURE_SIZE - sourceCorner, sourceCorner,
                destCorner, destMiddleH, sourceCorner, sourceMiddle, color);

        // Bottom row.
        blit(graphics, texture, x, y + height - destCorner, 0, TEXTURE_SIZE - sourceCorner,
                destCorner, destCorner, sourceCorner, sourceCorner, color);
        blit(graphics, texture, x + destCorner, y + height - destCorner,
                sourceCorner, TEXTURE_SIZE - sourceCorner, destMiddleW, destCorner,
                sourceMiddle, sourceCorner, color);
        blit(graphics, texture, x + width - destCorner, y + height - destCorner,
                TEXTURE_SIZE - sourceCorner, TEXTURE_SIZE - sourceCorner,
                destCorner, destCorner, sourceCorner, sourceCorner, color);
    }

    private static int quantizeRadius(int requested, int maximum) {
        // A true pill/circle must keep the exact half-height radius.
        if (requested >= maximum) return maximum;
        int selected = 0;
        int distance = Integer.MAX_VALUE;
        for (int step : RADIUS_STEPS) {
            if (step > maximum) continue;
            int candidateDistance = Math.abs(step - requested);
            if (candidateDistance < distance) {
                selected = step;
                distance = candidateDistance;
            }
        }
        return selected;
    }

    private static void blit(GuiGraphics graphics, Identifier texture,
                             int x, int y, int u, int v,
                             int width, int height, int sourceWidth, int sourceHeight,
                             int color) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
                x, y, (float) u, (float) v,
                width, height, sourceWidth, sourceHeight,
                TEXTURE_SIZE, TEXTURE_SIZE, color);
    }

    private static Identifier texture(int radius, boolean outline) {
        Map<Integer, Identifier> cache = outline ? OUTLINES : FILLS;
        return cache.computeIfAbsent(radius, key -> createTexture(key, outline));
    }

    private static Identifier createTexture(int radius, boolean outline) {
        Minecraft minecraft = Minecraft.getInstance();
        int corner = radius * SUPERSAMPLE;
        float stroke = OUTLINE_WIDTH * SUPERSAMPLE;
        NativeImage image = new NativeImage(
                NativeImage.Format.RGBA, TEXTURE_SIZE, TEXTURE_SIZE, false);

        for (int py = 0; py < TEXTURE_SIZE; py++) {
            for (int px = 0; px < TEXTURE_SIZE; px++) {
                int covered = 0;
                for (int sy = 0; sy < SUPERSAMPLE; sy++) {
                    for (int sx = 0; sx < SUPERSAMPLE; sx++) {
                        float sampleX = px + (sx + 0.5F) / SUPERSAMPLE;
                        float sampleY = py + (sy + 0.5F) / SUPERSAMPLE;
                        boolean outer = insideRounded(sampleX, sampleY,
                                TEXTURE_SIZE * 0.5F, TEXTURE_SIZE * 0.5F,
                                TEXTURE_SIZE * 0.5F, TEXTURE_SIZE * 0.5F, corner);
                        if (!outer) continue;

                        boolean insideInner = outline && insideRounded(sampleX, sampleY,
                                TEXTURE_SIZE * 0.5F, TEXTURE_SIZE * 0.5F,
                                TEXTURE_SIZE * 0.5F - stroke,
                                TEXTURE_SIZE * 0.5F - stroke,
                                Math.max(1.0F, corner - stroke));
                        if (!insideInner) covered++;
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
                () -> "Ruffy rounded " + (outline ? "outline " : "fill ") + radius,
                image);
        minecraft.getTextureManager().register(id, dynamic);
        dynamic.upload();
        return id;
    }

    /** Signed-distance test used by the 2x coverage mask. */
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
