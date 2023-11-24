package dev.kikugie.testmod.world;

import dev.kikugie.worldrenderer.util.EntitiesSupplier;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.map.MapState;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LightType;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.ColorResolver;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.tick.QueryableTickScheduler;
import net.minecraft.world.tick.TickPriority;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("deprecation")
public class DummyWorld extends World implements EntitiesSupplier, ServerWorldAccess {
    public static final RegistryKey<World> REGISTRY_KEY = RegistryKey.of(RegistryKeys.WORLD, new Identifier("worldrenderer", "testworld"));
    public static final ClientWorld.Properties PROPERTIES = new ClientWorld.Properties(Difficulty.PEACEFUL, false, true);
    private final MinecraftClient client;
    private final LightingProvider lightingProvider;
    private final RegistryEntry<Biome> biome;

    /* Very bad, for testing only */
    private final Long2ObjectMap<BlockState> stateMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<BlockEntity> blockEntityMap = new Long2ObjectOpenHashMap<>();
    private final List<Entity> entities = new ArrayList<>();
    private final BlockPos.Mutable min = new BlockPos.Mutable();
    private final BlockPos.Mutable max = new BlockPos.Mutable();

    private ServerWorld omegaStructureBlockPlacementHax = null;

    protected DummyWorld(MinecraftClient client) {
        super(PROPERTIES,
                REGISTRY_KEY,
                Objects.requireNonNull(client.getNetworkHandler()).getRegistryManager(),
                Objects.requireNonNull(client.world).getDimensionEntry(),
                client::getProfiler,
                true,
                false,
                0L,
                0);
        this.client = client;
        this.lightingProvider = new LightingProvider(new DummyChunkProvider(this), true, true);
        this.biome = Objects.requireNonNull(client.world).getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.PLAINS);
    }

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
    public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        long posl = pos.asLong();
        expand(pos);
        stateMap.put(posl, state);

        if (state.hasBlockEntity()) {
            BlockEntity blockEntity = ((BlockEntityProvider) state.getBlock()).createBlockEntity(pos, state);
            if (blockEntity == null) return true;

            blockEntity.setWorld(this);
            blockEntity.setCachedState(state);
            blockEntityMap.put(posl, blockEntity);
        }

        return true;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return stateMap.getOrDefault(pos.asLong(), Blocks.AIR.getDefaultState());
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return blockEntityMap.get(pos.asLong());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public boolean spawnEntity(Entity entity) {
        entities.add(entity);
        return true;
    }

    @Override
    public List<Entity> getEntities() {
        return entities;
    }

    @Override
    public int getLightLevel(LightType type, BlockPos pos) {
        return 15;
    }

    @Override
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public void markDirty(BlockPos pos) {

    }

    public void setServerWorld(ServerWorld world) {
        this.omegaStructureBlockPlacementHax = world;
    }

    @Override
    public ServerWorld toServerWorld() {
        if (omegaStructureBlockPlacementHax != null) return omegaStructureBlockPlacementHax;
        throw new IllegalStateException();
    }

    @Override
    public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {

    }

    @Override
    public void playSound(@Nullable PlayerEntity except, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public void playSoundFromEntity(@Nullable PlayerEntity except, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {

    }

    @Override
    public String asString() {
        return "null";
    }

    @Nullable
    @Override
    public Entity getEntityById(int id) {
        return null;
    }

    @Nullable
    @Override
    public MapState getMapState(String id) {
        return null;
    }

    @Override
    public void putMapState(String id, MapState state) {

    }

    @Override
    public int getNextMapId() {
        return 0;
    }

    @Override
    public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {

    }

    @Override
    public Scoreboard getScoreboard() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RecipeManager getRecipeManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected EntityLookup<Entity> getEntityLookup() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryableTickScheduler<Block> getBlockTickScheduler() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryableTickScheduler<Fluid> getFluidTickScheduler() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChunkManager getChunkManager() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void syncWorldEvent(@Nullable PlayerEntity player, int eventId, BlockPos pos, int data) {

    }

    @Override
    public void emitGameEvent(GameEvent event, Vec3d emitterPos, GameEvent.Emitter emitter) {

    }

    @Override
    public List<? extends PlayerEntity> getPlayers() {
        return Collections.emptyList();
    }

    @Override
    public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {
        return biome;
    }

    @Override
    public FeatureSet getEnabledFeatures() {
        return FeatureFlags.DEFAULT_ENABLED_FEATURES;
    }

    @Override
    public void scheduleBlockTick(BlockPos pos, Block block, int delay) {

    }

    @Override
    public void scheduleBlockTick(BlockPos pos, Block block, int delay, TickPriority priority) {

    }

    @Override
    public void scheduleFluidTick(BlockPos pos, Fluid fluid, int delay) {

    }

    @Override
    public void scheduleFluidTick(BlockPos pos, Fluid fluid, int delay, TickPriority priority) {

    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver) {
        return colorResolver.getColor(biome.value(), pos.getX(), pos.getZ());
    }
}