package dev.kikugie.worldrenderer.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.function.Consumer;

/*?>=1.20 {?*//*import com.mojang.blaze3d.systems.VertexSorter;*//*?}?*/

public class RenderableDispatcher {
    @SuppressWarnings("deprecation")
    public static void draw(Renderable renderable, float aspectRatio, float tickDelta, Consumer<MatrixStack> transformer) {

        renderable.prepare();

        // Prepare model view matrix
        final var modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.push();
        modelViewStack.loadIdentity();

        transformer.accept(modelViewStack);

        renderable.getProperties().applyToViewMatrix(modelViewStack);
        RenderSystem.applyModelViewMatrix();

        RenderSystem.backupProjectionMatrix();
        Matrix4f projectionMatrix = new Matrix4f().setOrtho(-aspectRatio, aspectRatio, -1, 1, -1000, 3000);

        // Unproject to get the camera position for vertex sorting
        var camPos = new Vector4f(0, 0, 0, 1);
        camPos.mul(new Matrix4f(projectionMatrix).invert()).mul(new Matrix4f(modelViewStack.peek().getPositionMatrix()).invert());
        RenderSystem.setProjectionMatrix(projectionMatrix /*?>=1.20 {?*//*, VertexSorter.byDistance(-camPos.x, -camPos.y, -camPos.z)*//*?}?*/);

        RenderSystem.runAsFancy(() -> {
            renderable.emitVertices(
                    new MatrixStack(),
                    MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                    tickDelta
            );

            renderable.draw(modelViewStack.peek().getPositionMatrix());
        });

        modelViewStack.pop();
        RenderSystem.applyModelViewMatrix();

        renderable.cleanUp();
        RenderSystem.restoreProjectionMatrix();
    }
}