package dev.kikugie.testmod.command;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.kikugie.testmod.RenderScreen;
import dev.kikugie.testmod.world.DummyWorld;
import dev.kikugie.testmod.world.StructureLoader;
import dev.kikugie.worldrenderer.mesh.WorldMesh;
import dev.kikugie.worldrenderer.mesh.WorldMeshBuilder;
import dev.kikugie.worldrenderer.property.DefaultRenderProperties;
import dev.kikugie.worldrenderer.render.AreaRenderable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;


public class RenderCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        RequiredArgumentBuilder<ServerCommandSource, Identifier> arg = RequiredArgumentBuilder.argument("id", IdentifierArgumentType.identifier());
        arg.executes(RenderCommand::render);
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("render").then(arg));
    }

    private static int render(CommandContext<ServerCommandSource> context) {
        ServerWorld world = context.getSource().getWorld();
        try {
            Optional<DummyWorld> optional = StructureLoader.createWorld(world, context.getArgument("id", Identifier.class));
            if (optional.isPresent()) {
                DummyWorld dummy = optional.get();
                WorldMeshBuilder builder = WorldMesh.builder(dummy, dummy.getMin(), dummy.getMax());
                builder.setEntitySupplier(dummy);
                WorldMesh mesh = builder.build();
                mesh.scheduleRebuild();

                AreaRenderable renderable = new AreaRenderable(mesh, new DefaultRenderProperties(500, 45, 30));
                MinecraftClient client = MinecraftClient.getInstance();
                RenderSystem.recordRenderCall(() -> client.setScreen(new RenderScreen(Text.of("Render"), renderable)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}