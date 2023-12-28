package dev.kikugie.testmod

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import dev.kikugie.worldrenderer.mesh.WorldMesh
import dev.kikugie.worldrenderer.property.DefaultRenderProperties
import dev.kikugie.worldrenderer.render.AreaRenderable
import net.minecraft.client.MinecraftClient
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.server.command.CommandManager.*
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.min

object RenderCommand {
    fun register(
        dispatcher: CommandDispatcher<ServerCommandSource>,
        access: CommandRegistryAccess,
        environment: RegistrationEnvironment
    ) {
        dispatcher.register(
            literal("render").then(
                argument(
                    "pos1",
                    BlockPosArgumentType.blockPos()
                ).then(
                    argument(
                        "pos2",
                        BlockPosArgumentType.blockPos()
                    ).executes(::render)
                )
            )
        )
    }

    private fun render(context: CommandContext<ServerCommandSource>): Int {
        val client = MinecraftClient.getInstance()
        val pos1 = BlockPosArgumentType.getBlockPos(context, "pos1")
        val pos2 = BlockPosArgumentType.getBlockPos(context, "pos2")

        val posO = BlockPos(min(pos1.x, pos2.x), min(pos1.y, pos2.y), min(pos1.z, pos2.z))
        val posE = BlockPos(max(pos1.x, pos2.x), max(pos1.y, pos2.y), max(pos1.z, pos2.z))

        try {
            val mesh = WorldMesh.create {
                world = client.world!!
                origin = posO
                end = posE
                camera = Vec3d(1000.0, 0.0, 0.0)
                entities = {
                    client.world!!.getOtherEntities(null, Box(posO, posE.add(1, 1, 1)))
                }
            }.apply { scheduleRebuild() }
            val renderable = AreaRenderable(mesh, DefaultRenderProperties(500.0, 45.0, 30.0))
            RenderSystem.recordRenderCall { client.setScreen(RenderScreen(renderable)) }
        } catch (e: Throwable) {
            // Mfw logging
            e.printStackTrace()
        }
        return 0
    }
}