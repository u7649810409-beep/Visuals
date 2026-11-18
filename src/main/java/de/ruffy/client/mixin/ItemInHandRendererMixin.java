package de.ruffy.client.mixin;

import de.ruffy.client.RuffyConfig;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Smooths both the empty arm and held-item swing using the shared animation settings. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @ModifyVariable(
            method = "swingArm",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float ruffy$smoothBareArmSwing(float vanillaProgress) {
        return ruffy$curve(vanillaProgress);
    }

    @ModifyVariable(
            method = "applyItemArmAttackTransform",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float ruffy$smoothHeldItemSwing(float vanillaProgress) {
        return ruffy$curve(vanillaProgress);
    }

    private static float ruffy$curve(float vanillaProgress) {
        if (!RuffyConfig.get("swing_animation")
                || !RuffyConfig.get("smooth_animations")) {
            return vanillaProgress;
        }

        float progress = RuffyAnimation.clamp01(vanillaProgress);
        float eased = RuffyAnimation.easeInOut(progress);
        float smoothness = RuffyConfig.getInt("animation_smoothness") / 100.0F;
        float influence = 0.24F + smoothness * 0.68F;
        return RuffyAnimation.clamp01(progress + (eased - progress) * influence);
    }
}
