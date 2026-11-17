package de.ruffy.client.mixin;

import de.ruffy.client.RuffyConfig;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
        if (!RuffyConfig.get("swing_animation")) {
            return vanillaProgress;
        }

        float progress = Math.max(0.0F, Math.min(1.0F, vanillaProgress));
        float easeOut = 1.0F - (float) Math.pow(1.0F - progress, 2.35);
        float snap = (float) Math.sin(progress * Math.PI) * 0.055F;
        return Math.max(0.0F, Math.min(1.0F, easeOut + snap));
    }
}
