package dev.kikugie.worldrenderer.mixin;

import dev.kikugie.worldrenderer.util.WRFluidRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidRenderer.class, priority = 1100)
public class PostFluidRendererMixin {
    @SuppressWarnings("MixinAnnotationTarget")
    @Shadow(remap = false)
    @Final
    private ThreadLocal<Boolean> fabric_customRendering;

    @SuppressWarnings({"UnresolvedMixinReference", "MixinAnnotationTarget", "CancellableInjectionUsage", "ConstantConditions"})
    @Inject(method = "tessellateViaHandler", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelFabricOnTwitter(BlockRenderView view, BlockPos pos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfo delegateInfo, CallbackInfo info) {
        if ((Object) this instanceof WRFluidRenderer) {
            this.fabric_customRendering.set(true);
            info.cancel();
        }
    }
}