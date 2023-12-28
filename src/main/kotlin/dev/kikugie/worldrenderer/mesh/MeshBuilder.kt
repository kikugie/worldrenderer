package dev.kikugie.worldrenderer.mesh

import dev.kikugie.worldrenderer.util.EntitySupplier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.BlockRenderView

class MeshBuilder {
    lateinit var world: BlockRenderView
    lateinit var origin: BlockPos
    lateinit var end: BlockPos
    lateinit var entities: EntitySupplier
    var camera: Vec3d = Vec3d(1000.0, 0.0, 0.0)
    var preRender: () -> Unit = {}
    var postRender: () -> Unit = {}
}