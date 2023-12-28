package dev.kikugie.worldrenderer.render

import com.mojang.blaze3d.systems.RenderSystem
import dev.kikugie.worldrenderer.mesh.WorldMesh
import dev.kikugie.worldrenderer.property.RenderPropertyBundle
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f

class AreaRenderable(
    val mesh: WorldMesh,
    override val properties: RenderPropertyBundle
) : Renderable {
    private val client = MinecraftClient.getInstance()
    private val size = mesh.end.subtract(mesh.origin).add(1, 1, 1)
    override fun emitVertices(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, tickDelta: Float) {
        if (!mesh.canRender) return

        with(matrices) {
            loadIdentity()
            translate(-size.x / 2F, -size.y / 2F, -size.z / 2F)

            renderBlockEntities(matrices, vertexConsumers, tickDelta)
            drawLight(RenderSystem.getModelViewMatrix())
            drawBuffers()
            renderEntities(matrices, vertexConsumers, tickDelta)

            Vec3d.of(mesh.origin).subtract(client.player?.pos ?: Vec3d.ZERO).also {
                matrices.translate(-it.x, -it.y + 1.65, -it.z)
            }
        }
    }

    override fun draw(modelViewMatrix: Matrix4f) {
        if (!mesh.canRender) return

        drawLight(modelViewMatrix)
        MatrixStack().apply {
            multiplyPositionMatrix(modelViewMatrix)
            translate(-size.x / 2F, -size.y / 2F, -size.z / 2F)
            mesh.render(this)
        }
    }

    private fun renderBlockEntities(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, tickDelta: Float) {
        for ((pos, block) in mesh.renderInfo.blockEntities) with(matrices) {
            push()
            translate(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
            client.blockEntityRenderDispatcher.render(block, tickDelta, this, vertexConsumers)
            pop()
        }
    }

    private fun renderEntities(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, tickDelta: Float) {
        mesh.renderInfo.entities.forEach { _, entry ->
            val pos = entry.entity
                .getLerpedPos(tickDelta)
                .subtract(mesh.origin.x.toDouble(), mesh.origin.y.toDouble(), mesh.origin.z.toDouble())
            client.entityRenderDispatcher.render(
                entry.entity,
                pos.x,
                pos.y,
                pos.z,
                entry.entity.getYaw(tickDelta),
                tickDelta,
                matrices,
                vertexConsumers,
                entry.light
            )
        }
    }

    private fun drawLight(matrix: Matrix4f) {
        val lightDirection = Vector4f((properties.lightAngle() / 90.0).toFloat(), .35F, 1F, 0F)
        val lightTransform = Matrix4f(matrix)
        lightTransform.invert()
        lightDirection.mul(lightTransform)
        Vector3f(lightDirection.x, lightDirection.y, lightDirection.z).also {
            RenderSystem.setShaderLights(it, it)
        }

        drawBuffers()
    }

    private fun drawBuffers() {
        client.bufferBuilders.entityVertexConsumers.draw()
    }
}