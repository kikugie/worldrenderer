package dev.kikugie.worldrenderer.util;

import net.minecraft.entity.Entity;

import java.util.List;

@FunctionalInterface
public interface EntitiesSupplier {
    List<Entity> getEntities();
}