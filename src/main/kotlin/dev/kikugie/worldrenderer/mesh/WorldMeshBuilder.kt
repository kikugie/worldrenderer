package dev.kikugie.worldrenderer.mesh

import com.google.common.collect.HashMultimap
import com.mojang.blaze3d.systems.RenderSystem
import dev.kikugie.worldrenderer.Reference
import dev.kikugie.worldrenderer.mixin.BufferBuilderAccessor
import dev.kikugie.worldrenderer.mixin.GlAllocationUtilsAccessor
import dev.kikugie.worldrenderer.util.EntitySupplier
import dev.kikugie.worldrenderer.util.WRBlockRenderer
import dev.kikugie.worldrenderer.util.WRFluidRenderer
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer
import net.fabricmc.fabric.impl.client.indigo.renderer.render.WRRenderContext
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.BlockRenderType
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.VertexBuffer
import net.minecraft.client.render.*
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.random.Random
import net.minecraft.world.BlockRenderView
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

class WorldMeshBuilder internal constructor(
    private val world: BlockRenderView,
    private val origin: BlockPos,
    private val end: BlockPos,
    private val camera: Vec3d,
    private val entities: EntitySupplier
) {
    private val client = MinecraftClient.getInstance()
    private val random = Random.createLocal()

    var progress = 0.0
        private set
    private val blockEntities = mutableMapOf<BlockPos, BlockEntity>()
    private val builderStorage = mutableMapOf<RenderLayer, BufferBuilder>()
    val vertexStorage = mutableMapOf<RenderLayer, VertexBuffer>()
    private val renderManager = client.blockRenderManager
    private val blockRenderer = WRBlockRenderer()
    private val fluidRenderer = WRFluidRenderer()
    lateinit var renderInfo: DynamicRenderInfo
        private set

    @Suppress("UnstableApiUsage")
    private val context: WRRenderContext? = try {
        (RendererAccess.INSTANCE.renderer as? IndigoRenderer)?.let {
            WRRenderContext(world) { l ->
                getOrCreateBuffer(
                    builderStorage,
                    l
                )
            }
        }
    } catch (e: Throwable) {
        val fapi = FabricLoader.getInstance().getModContainer("worldrenderer")
            .get().metadata.getCustomValue("worldrenderer:fapi_build_version").asString
        Reference.LOGGER.error(
            """
            Could not create a context for rendering Fabric API models.
            This is most likely due to an incompatible Fabric API version -
            this build of WorldRenderer was compiled against $fapi, try that instead
        """.trimIndent()
        )
        null
    }

    private fun getOrCreateBuffer(
        builderStorage: MutableMap<RenderLayer, BufferBuilder>,
        layer: RenderLayer
    ): VertexConsumer = builderStorage.computeIfAbsent(layer) {
        BufferBuilder(it.expectedBufferSize).apply {
            begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL)
        }
    }

    internal fun build() {
        val futureEntities = processEntities()
        processBlocks(MatrixStack())
        sortTranslucent()
        cleanUpBuffers()

        val entityMap = HashMultimap.create<Vec3d, DynamicRenderInfo.EntityEntry>()
        futureEntities.join().forEach {
            val newPos = it.entity.pos.subtract(origin.x.toDouble(), origin.y.toDouble(), origin.z.toDouble())
            entityMap.put(newPos, it)
        }

        renderInfo = DynamicRenderInfo(blockEntities, entityMap)
    }

    private fun processEntities(): CompletableFuture<List<DynamicRenderInfo.EntityEntry>> {
        val future = CompletableFuture<List<DynamicRenderInfo.EntityEntry>>()
        val dispatcher = client.entityRenderDispatcher
        client.execute {
            future.complete(entities().map {
                DynamicRenderInfo.EntityEntry(
                    it,
                    dispatcher.getLight(it, 0F)
                )
            })
        }
        return future
    }

    private fun processBlocks(matrices: MatrixStack) {
        var current = 0
        val volume = ((end.x - origin.x + 1) * (end.y - origin.y + 1) * (end.z - origin.z + 1)).toDouble()

        for (pos in BlockPos.iterate(origin, end)) {
            progress = current++ / volume
            val state = world.getBlockState(pos)
            if (state.isAir) continue

            val renderPos = pos.subtract(origin)
            world.getBlockEntity(pos)?.let { blockEntities[renderPos.toImmutable()] = it }
            world.getFluidState(pos).takeIf { !it.isEmpty }?.let {
                val layer = RenderLayers.getFluidLayer(it)
                matrices.push()
                matrices.translate(-(pos.x and 15).toFloat(), -(pos.y and 15).toFloat(), -(pos.z and 15).toFloat())
                matrices.translate(renderPos.x.toFloat(), renderPos.y.toFloat(), renderPos.z.toFloat())
                fluidRenderer.matrix = matrices.peek().positionMatrix
                fluidRenderer.render(world, pos, getOrCreateBuffer(builderStorage, layer), state, it)
                matrices.pop()
            }

            matrices.push()
            matrices.translate(renderPos.x.toFloat(), renderPos.y.toFloat(), renderPos.z.toFloat())
            with(blockRenderer) {
                clearCullingOverrides()
                setCullDirection(Direction.EAST, pos.x == end.x)
                setCullDirection(Direction.WEST, pos.x == origin.x)
                setCullDirection(Direction.SOUTH, pos.z == end.z)
                setCullDirection(Direction.NORTH, pos.z == origin.z)
                setCullDirection(Direction.UP, pos.y == end.y)
                setCullDirection(Direction.DOWN, pos.y == origin.y)
            }

            val layer = RenderLayers.getBlockLayer(state)
            val model = renderManager.getModel(state)
            if (context != null && !(model as FabricBakedModel).isVanillaAdapter) {
                context.tessellateBlock(world, state, pos, model, matrices)
            } else if (state.renderType == BlockRenderType.MODEL) {
                blockRenderer.render(
                    world,
                    model,
                    state,
                    pos,
                    matrices,
                    getOrCreateBuffer(builderStorage, layer),
                    true,
                    random,
                    state.getRenderingSeed(pos),
                    OverlayTexture.DEFAULT_UV
                )
            }

            matrices.pop()
        }
    }

    private fun sortTranslucent() {
        if (builderStorage.containsKey(RenderLayer.getTranslucent())) {
            val translucentBuilder = builderStorage[RenderLayer.getTranslucent()]
            /*? if >=1.20 {*//*
            translucentBuilder?.setSorter(
                com.mojang.blaze3d.systems.VertexSorter.byDistance(
                    camera.x.toFloat() - origin.x,
                    camera.y.toFloat() - origin.y,
                    camera.z.toFloat() - origin.z
                )
            )
            *//*?} else {*/
            translucentBuilder?.sortFrom(
                camera.x.toFloat() - origin.x,
                camera.y.toFloat() - origin.y,
                camera.z.toFloat() - origin.z
            )
            /*?} */
        }
    }

    private fun cleanUpBuffers() {
        RenderSystem.recordRenderCall {
            vertexStorage.values.forEach { it.close() }
            vertexStorage.clear()
            builderStorage.forEach { (layer: RenderLayer, builder: BufferBuilder) ->
                val newBuffer = VertexBuffer(/*?if >=1.20 {*//*VertexBuffer.Usage.STATIC*//*?} */)
                newBuffer.bind()
                newBuffer.upload(builder.end())
                GlAllocationUtilsAccessor.`worldrenderer$getAllocator`().free(
                    MemoryUtil.memAddress(
                        (builder as BufferBuilderAccessor).`worldrenderer$getBuffer`(),
                        0
                    )
                )

                // primarily here to inform ModernFix about what we did
                (builder as BufferBuilderAccessor).`worldrenderer$setBuffer`(null)
                vertexStorage.put(layer, newBuffer)?.apply { close() }
            }
        }
    }

    companion object {
        fun launch(
            world: BlockRenderView,
            origin: BlockPos,
            end: BlockPos,
            camera: Vec3d,
            entities: EntitySupplier,
            executor: Executor,
            onComplete: (WorldMeshBuilder, Throwable?) -> Unit
        ) = WorldMeshBuilder(world, origin, end, camera, entities).also {
            CompletableFuture.runAsync(it::build, executor).whenComplete { _, e -> onComplete(it, e) }
        }
    }
}