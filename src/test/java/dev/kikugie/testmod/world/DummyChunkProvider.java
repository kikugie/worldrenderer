package dev.kikugie.testmod.world;

import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.LightSourceView;
import org.jetbrains.annotations.Nullable;

public class DummyChunkProvider implements ChunkProvider {
    private final BlockView world;

    public DummyChunkProvider(BlockView world) {
        this.world = world;
    }

    @Nullable
    @Override
    public LightSourceView getChunk(int chunkX, int chunkZ) {
        return null;
    }

    @Override
    public BlockView getWorld() {
        return this.world;
    }
}