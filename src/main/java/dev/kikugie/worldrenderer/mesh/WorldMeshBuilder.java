package dev.kikugie.worldrenderer.mesh;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class WorldMeshBuilder {
    private final BlockRenderView world;
    private final BlockPos origin;
    private final BlockPos end;
    private Supplier<List<Entity>> entitySupplier = Collections::emptyList;
    private Runnable preRender = () -> {};
    private Runnable postRender = () -> {};
    private Vec3d camera = new Vec3d(1000.0, 0, 0);

    public WorldMeshBuilder(BlockRenderView world, BlockPos origin, BlockPos end) {
        this.world = world;
        this.origin = origin;
        this.end = end;
    }

    public void setEntitySupplier(Supplier<List<Entity>> entitySupplier) {
        this.entitySupplier = entitySupplier;
    }

    public void setPreRender(Runnable preRender) {
        this.preRender = preRender;
    }

    public void setPostRender(Runnable postRender) {
        this.postRender = postRender;
    }

    public void setCamera(Vec3d camera) {
        this.camera = camera;
    }

    public WorldMesh build() {
        return new WorldMesh(this.world, this.origin, this.end, this.camera, this.entitySupplier, this.preRender, this.postRender);
    }
}