package dev.kikugie.testmod

import dev.kikugie.worldrenderer.render.Renderable
import dev.kikugie.worldrenderer.render.RenderableDispatcher
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

class RenderScreen(private val renderable: Renderable) : Screen(Text.of("")) {

    override fun render(
        /*? if >=1.20 */
        /*context: net.minecraft.client.gui.DrawContext,*/
        /*? if <1.20 */
        context: net.minecraft.client.util.math.MatrixStack,
        mouseX: Int,
        mouseY: Int,
        delta: Float
    ) {
        super.render(context, mouseX, mouseY, delta)
        val ratio = width / height.toFloat()
        RenderableDispatcher.draw(renderable, ratio, client?.tickDelta ?: 0F) {}
    }
}