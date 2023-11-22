package dev.kikugie.worldrenderer.mixin;

import net.minecraft.client.render.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Accessor("buffer")
    ByteBuffer worldrenderer$getBuffer();

    @Accessor("buffer")
    void worldrenderer$setBuffer(ByteBuffer buffer);
}
