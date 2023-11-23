package dev.kikugie.worldrenderer.mesh;

import com.google.common.collect.HashMultimap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import dev.kikugie.worldrenderer.Reference;
import dev.kikugie.worldrenderer.mixin.BufferBuilderAccessor;
import dev.kikugie.worldrenderer.mixin.GlAllocationUtilsAccessor;
import dev.kikugie.worldrenderer.util.EntitiesSupplier;
import dev.kikugie.worldrenderer.util.WRBlockModelRenderer;
import dev.kikugie.worldrenderer.util.WRFluidRenderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.WRRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

/**
 * This class is responsible for building meshes for rendering in Minecraft.
 * It processes blocks and entities within a specified region of the world
 * and generates vertex data for rendering.
 */
public final class MeshBuilder {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final BlockRenderManager renderManager = this.client.getBlockRenderManager();
    private final WRBlockModelRenderer blockRenderer = new WRBlockModelRenderer();
    private final WRFluidRenderer fluidRenderer = new WRFluidRenderer();
    private final Map<RenderLayer, BufferBuilder> builderStorage = new HashMap<>();
    private final Random random = Random.createLocal();
    @Nullable
    private final WRRenderContext context;
    private final Map<RenderLayer, VertexBuffer> vertexStorage = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();

    private final BlockRenderView world;
    private final BlockPos origin;
    private final BlockPos end;
    private final Vec3d camera;
    private final EntitiesSupplier entitySupplier;

    private final CompletableFuture<Void> future;
    private double progress = 0.0;
    @Nullable
    private DynamicRenderInfo renderInfo = null;

    /**
     * Private because launches the build upon instantiating, use {@link MeshBuilder#launch(BlockRenderView, BlockPos, BlockPos, Vec3d, EntitiesSupplier, Executor, BiConsumer)})}
     */
    private MeshBuilder(BlockRenderView world,
                        BlockPos origin,
                        BlockPos end,
                        Vec3d camera,
                        EntitiesSupplier entitySupplier,
                        Executor executor,
                        BiConsumer<MeshBuilder, @Nullable Throwable> onComplete) {
        this.world = world;
        this.origin = origin;
        this.end = end;
        this.camera = camera;
        this.entitySupplier = entitySupplier;

        WRRenderContext contextConstructor = null;
        try {
            //noinspection UnstableApiUsage
            contextConstructor = RendererAccess.INSTANCE.getRenderer() instanceof IndigoRenderer
                    ? new WRRenderContext(this.world, layer -> this.getOrCreateBuilder(this.builderStorage, layer))
                    : null;
        } catch (Throwable e) {
            String fapiVersion = FabricLoader.getInstance().getModContainer("worldrenderer").get().getMetadata().getCustomValue("worldrenderer:fapi_build_version").getAsString();
            Reference.LOGGER.error(
                    "Could not create a context for rendering Fabric API models. This is most likely due to an incompatible Fabric API version - this build of WorldRenderer was compiled against '{}', try that instead", fapiVersion, e);
        }
        this.context = contextConstructor;
        this.future = CompletableFuture.runAsync(this::build, executor)
                .whenComplete((($, e) -> onComplete.accept(this, e)));
    }

    public static MeshBuilder launch(BlockRenderView world,
                                     BlockPos origin,
                                     BlockPos end,
                                     Vec3d camera,
                                     EntitiesSupplier entitySupplier,
                                     Executor executor,
                                     BiConsumer<MeshBuilder, @Nullable Throwable> onComplete) {
        return new MeshBuilder(world, origin, end, camera, entitySupplier, executor, onComplete);
    }

    private void build() {
        MatrixStack matrices = new MatrixStack();
        CompletableFuture<List<DynamicRenderInfo.EntityEntry>> entitiesFuture = processEntities();
        processBlocks(matrices);
        sortTranslucent();
        cleanUpBuffers();

        var entities = HashMultimap.<Vec3d, DynamicRenderInfo.EntityEntry>create();
        for (var entityEntry : entitiesFuture.join()) {
            entities.put(
                    entityEntry.entity().getPos().subtract(this.origin.getX(), this.origin.getY(), this.origin.getZ()),
                    entityEntry
            );
        }

        this.renderInfo = new DynamicRenderInfo(
                this.blockEntities, entities
        );
    }

