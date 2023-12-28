package dev.kikugie.worldrenderer.util

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.block.BlockModelRenderer
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.random.Random
import net.minecraft.world.BlockRenderView
import java.util.*

class WRBlockRenderer : BlockModelRenderer(MinecraftClient.getInstance().blockColors) {
    private var cullingOverrides = 0

    fun setCullDirection(direction: Direction, alwaysDraw: Boolean) {
        if (alwaysDraw) cullingOverrides = cullingOverrides or (1 shl direction.id)
    }

    fun clearCullingOverrides() {
        cullingOverrides = 0
    }

    private fun shouldAlwaysDraw(direction: Direction): Boolean {
        return cullingOverrides and (1 shl direction.id) != 0
    }

    override fun renderSmooth(
        world: BlockRenderView,
        model: BakedModel,
        state: BlockState,
        pos: BlockPos,
        matrices: MatrixStack,
        consumer: VertexConsumer,
        cull: Boolean,
        random: Random,
        seed: Long,
        overlay: Int
    ) {
        val fs = FloatArray(DIRECTIONS.size * 2)
        val bitSet = BitSet(3)
        val ambientOcclusionCalculator = AmbientOcclusionCalculator()
        val mutable = pos.mutableCopy()

        for (direction in DIRECTIONS) {
            random.setSeed(seed)
            val list = model.getQuads(state, direction, random)
            if (list.isNotEmpty()) {
                mutable[pos] = direction
                if (!cull || shouldAlwaysDraw(direction) || Block.shouldDrawSide(
                        state,
                        world,
                        pos,
                        direction,
                        mutable
                    )
                ) {
                    renderQuadsSmooth(
                        world,
                        state,
                        if (shouldAlwaysDraw(direction)) pos.add(0, 500, 0) else pos,
                        matrices,
                        consumer,
                        list,
                        fs,
                        bitSet,
                        ambientOcclusionCalculator,
                        overlay
                    )
                }
            }
        }

        random.setSeed(seed)
        val quads = model.getQuads(state, null, random)
        if (quads.isNotEmpty()) {
            renderQuadsSmooth(
                world,
                state,
                pos,
                matrices,
                consumer,
                quads,
                fs,
                bitSet,
                ambientOcclusionCalculator,
                overlay
            )
        }
    }

    override fun renderFlat(
        world: BlockRenderView,
        model: BakedModel,
        state: BlockState,
        pos: BlockPos,
        matrices: MatrixStack,
        consumer: VertexConsumer,
        cull: Boolean,
        random: Random,
        seed: Long,
        overlay: Int
    ) {
        val bitSet = BitSet(3)
        val mutable = pos.mutableCopy()

        for (direction in DIRECTIONS) {
            random.setSeed(seed)
            val list = model.getQuads(state, direction, random)
            if (list.isNotEmpty()) {
                mutable[pos] = direction
                if (!cull || shouldAlwaysDraw(direction) || Block.shouldDrawSide(
                        state,
                        world,
                        pos,
                        direction,
                        mutable
                    )
                ) {
                    val i = WorldRenderer.getLightmapCoordinates(world, state, mutable)
                    renderQuadsFlat(
                        world,
                        state,
                        if (shouldAlwaysDraw(direction)) pos.add(0, 500, 0) else pos,
                        i,
                        overlay,
                        false,
                        matrices,
                        consumer,
                        list,
                        bitSet
                    )
                }
            }
        }

        random.setSeed(seed)
        val list2 = model.getQuads(state, null, random)
        if (list2.isNotEmpty()) {
            renderQuadsFlat(world, state, pos, -1, overlay, true, matrices, consumer, list2, bitSet)
        }
    }

    companion object {
        val DIRECTIONS = Direction.entries.toTypedArray()
    }
}