package dev.kikugie.worldrenderer.render

import dev.kikugie.worldrenderer.property.RenderPropertyBundle
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f

interface Renderable {
    val properties: RenderPropertyBundle
    fun prepare() {}
    fun emitVertices(matrices: MatrixStack, vertexConsumers: VertexConsumerProvider, tickDelta: Float)
    fun draw(modelViewMatrix: Matrix4f)
    fun cleanUp() {}
}