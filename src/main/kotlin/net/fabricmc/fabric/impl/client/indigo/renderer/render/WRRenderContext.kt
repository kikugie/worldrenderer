package net.fabricmc.fabric.impl.client.indigo.renderer.render

import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoLuminanceFix
import net.minecraft.block.BlockState
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.crash.CrashException
import net.minecraft.util.crash.CrashReport
import net.minecraft.util.crash.CrashReportSection
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.BlockRenderView
import java.util.function.Function

@Suppress("UnstableApiUsage")
class WRRenderContext(
    private val blockView: BlockRenderView,
    private val bufferFunc: Function<RenderLayer, VertexConsumer>
) : AbstractBlockRenderContext() {
    init {
        blockInfo.prepareForWorld(blockView, true)
        blockInfo.random = Random.create()
    }

    fun tessellateBlock(
        blockView: BlockRenderView?,
        blockState: BlockState,
        blockPos: BlockPos?,
        model: BakedModel,
        matrixStack: MatrixStack
    ) {
        try {
            val vec3d = blockState.getModelOffset(blockView, blockPos)
            matrixStack.translate(vec3d.x, vec3d.y, vec3d.z)
            matrix = matrixStack.peek().positionMatrix
            normalMatrix = matrixStack.peek().normalMatrix
            with(blockInfo) {
                recomputeSeed = true
                aoCalc.clear()
                prepareForBlock(blockState, blockPos, model.useAmbientOcclusion())
                model.emitBlockQuads(
                    this.blockView,
                    this.blockState,
                    this.blockPos,
                    this.randomSupplier,
                    this@WRRenderContext
                )
            }
        } catch (throwable: Throwable) {
            val crashReport = CrashReport.create(throwable, "Tessellating block in WorldRenderer mesh")
            val crashReportSection = crashReport.addElement("Block being tessellated")
            CrashReportSection.addBlockInfo(crashReportSection, blockView, blockPos, blockState)
            throw CrashException(crashReport)
        }
    }

    override fun createAoCalc(blockInfo: BlockRenderInfo): AoCalculator {
        return object : AoCalculator(blockInfo) {
            override fun light(pos: BlockPos, state: BlockState): Int {
                return WorldRenderer.getLightmapCoordinates(blockView, state, pos)
            }

            override fun ao(pos: BlockPos, state: BlockState): Float {
                return AoLuminanceFix.INSTANCE.apply(blockView, pos, state)
            }
        }
    }

    override fun getVertexConsumer(layer: RenderLayer): VertexConsumer {
        return bufferFunc.apply(layer)
    }
}