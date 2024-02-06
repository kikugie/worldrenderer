package dev.kikugie.worldrenderer.mesh

import com.mojang.blaze3d.systems.RenderSystem
import dev.kikugie.worldrenderer.Reference
import dev.kikugie.worldrenderer.util.EntitySupplier
import net.minecraft.client.gl.VertexBuffer
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.BlockRenderView
import org.joml.Matrix4f
import java.util.concurrent.Executor

@Suppress("unused")
class WorldMesh(
    val world: BlockRenderView,
    val origin: BlockPos,
    val end: BlockPos,
    val camera: Vec3d,
    private val entities: EntitySupplier,
    private val preRender: () -> Unit,
    private val postRender: () -> Unit
) {
    private var vertexStorage = mapOf<RenderLayer, VertexBuffer>()
    private var builder: WorldMeshBuilder? = null
    var renderInfo = DynamicRenderInfo.EMPTY
        private set
    var state = MeshState.NEW
        private set
    val canRender: Boolean
        get() = state.canRender
    val progress: Double
        get() = if (canRender) 1.0 else builder?.progress ?: 0.0

    @Synchronized
    fun scheduleRebuild(executor: Executor = Util.getMainWorkerExecutor()): WorldMeshBuilder {
        if (builder != null) return builder!!
        state = if (state == MeshState.NEW) MeshState.BUILDING else MeshState.REBUILDING
        return WorldMeshBuilder.launch(world, origin, end, camera, entities, executor) { it, e ->
            builder = null
            renderInfo = DynamicRenderInfo.EMPTY
            vertexStorage.values.forEach { it.close() }

            if (e == null) {
                vertexStorage = it.vertexStorage
                renderInfo = it.renderInfo
                state = MeshState.READY
            } else {
                state = MeshState.CORRUPT
                Reference.LOGGER.error("Mesh corrupted during build:\n", e)
            }
        }.also { builder = it }
    }

    fun render(matrices: MatrixStack) {
        if (!canRender) return

        val matrix = matrices.peek().positionMatrix
        val translucent = RenderLayer.getTranslucent()

        vertexStorage.forEach { (layer, buffer) ->
            if (layer != translucent) drawBuffer(buffer, layer, matrix)
        }
        vertexStorage[translucent]?.also { drawBuffer(it, translucent, matrix) }
        VertexBuffer.unbind()
    }

    private fun drawBuffer(buffer: VertexBuffer, layer: RenderLayer, matrix: Matrix4f) {
        layer.startDrawing()
        preRender()

        buffer.bind()
        buffer.draw(matrix, RenderSystem.getProjectionMatrix(), RenderSystem.getShader())

        postRender()
        layer.endDrawing()
    }

    companion object {
        fun create(init: MeshBuilder.() -> Unit): WorldMesh = MeshBuilder()
            .apply(init)
            .let { WorldMesh(it.world, it.origin, it.end, it.camera, it.entities, it.preRender, it.postRender) }
    }
}