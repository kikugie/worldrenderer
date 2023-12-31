package dev.kikugie.worldrenderer.property

import net.minecraft.client.util.math.MatrixStack

interface RenderPropertyBundle {
    var xOffset: Int
    var yOffset: Int

    var scale: Double
    var rotation: Double
    var slant: Double
    var lightAngle: Double

    fun applyToViewMatrix(matrices: MatrixStack)
}