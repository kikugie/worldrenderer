package dev.kikugie.worldrenderer.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import java.util.BitSet;
import java.util.List;

public class WRBlockModelRenderer extends BlockModelRenderer {

    private static final Direction[] DIRECTIONS = Direction.values();
    private byte cullingOverrides = 0;

    public WRBlockModelRenderer() {
        super(MinecraftClient.getInstance().getBlockColors());
    }

    public void setCullDirection(Direction direction, boolean alwaysDraw) {
        if (!alwaysDraw) return;
        this.cullingOverrides |= (byte) (1 << direction.getId());
    }

    public void clearCullingOverrides() {
        this.cullingOverrides = 0;
    }

    private boolean shouldAlwaysDraw(Direction direction) {
        return (this.cullingOverrides & (1 << direction.getId())) != 0;
    }

    @Override
    public void renderSmooth(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        float[] fs = new float[DIRECTIONS.length * 2];
        BitSet bitSet = new BitSet(3);
        BlockModelRenderer.AmbientOcclusionCalculator ambientOcclusionCalculator = new BlockModelRenderer.AmbientOcclusionCalculator();
        BlockPos.Mutable mutable = pos.mutableCopy();

        for (Direction direction : DIRECTIONS) {
            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random);
            if (!list.isEmpty()) {
                mutable.set(pos, direction);
                if (!cull || shouldAlwaysDraw(direction) || Block.shouldDrawSide(state, world, pos, direction, mutable)) {
                    this.renderQuadsSmooth(world, state, !shouldAlwaysDraw(direction) ? pos : pos.add(0, 500, 0), matrices, vertexConsumer, list, fs, bitSet, ambientOcclusionCalculator, overlay);
                }
            }
        }

        random.setSeed(seed);
        List<BakedQuad> quads = model.getQuads(state, null, random);
        if (!quads.isEmpty()) {
            this.renderQuadsSmooth(world, state, pos, matrices, vertexConsumer, quads, fs, bitSet, ambientOcclusionCalculator, overlay);
        }

    }

    public void renderFlat(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrices, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        BitSet bitSet = new BitSet(3);
        BlockPos.Mutable mutable = pos.mutableCopy();

        for (Direction direction : DIRECTIONS) {
            random.setSeed(seed);
            List<BakedQuad> list = model.getQuads(state, direction, random);
            if (!list.isEmpty()) {
                mutable.set(pos, direction);
                if (!cull || shouldAlwaysDraw(direction) || Block.shouldDrawSide(state, world, pos, direction, mutable)) {
                    int i = WorldRenderer.getLightmapCoordinates(world, state, mutable);
                    this.renderQuadsFlat(world, state, !shouldAlwaysDraw(direction) ? pos : pos.add(0, 500, 0), i, overlay, false, matrices, vertexConsumer, list, bitSet);
                }
            }
        }

        random.setSeed(seed);
        List<BakedQuad> list2 = model.getQuads(state, null, random);
        if (!list2.isEmpty()) {
            this.renderQuadsFlat(world, state, pos, -1, overlay, true, matrices, vertexConsumer, list2, bitSet);
        }

    }

}