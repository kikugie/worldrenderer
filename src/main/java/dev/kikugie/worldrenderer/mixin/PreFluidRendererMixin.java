package dev.kikugie.worldrenderer.mixin;

import dev.kikugie.worldrenderer.util.WRFluidRenderer;
import net.minecraft.client.render.block.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidRenderer.class, priority = 900)
public class PreFluidRendererMixin {
    @SuppressWarnings("ConstantConditions")
    @Inject(method = "onResourceReload", at = @At("RETURN"), cancellable = true)
    private void cancelFabricRenderer(CallbackInfo ci) {
        if ((Object) this instanceof WRFluidRenderer) ci.cancel();
    }
}