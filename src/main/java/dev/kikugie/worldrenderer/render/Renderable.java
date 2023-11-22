package dev.kikugie.worldrenderer.render;

import dev.kikugie.worldrenderer.property.RenderPropertyBundle;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public interface Renderable {
    default void prepare() {
    }

    void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta);

    void draw(Matrix4f modelViewMatrix);

    default void cleanUp() {
    }

    default void dispose() {
    }

    RenderPropertyBundle getProperties();
}