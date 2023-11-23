package dev.kikugie.testmod.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import java.util.Optional;

public class StructureLoader {
    public static Optional<DummyWorld> createWorld(ServerWorld world, Identifier name) {
        Optional<StructureTemplate> optional = load(world, name);
        if (optional.isPresent()) {
            DummyWorld dummy = new DummyWorld(MinecraftClient.getInstance());
            dummy.setServerWorld(world);
            place(dummy, optional.get());
            return Optional.of(dummy);
        } else return Optional.empty();
    }

    public static Optional<StructureTemplate> load(ServerWorld world, Identifier name) {
        try {
            return world.getStructureTemplateManager().getTemplate(name);
        } catch (InvalidIdentifierException var6) {
            return Optional.empty();
        }
    }

    public static void place(DummyWorld world, StructureTemplate structure) {
        StructurePlacementData data = new StructurePlacementData();
        BlockBox box = structure.calculateBoundingBox(data, BlockPos.ORIGIN);
        data.setBoundingBox(box);

        structure.place(world, BlockPos.ORIGIN, BlockPos.ORIGIN, data, Random.create(), 2);
    }
}