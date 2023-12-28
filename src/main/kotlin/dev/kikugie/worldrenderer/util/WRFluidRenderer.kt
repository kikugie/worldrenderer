package dev.kikugie.worldrenderer.util

import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.block.FluidRenderer
import org.joml.Matrix4f

class WRFluidRenderer : FluidRenderer() {
    lateinit var matrix: Matrix4f

    init {
        onResourceReload()
    }
    override fun vertex(
        vertexConsumer: VertexConsumer,
        x: Double,
        y: Double,
        z: Double,
        red: Float,
        green: Float,
        blue: Float,
        u: Float,
        v: Float,
        light: Int
    ) {
        vertexConsumer.vertex(matrix, x.toFloat(), y.toFloat(), z.toFloat()).color(red, green, blue, 1.0f).texture(u, v)
            .light(light).normal(0.0f, 1.0f, 0.0f).next()
    }
}