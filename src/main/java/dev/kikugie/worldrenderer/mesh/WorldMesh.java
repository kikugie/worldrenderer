package dev.kikugie.worldrenderer.mesh;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.kikugie.worldrenderer.Reference;
import dev.kikugie.worldrenderer.util.EntitiesSupplier;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class WorldMesh {
    private final BlockRenderView world;
    private final BlockPos origin;
    private final BlockPos end;
    private final EntitiesSupplier entitySupplier;
    private final Runnable preRender;
    private final Runnable postRender;
    private final Vec3d camera;
    private final Map<RenderLayer, VertexBuffer> vertexStorage = new HashMap<>();
    private DynamicRenderInfo renderInfo = DynamicRenderInfo.EMPTY;
    @Nullable
    private MeshBuilder builder = null;
    private MeshState state = MeshState.NEW;

    public WorldMesh(BlockRenderView world, BlockPos origin, BlockPos end, Vec3d camera, EntitiesSupplier entitySupplier, Runnable preRender, Runnable postRender) {
        this.world = world;
        this.origin = origin;
        this.end = end;
        this.camera = camera;
        this.entitySupplier = entitySupplier;
        this.preRender = preRender;
        this.postRender = postRender;
    }

    public static WorldMeshBuilder builder(BlockRenderView world, BlockPos origin, BlockPos end) {
        return new WorldMeshBuilder(world, origin, end);
    }

    public void render(MatrixStack matrices) throws IllegalStateException {
        if (!canRender()) {
            throw new IllegalStateException("World mesh not prepared!");
        }

        var matrix = matrices.peek().getPositionMatrix();
        var translucent = RenderLayer.getTranslucent();

        this.vertexStorage.forEach((layer, buffer) -> {
            if (layer == translucent) return;
            this.drawBuffer(buffer, layer, matrix);
        });

        if (this.vertexStorage.containsKey(translucent)) {
            this.drawBuffer(this.vertexStorage.get(translucent), translucent, matrix);
        }

        VertexBuffer.unbind();
    }

    private void drawBuffer(VertexBuffer vertexBuffer, RenderLayer renderLayer, Matrix4f matrix) {
        renderLayer.startDrawing();
        this.preRender.run();

        vertexBuffer.bind();
        vertexBuffer.draw(matrix, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());

        this.postRender.run();
        renderLayer.endDrawing();
    }

    public synchronized MeshBuilder scheduleRebuild() {
        return scheduleRebuild(Util.getMainWorkerExecutor());
    }

    public synchronized MeshBuilder scheduleRebuild(Executor executor) {
        if (this.builder != null) return this.builder;

        this.state = this.state != MeshState.NEW
                ? MeshState.REBUILDING
                : MeshState.BUILDING;
        this.builder = MeshBuilder.launch(this.world, this.origin, this.end, this.camera, this.entitySupplier, executor, (builder, throwable) -> {
            this.builder = null;
            this.renderInfo = DynamicRenderInfo.EMPTY;
            clearVertexStorage();

            if (throwable == null) {
                this.state = MeshState.READY;
                this.vertexStorage.putAll(builder.getVertexStorage());
                this.renderInfo = builder.getRenderInfo();
            } else {
                Reference.LOGGER.warn("Failed to build the mesh", throwable);
                this.state = MeshState.CORRUPT;
            }
        });
        return this.builder;
    }

    private void clearVertexStorage() {
        this.vertexStorage.forEach(($, buffer) -> buffer.close());
        this.vertexStorage.clear();
    }

    /**
     * Returns the build progress from 0 to 1
     */
    public double getProgress() {
        if (canRender()) return 1.0;
        if (!this.state.isBuildStage) return 0.0;
        if (this.builder == null) return 0.0;
        return this.builder.getProgress();
    }

    public DynamicRenderInfo getRenderInfo() {
        return this.renderInfo;
    }

    public BlockRenderView getWorld() {
        return this.world;
    }

    public BlockPos getOrigin() {
        return this.origin;
    }

    public BlockPos getEnd() {
        return this.end;
    }

    public MeshState getState() {
        return this.state;
    }

    public Vec3d getCamera() {
        return this.camera;
    }

    public void setCamera(Vec3d camera) {
        // TODO: updating camera should re-sort faces
        throw new NotImplementedException();
    }

    public boolean canRender() {
        return this.state.canRender;
    }

    public enum MeshState {
        NEW(false, false),
        BUILDING(true, false),
        REBUILDING(true, true),
        READY(false, true),
        CORRUPT(false, false);

        public final boolean isBuildStage;
        public final boolean canRender;

        MeshState(boolean buildStage, boolean canRender) {
            this.isBuildStage = buildStage;
            this.canRender = canRender;
        }
    }
}