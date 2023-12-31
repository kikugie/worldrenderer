package dev.kikugie.worldrenderer.property

import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.RotationAxis

class DefaultRenderProperties(maxScale: Double, defaultRotation: Double, defaultSlant: Double,
    private val scaleDiv: Double = 10.0,
    private val posDiv: Double = 26000.0
) : RenderPropertyBundle {
    override var xOffset: Int by PropertyDelegate(IntProperty(0, Int.MIN_VALUE, Int.MAX_VALUE))
    override var yOffset: Int by PropertyDelegate(IntProperty(0, Int.MIN_VALUE, Int.MAX_VALUE))
    override var scale: Double by PropertyDelegate(DoubleProperty(1.0, 0.0001, maxScale))
    override var rotation: Double by PropertyDelegate(DoubleProperty(defaultRotation, 0.0, 360.0, true))
    override var slant: Double by PropertyDelegate(DoubleProperty(defaultSlant, -90.0, 90.0))
    override var lightAngle: Double by PropertyDelegate(DoubleProperty(-45.0, 0.0, 360.0, true))

    override fun applyToViewMatrix(matrices: MatrixStack) {
        with(matrices) {
            (scale / scaleDiv).toFloat().also { scale(it, it, it) }
            translate(xOffset / posDiv, yOffset / -posDiv, 0.0)
            multiply(RotationAxis.POSITIVE_X.rotationDegrees(slant.toFloat()))
            multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation.toFloat()))
        }
    }
}