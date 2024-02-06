package dev.kikugie.worldrenderer.render

import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f
import org.joml.Vector4f
import java.util.function.Consumer


object RenderableDispatcher {
    @Suppress("DEPRECATION")
    fun draw(renderable: Renderable, aspectRatio: Float, tickDelta: Float, transformer: Consumer<MatrixStack>) {
        renderable.prepare()

        // Prepare model view matrix
        with(RenderSystem.getModelViewStack()) {
            push()
            loadIdentity()
            transformer.accept(this)
            renderable.properties.applyToViewMatrix(this)
            RenderSystem.applyModelViewMatrix()
            RenderSystem.backupProjectionMatrix()
            val projectionMatrix = Matrix4f().setOrtho(-aspectRatio, aspectRatio, -1f, 1f, -1000f, 3000f)

            // Unproject to get the camera position for vertex sorting
            /*? if >=1.20 {*//*
            val camPos = Vector4f(0f, 0f, 0f, 1f).also {
                it.mul(Matrix4f(projectionMatrix).invert()).mul(Matrix4f(peek().positionMatrix).invert())
                val inverted = Matrix4f(projectionMatrix)
                inverted.invert()
                it.mul(inverted)
                val model = Matrix4f(peek().positionMatrix)
                model.invert()
                it.mul(model)
            }
            *//*?} */

            RenderSystem.setProjectionMatrix(
                projectionMatrix,
                /*? if >=1.20 */
                /*com.mojang.blaze3d.systems.VertexSorter.byDistance(-camPos.x, -camPos.y, -camPos.z)*/
            )
            RenderSystem.runAsFancy {
                renderable.emitVertices(
                    MatrixStack(),
                    MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers,
                    tickDelta
                )
                renderable.draw(peek().positionMatrix)
            }
            pop()
            RenderSystem.applyModelViewMatrix()
            renderable.cleanUp()
            RenderSystem.restoreProjectionMatrix()
        }
    }
}