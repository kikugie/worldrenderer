package dev.kikugie.worldrenderer.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kikugie.worldrenderer.mesh.WorldMesh;
import dev.kikugie.worldrenderer.property.RenderPropertyBundle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class AreaRenderable implements Renderable {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final WorldMesh mesh;
    private final Vec3i size;
    private final RenderPropertyBundle properties;

    public AreaRenderable(WorldMesh mesh, RenderPropertyBundle properties) {
        this.mesh = mesh;
        this.properties = properties;
        this.size = mesh.getEnd().subtract(mesh.getOrigin()).add(1, 1, 1);
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        if (!this.mesh.canRender()) return;

        matrices.loadIdentity();
        matrices.translate(-this.size.getX() / 2f, -this.size.getY() / 2f, -this.size.getZ() / 2f);

        renderBlockEntities(matrices, vertexConsumers);
        drawLight(RenderSystem.getModelViewMatrix());
        drawBuffers();
        renderEntities(matrices, vertexConsumers, tickDelta);

        assert this.client.player != null;
        Vec3d diff = Vec3d.of(this.mesh.getOrigin()).subtract(this.client.player.getPos());
        matrices.translate(-diff.x, -diff.y + 1.65, -diff.z);
    }

    @Override
    public void draw(Matrix4f modelViewMatrix) {
        if (!this.mesh.canRender()) return;

        drawLight(modelViewMatrix);

        final var meshStack = new MatrixStack();
        meshStack.multiplyPositionMatrix(modelViewMatrix);
        meshStack.translate(-this.size.getX() / 2f, -this.size.getY() / 2f, -this.size.getZ() / 2f);
        this.mesh.render(meshStack);
    }

    @Override
    public RenderPropertyBundle getProperties() {
        return this.properties;
    }

    private void renderBlockEntities(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        this.mesh.getRenderInfo().getBlockEntities().forEach((blockPos, entity) -> {
            matrices.push();
            matrices.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            this.client.getBlockEntityRenderDispatcher().render(entity, 0, matrices, vertexConsumers);
            matrices.pop();
        });
    }

    private void renderEntities(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        this.mesh.getRenderInfo().getEntities().forEach((vec3d, entry) -> {
            vec3d = entry.entity().getLerpedPos(tickDelta).subtract(this.mesh.getOrigin().getX(), this.mesh.getOrigin().getY(), this.mesh.getOrigin().getZ());
            this.client.getEntityRenderDispatcher().render(entry.entity(), vec3d.x, vec3d.y, vec3d.z, entry.entity().getYaw(tickDelta), tickDelta, matrices, vertexConsumers, entry.light());
            drawLight(RenderSystem.getModelViewMatrix());
        });
    }

    public void drawLight(Matrix4f modelViewMatrix) {
        final var lightDirection = new Vector4f(this.properties.lightAngle().get() / 90f, .35f, 1, 0);
        final var lightTransform = new Matrix4f(modelViewMatrix);
        lightTransform.invert();
        lightDirection.mul(lightTransform);

        final var transformedLightDirection = new Vector3f(lightDirection.x, lightDirection.y, lightDirection.z);
        RenderSystem.setShaderLights(transformedLightDirection, transformedLightDirection);

        drawBuffers();
    }

    private void drawBuffers() {
        client.getBufferBuilders().getEntityVertexConsumers().draw();
    }
}