    private void cleanUpBuffers() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        RenderSystem.recordRenderCall(() -> {
            this.vertexStorage.forEach((renderLayer, vertexBuffer) -> vertexBuffer.close());
            this.vertexStorage.clear();

            this.builderStorage.forEach((renderLayer, bufferBuilder) -> {
                var newBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);

                newBuffer.bind();
                newBuffer.upload(bufferBuilder.end());

                GlAllocationUtilsAccessor.worldrenderer$getAllocator().free(
                        MemoryUtil.memAddress(((BufferBuilderAccessor) bufferBuilder).worldrenderer$getBuffer(), 0)
                );

                // primarily here to inform ModernFix about what we did
                ((BufferBuilderAccessor) bufferBuilder).worldrenderer$setBuffer(null);

                var discardedBuffer = this.vertexStorage.put(renderLayer, newBuffer);
                if (discardedBuffer != null)
                    discardedBuffer.close();
            });

            future.complete(null);
        });
        future.join();
    }

    private void sortTranslucent() {
        if (this.builderStorage.containsKey(RenderLayer.getTranslucent())) {
            var translucentBuilder = this.builderStorage.get(RenderLayer.getTranslucent());

            translucentBuilder.setSorter(VertexSorter.byDistance(
                    (float) this.camera.x - this.origin.getX(),
                    (float) this.camera.y - this.origin.getY(),
                    (float) this.camera.z - this.origin.getZ()));
        }
    }

    private void processBlocks(MatrixStack matrices) {
        int current = 0;
        final double volume = (this.end.getX() - this.origin.getX() + 1)
                * (this.end.getY() - this.origin.getY() + 1)
                * (this.end.getZ() - this.origin.getZ() + 1);

        for (BlockPos pos : BlockPos.iterate(this.origin, this.end)) {
            current++;
            this.progress = current / volume;

            BlockState state = this.world.getBlockState(pos);
            if (state.isAir()) continue;

            BlockPos renderPos = pos.subtract(this.origin);
            BlockEntity blockEntity = this.world.getBlockEntity(pos);
            if (blockEntity != null)
                this.blockEntities.put(pos, blockEntity);

            if (!this.world.getFluidState(pos).isEmpty()) {
                var fluidState = this.world.getFluidState(pos);
                var fluidLayer = RenderLayers.getFluidLayer(fluidState);

                matrices.push();
                matrices.translate(-(pos.getX() & 15), -(pos.getY() & 15), -(pos.getZ() & 15));
                matrices.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());

                this.fluidRenderer.setMatrix(matrices.peek().getPositionMatrix());
                this.fluidRenderer.render(this.world, pos, this.getOrCreateBuilder(this.builderStorage, fluidLayer), state, fluidState);

                matrices.pop();
            }

            matrices.push();
            matrices.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());

            this.blockRenderer.clearCullingOverrides();
            this.blockRenderer.setCullDirection(Direction.EAST, pos.getX() == this.end.getX());
            this.blockRenderer.setCullDirection(Direction.WEST, pos.getX() == this.origin.getX());
            this.blockRenderer.setCullDirection(Direction.SOUTH, pos.getZ() == this.end.getZ());
            this.blockRenderer.setCullDirection(Direction.NORTH, pos.getZ() == this.origin.getZ());
            this.blockRenderer.setCullDirection(Direction.UP, pos.getY() == this.end.getY());
            this.blockRenderer.setCullDirection(Direction.DOWN, pos.getY() == this.origin.getY());

            var blockLayer = RenderLayers.getBlockLayer(state);

            final var model = this.renderManager.getModel(state);
            if (this.context != null && !model.isVanillaAdapter()) {
                this.context.tessellateBlock(this.world, state, pos, model, matrices);
            } else if (state.getRenderType() == BlockRenderType.MODEL) {
                this.blockRenderer.render(this.world, model, state, pos, matrices, this.getOrCreateBuilder(this.builderStorage, blockLayer), true, this.random, state.getRenderingSeed(pos), OverlayTexture.DEFAULT_UV);
            }

            matrices.pop();
        }
    }

    private CompletableFuture<List<DynamicRenderInfo.EntityEntry>> processEntities() {
        CompletableFuture<List<DynamicRenderInfo.EntityEntry>> entitiesFuture = new CompletableFuture<>();
        this.client.execute(() -> entitiesFuture.complete(this.entitySupplier.getEntities()
                .stream()
                .map(entity -> {
//                    if (this.freezeEntities) {
//                        var originalEntity = entity;
//                        entity = entity.getType().create(client.world);
//
//                        entity.copyFrom(originalEntity);
//                        entity.copyPositionAndRotation(originalEntity);
//                        entity.tick();
//                    }

                    return new DynamicRenderInfo.EntityEntry(
                            entity,
                            this.client.getEntityRenderDispatcher().getLight(entity, 0)
                    );
                }).toList()));
        return entitiesFuture;
    }

    private VertexConsumer getOrCreateBuilder(Map<RenderLayer, BufferBuilder> builderStorage, RenderLayer layer) {
        return builderStorage.computeIfAbsent(layer, renderLayer -> {
            var builder = new BufferBuilder(layer.getExpectedBufferSize());
            builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);

            return builder;
        });
    }

    public double getProgress() {
        return this.progress;
    }

    public CompletableFuture<Void> getFuture() {
        return this.future;
    }

    public DynamicRenderInfo getRenderInfo() {
        return this.renderInfo;
    }

    public Map<RenderLayer, VertexBuffer> getVertexStorage() {
        return this.vertexStorage;
    }
}