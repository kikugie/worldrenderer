package dev.kikugie.testmod.world;

import dev.kikugie.worldrenderer.util.EntitiesSupplier;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.QueryableTickScheduler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class DummyWorld implements BlockRenderView, ModifiableWorld, EntitiesSupplier, ServerWorldAccess {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final LightingProvider lightingProvider = new LightingProvider(new DummyChunkProvider(this), true, true);
    private final Biome biome = Objects.requireNonNull(this.client.world).getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.PLAINS).value();

    /* Very bad, for testing only */
    private final Long2ObjectMap<BlockState> stateMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<BlockEntity> blockEntityMap = new Long2ObjectOpenHashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private final BlockPos.Mutable min = new BlockPos.Mutable();
    private final BlockPos.Mutable max = new BlockPos.Mutable();

    private void expand(BlockPos pos) {
        // Expanding min
        min.setX(Math.min(min.getX(), pos.getX()));
        min.setY(Math.min(min.getY(), pos.getY()));
        min.setZ(Math.min(min.getZ(), pos.getZ()));

        // Expanding max
        max.setX(Math.max(max.getX(), pos.getX()));
        max.setY(Math.max(max.getY(), pos.getY()));
        max.setZ(Math.max(max.getZ(), pos.getZ()));
    }

    public BlockPos getMin() {
        return min.toImmutable();
    }

    public BlockPos getMax() {
        return max.toImmutable();
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        return shaded ? switch (direction) {
            case UP -> 1.0F;
            case DOWN -> 0.9F;
            case NORTH, SOUTH -> 0.8F;
            case EAST, WEST -> 0.6F;
        } : 1.0F;
    }

    @Override
    public LightingProvider getLightingProvider() {
        return this.lightingProvider;
    }

    @Nullable
    @Override
    public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
        return null;
    }

    @Override
    public int getTopY(Heightmap.Type heightmap, int x, int z) {
        return 0;
    }

    @Override
    public int getAmbientDarkness() {
        return 0;
    }

    @Override
    public BiomeAccess getBiomeAccess() {
        return null;
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver) {
        return colorResolver.getColor(this.biome, pos.getX(), pos.getZ());
    }

    @Override
    public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
        return null;
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public DimensionType getDimension() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.blockEntityMap.get(pos.asLong());
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.stateMap.getOrDefault(pos.asLong(), Blocks.AIR.getDefaultState());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return 320;
    }

    @Override
    public DynamicRegistryManager getRegistryManager() {
        return null;
    }

    @Override
    public FeatureSet getEnabledFeatures() {
        return null;
    }

    @Override
    public int getBottomY() {
        return -64;
    }

    @Override
    public List<Entity> getEntities() {
        return this.entities;
    }

    @Override
    public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        expand(pos);
        return this.stateMap.put(pos.asLong(), state) != null;
    }

    @Override
    public boolean removeBlock(BlockPos pos, boolean move) {
        return this.stateMap.remove(pos.asLong()) != null;
    }

    @Override
    public boolean breakBlock(BlockPos pos, boolean drop, @Nullable Entity breakingEntity, int maxUpdateDepth) {
        return removeBlock(pos, false);
    }

    public boolean setBlockEntity(BlockPos pos, BlockEntity entity) {
        return this.blockEntityMap.put(pos.asLong(), entity) != null;
    }

    @Override
    public ServerWorld toServerWorld() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTickOrder() {
        return 0;
    }

    @Override
    public QueryableTickScheduler<Block> getBlockTickScheduler() {
        return Objects.requireNonNull(client.world).getBlockTickScheduler();
    }

    @Override
    public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
        return Objects.requireNonNull(client.world).getFluidTickScheduler();
    }

    @Override
    public WorldProperties getLevelProperties() {
        return Objects.requireNonNull(client.world).getLevelProperties();
    }

    @Override
    public LocalDifficulty getLocalDifficulty(BlockPos pos) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public MinecraftServer getServer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChunkManager getChunkManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Random getRandom() {
        return Objects.requireNonNull(client.world).getRandom();
    }

    @Override
    public void playSound(@Nullable PlayerEntity except, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {

    }

    @Override
    public void addParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {

    }

    @Override
    public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {

    }

    @Override
    public void emitGameEvent(GameEvent event, Vec3d emitterPos, GameEvent.Emitter emitter) {

    }

    @Override
    public WorldBorder getWorldBorder() {
        return null;
    }

    @Override
    public List<Entity> getOtherEntities(@Nullable Entity except, Box box, Predicate<? super Entity> predicate) {
        return Collections.emptyList();
    }

    @Override
    public <T extends Entity> List<T> getEntitiesByType(TypeFilter<Entity, T> filter, Box box, Predicate<? super T> predicate) {
        return Collections.emptyList();
    }

    @Override
    public List<? extends PlayerEntity> getPlayers() {
        return Collections.emptyList();
    }

    @Override
    public boolean testBlockState(BlockPos pos, Predicate<BlockState> state) {
        return false;
    }

    @Override
    public boolean testFluidState(BlockPos pos, Predicate<FluidState> state) {
        return false;
    }
}