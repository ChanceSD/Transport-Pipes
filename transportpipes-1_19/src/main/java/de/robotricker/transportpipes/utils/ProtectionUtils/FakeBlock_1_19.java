package de.robotricker.transportpipes.utils.ProtectionUtils;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FakeBlock_1_19 extends FakeBlock implements Block {

    public FakeBlock_1_19(World world, Location location, Material material) {
        this.world = world;
        this.location = location;
        this.material = material;
        this.blockData = material.createBlockData();
    }

    @Override
    public byte getData() {
        return 0;
    }

    @NotNull
    @Override
    public BlockData getBlockData() {
        return blockData;
    }

    @NotNull
    @Override
    public Block getRelative(int modX, int modY, int modZ) {
        return location.add(modX, modY, modZ).getBlock();
    }

    @NotNull
    @Override
    public Block getRelative(@NotNull BlockFace face) {
        return getRelative(face, 1);
    }

    @NotNull
    @Override
    public Block getRelative(@NotNull BlockFace face, int distance) {
        return location.getBlock().getRelative(face, distance);
    }

    @NotNull
    @Override
    public Material getType() {
        return material;
    }

    @Override
    public byte getLightLevel() {
        return 0;
    }

    @Override
    public byte getLightFromSky() {
        return 0;
    }

    @Override
    public byte getLightFromBlocks() {
        return 0;
    }

    @NotNull
    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public int getX() {
        return location.getBlockX();
    }

    @Override
    public int getY() {
        return location.getBlockY();
    }

    @Override
    public int getZ() {
        return location.getBlockZ();
    }

    @NotNull
    @Override
    public Location getLocation() {
        return location;
    }

    @Nullable
    @Override
    public Location getLocation(@Nullable Location loc) {
        if (loc == null) {
            return null;
        }
        loc.setDirection(location.getDirection());
        loc.setPitch(location.getPitch());
        loc.setYaw(location.getYaw());
        loc.setWorld(location.getWorld());
        loc.setX(location.getX());
        loc.setY(location.getY());
        loc.setZ(location.getZ());
        return loc;
    }

    @NotNull
    @Override
    public Chunk getChunk() {
        return location.getChunk();
    }

    @Override
    public void setBlockData(@NotNull BlockData data) {
        this.blockData = data;
    }

    @Override
    public void setBlockData(@NotNull BlockData data, boolean applyPhysics) {
        this.blockData = data;
    }

    @Override
    public void setType(@NotNull Material type) {
        this.material = type;
    }

    @Override
    public void setType(@NotNull Material type, boolean applyPhysics) {
        this.material = type;
    }

    @Nullable
    @Override
    public BlockFace getFace(@NotNull Block block) {
        BlockFace face = block.getFace(location.getBlock());
        return face == null ? null : face.getOppositeFace();
    }

    @NotNull
    @Override
    public BlockState getState() {
        return location.getBlock().getState();
    }

    @NotNull
    @Override
    public Biome getBiome() {
        return location.getBlock().getBiome();
    }

    @Override
    public void setBiome(@NotNull Biome bio) {
        location.getBlock().setBiome(bio);
    }

    @Override
    public boolean isBlockPowered() {
        return location.getBlock().isBlockPowered();
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        return location.getBlock().isBlockIndirectlyPowered();
    }

    @Override
    public boolean isBlockFacePowered(@NotNull BlockFace face) {
        return location.getBlock().isBlockFacePowered(face);
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(@NotNull BlockFace face) {
        return location.getBlock().isBlockFaceIndirectlyPowered(face);
    }

    @Override
    public int getBlockPower(@NotNull BlockFace face) {
        return location.getBlock().getBlockPower(face);
    }

    @Override
    public int getBlockPower() {
        return location.getBlock().getBlockPower();
    }

    @Override
    public boolean isEmpty() {
        return material.isAir();
    }

    @Override
    public boolean isLiquid() {
        return material == Material.WATER || material == Material.LAVA;
    }

    @Override
    public double getTemperature() {
        return location.getBlock().getTemperature();
    }

    @Override
    public double getHumidity() {
        return location.getBlock().getHumidity();
    }

    @NotNull
    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return location.getBlock().getPistonMoveReaction();
    }

    @Override
    public boolean breakNaturally() {
        return false;
    }

    @Override
    public boolean breakNaturally(@Nullable ItemStack tool) {
        return false;
    }

    @Override
    public boolean applyBoneMeal(@NotNull BlockFace face) {
        return false;
    }

    @NotNull
    @Override
    public Collection<ItemStack> getDrops() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<ItemStack> getDrops(@Nullable ItemStack tool) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<ItemStack> getDrops(@NotNull ItemStack tool, @Nullable Entity entity) {
        return Collections.emptyList();
    }

    @Override
    public boolean isPreferredTool(@NotNull ItemStack tool) {
        return false;
    }

    @Override
    public float getBreakSpeed(@NotNull Player player) {
        return 0;
    }

    @Override
    public boolean isPassable() {
        return location.getBlock().isPassable();
    }

    @Nullable
    @Override
    public RayTraceResult rayTrace(@NotNull Location start, @NotNull Vector direction, double maxDistance, @NotNull FluidCollisionMode fluidCollisionMode) {
        return null;
    }

    @NotNull
    @Override
    public BoundingBox getBoundingBox() {
        return location.getBlock().getBoundingBox();
    }

    @NotNull
    @Override
    public VoxelShape getCollisionShape() {
        return location.getBlock().getCollisionShape();
    }

    @Override
    public boolean canPlace(@NotNull BlockData blockData) {
        return location.getBlock().canPlace(blockData);
    }

    @Override
    public void setMetadata(@NotNull String metadataKey, @NotNull MetadataValue newMetadataValue) {

    }

    @NotNull
    @Override
    public List<MetadataValue> getMetadata(@NotNull String metadataKey) {
        return Collections.emptyList();
    }

    @Override
    public boolean hasMetadata(@NotNull String metadataKey) {
        return false;
    }

    @Override
    public void removeMetadata(@NotNull String metadataKey, @NotNull Plugin owningPlugin) {

    }
}