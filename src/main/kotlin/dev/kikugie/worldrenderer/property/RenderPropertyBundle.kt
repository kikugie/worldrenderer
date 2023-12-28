package dev.kikugie.worldrenderer.property

import net.minecraft.client.util.math.MatrixStack

interface RenderPropertyBundle {
    val xOffset: IntProperty
    val yOffset: IntProperty

    val scale: DoubleProperty
    val rotation: DoubleProperty
    val slant: DoubleProperty
    val lightAngle: DoubleProperty

    fun applyToViewMatrix(matrices: MatrixStack)
}