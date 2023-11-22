package dev.kikugie.worldrenderer.mixin;

import net.minecraft.client.util.GlAllocationUtils;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlAllocationUtils.class)
public interface GlAllocationUtilsAccessor {

    @Accessor("ALLOCATOR")
    static MemoryUtil.MemoryAllocator worldrenderer$getAllocator() {
        throw new UnsupportedOperationException();
    }

}

