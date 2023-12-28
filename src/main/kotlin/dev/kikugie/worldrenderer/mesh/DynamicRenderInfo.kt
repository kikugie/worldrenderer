package dev.kikugie.worldrenderer.mesh

import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.Entity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class DynamicRenderInfo(
    val blockEntities: Map<BlockPos, BlockEntity> = mutableMapOf(),
    val entities: Multimap<Vec3d, EntityEntry> = HashMultimap.create()
) {
    val isEmpty: Boolean
        get() = blockEntities.isEmpty() && entities.isEmpty

    data class EntityEntry(val entity: Entity, val light: Int)
    companion object {
        var EMPTY = DynamicRenderInfo(ImmutableMap.of(), ImmutableMultimap.of())
    }
}