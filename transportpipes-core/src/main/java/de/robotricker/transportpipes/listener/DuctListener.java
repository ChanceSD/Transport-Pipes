package de.robotricker.transportpipes.listener;

import de.robotricker.transportpipes.PlayerSettingsService;
import de.robotricker.transportpipes.ThreadService;
import de.robotricker.transportpipes.TransportPipes;
import de.robotricker.transportpipes.api.DuctBreakEvent;
import de.robotricker.transportpipes.api.DuctPlaceEvent;
import de.robotricker.transportpipes.config.GeneralConf;
import de.robotricker.transportpipes.config.LangConf;
import de.robotricker.transportpipes.duct.Duct;
import de.robotricker.transportpipes.duct.DuctRegister;
import de.robotricker.transportpipes.duct.manager.GlobalDuctManager;
import de.robotricker.transportpipes.duct.types.DuctType;
import de.robotricker.transportpipes.items.ItemService;
import de.robotricker.transportpipes.location.BlockLocation;
import de.robotricker.transportpipes.location.TPDirection;
import de.robotricker.transportpipes.utils.HitboxUtils;
import de.robotricker.transportpipes.utils.ProtectionUtils.ProtectionUtils;
import de.robotricker.transportpipes.utils.WorldUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import java.util.*;

public class DuctListener implements Listener {

    private final List<Material> interactables = new ArrayList<>();

    //makes sure that "callInteraction" is called with the mainHand and with the offHand every single time
    private final Map<Player, Interaction> interactions = new HashMap<>();
    private final Set<UUID> noClick = new HashSet<>();

    private final ItemService itemService;
    private final DuctRegister ductRegister;
    private final GlobalDuctManager globalDuctManager;
    private final TPContainerListener tpContainerListener;
    private final GeneralConf generalConf;
    private final TransportPipes transportPipes;
    private final ThreadService threadService;
    private final PlayerSettingsService playerSettingsService;
    private final ProtectionUtils protectionUtils;

    @Inject
    public DuctListener(ItemService itemService, JavaPlugin plugin, DuctRegister ductRegister, GlobalDuctManager globalDuctManager, TPContainerListener tpContainerListener, GeneralConf generalConf, TransportPipes transportPipes, ThreadService threadService, PlayerSettingsService playerSettingsService, ProtectionUtils protectionUtils) {
        this.itemService = itemService;
        this.ductRegister = ductRegister;
        this.globalDuctManager = globalDuctManager;
        this.tpContainerListener = tpContainerListener;
        this.generalConf = generalConf;
        this.transportPipes = transportPipes;
        this.threadService = threadService;
        this.playerSettingsService = playerSettingsService;
        this.protectionUtils = protectionUtils;

        for (Material m : Material.values()) {
            if (m.isInteractable()) {
                if (m != Material.PUMPKIN && m != Material.REDSTONE_ORE && m != Material.MOVING_PISTON && m != Material.TNT) interactables.add(m);
            }
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateInteractSet, 0L, 1L);
    }

    private void updateInteractSet() {
        Iterator<Player> events = interactions.keySet().iterator();
        while (events.hasNext()) {
            Player p = events.next();
            if (interactions.get(p) != null)
                callInteraction(interactions.get(p));
            events.remove();
        }
    }
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        noClick.add(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                noClick.remove(uuid);
            }
        }.runTaskLater(transportPipes, 2L);
    }
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityClick(PlayerInteractEntityEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        noClick.add(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                noClick.remove(uuid);
            }
        }.runTaskLater(transportPipes, 2L);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block clickedBlock = e.getClickedBlock();
        UUID uuid = p.getUniqueId();

        if (e.getAction() == Action.PHYSICAL) {
            return;
        }

        if (e.getAction() == Action.LEFT_CLICK_AIR) {
            if (noClick.contains(uuid)) {
                return;
            }
        }
        
        noClick.add(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                noClick.remove(uuid);
            }
        }.runTaskLater(transportPipes, 3L);
        
        if (e.getHand() == EquipmentSlot.HAND) {
            Interaction offHandInteraction = new Interaction(p, EquipmentSlot.OFF_HAND, p.getInventory().getItemInOffHand(), clickedBlock, e.getBlockFace(), e.getAction());
            interactions.put(p, offHandInteraction);

            Interaction mainHandInteraction = new Interaction(p, EquipmentSlot.HAND, p.getInventory().getItemInMainHand(), clickedBlock, e.getBlockFace(), e.getAction());
            callInteraction(mainHandInteraction);
            if (mainHandInteraction.cancel) e.setCancelled(true);
            if (mainHandInteraction.denyBlockUse) e.setUseInteractedBlock(Event.Result.DENY);
            if (mainHandInteraction.successful) {
                interactions.put(p, null);
            }
        }
        else if (e.getHand() == EquipmentSlot.OFF_HAND) {
            if (interactions.containsKey(p) && interactions.get(p) == null) {
                e.setCancelled(true);
                return;
            }
            if (interactions.containsKey(p)) {
                interactions.remove(p);
            }
            else {
                Interaction mainHandInteraction = new Interaction(p, EquipmentSlot.HAND, p.getInventory().getItemInMainHand(), clickedBlock, e.getBlockFace(), e.getAction());
                callInteraction(mainHandInteraction);
                if (mainHandInteraction.successful) {
                    return;
                }
            }
            Interaction offHandInteraction = new Interaction(p, EquipmentSlot.OFF_HAND, p.getInventory().getItemInOffHand(), clickedBlock, e.getBlockFace(), e.getAction());
            callInteraction(offHandInteraction);
            if (offHandInteraction.cancel) e.setCancelled(true);
            if (offHandInteraction.denyBlockUse) e.setUseInteractedBlock(Event.Result.DENY);
        }

    }

    private void callInteraction(Interaction interaction) {
        if (interaction.action == Action.RIGHT_CLICK_AIR || interaction.action == Action.RIGHT_CLICK_BLOCK) {
            if (interaction.item != null) {
                // clicedDuct is the duct that was clicked, if a duct was clicked, otherwise null
                Duct clickedDuct = HitboxUtils.getDuctLookingTo(globalDuctManager, interaction.p, interaction.clickedBlock);
                // itemDuctType is the duct type in the player's hand when they click, if they have one, otherwise null
                DuctType itemDuctType = itemService.readDuctTags(interaction.item, ductRegister);
                // manualPlaceable is true if the item in the player's hand can be placed, ie the item is a block or a duct
                boolean manualPlaceable = itemDuctType != null || interaction.item.getType().isSolid();
                
                // ********************** CLICKING AIR WITHOUT A WRENCH ****************************
                if (interaction.action == Action.RIGHT_CLICK_AIR && clickedDuct == null && !itemService.isWrench(interaction.item)) {
                	return;
                }

                // ********************** CLICKING BLOCK WITH NO PIPE OR PIPE ITEM WITHOUT A WRENCH ****************************
                if (clickedDuct == null && itemDuctType == null && interaction.clickedBlock != null && !itemService.isWrench(interaction.item) &&
                        globalDuctManager.getDuctAtLoc(interaction.clickedBlock.getRelative(interaction.blockFace).getLocation()) == null) {
                    return;
                }

                // ********************** WRENCH SNEAK DUCT CLICK ****************************
                if (clickedDuct != null && itemService.isWrench(interaction.item) && interaction.p.isSneaking()) {
                    // Wrench sneak click
                    Block ductBlock = clickedDuct.getBlockLoc().toBlock(interaction.p.getWorld());
                    if (protectionUtils.canBreak(interaction.p, ductBlock)) {
                        Block relativeBlock = HitboxUtils.getRelativeBlockOfDuct(globalDuctManager, interaction.p, ductBlock);
                        TPDirection clickedDir = TPDirection.fromBlockFace(ductBlock.getFace(Objects.requireNonNull(relativeBlock)));
                        Duct relativeDuct = clickedDuct.getDuctConnections().get(clickedDir);
                        if (clickedDuct.getBlockedConnections().contains(clickedDir)) {
                            clickedDuct.getBlockedConnections().remove(clickedDir);
                            LangConf.Key.CONNECTION_UNBLOCKED.sendMessage(interaction.p, Objects.requireNonNull(clickedDir).toString());
                        }
                        else {
                            clickedDuct.getBlockedConnections().add(clickedDir);
                            LangConf.Key.CONNECTION_BLOCKED.sendMessage(interaction.p, Objects.requireNonNull(clickedDir).toString());
                        }
                        globalDuctManager.updateDuctConnections(clickedDuct);
                        globalDuctManager.updateDuctInRenderSystems(clickedDuct, true);
                        relativeDuct = relativeDuct != null ? relativeDuct : clickedDuct.getDuctConnections().get(clickedDir);
                        if (relativeDuct != null) {
                            globalDuctManager.updateDuctConnections(relativeDuct);
                            globalDuctManager.updateDuctInRenderSystems(relativeDuct, true);
                        }
                    }
                    
                    interaction.cancel = true;
                    interaction.successful = true;
                    return;
                }

                // ********************** WRENCH DUCT CLICK ****************************
                if (clickedDuct != null && !manualPlaceable &&
                        (itemService.isWrench(interaction.item) || (!generalConf.getWrenchRequired() && !canBeUsedToObfuscate(interaction.item.getType())))) {
                    //wrench click
                    if (protectionUtils.canBreak(interaction.p, clickedDuct.getBlockLoc().toBlock(interaction.p.getWorld()))) {
                        clickedDuct.notifyClick(interaction.p, interaction.p.isSneaking());
                    }

                    interaction.cancel = true;
                    interaction.successful = true;
                    return;
                }

                // ********************** SNEAK WRENCH NON DUCT CLICK ****************************
                if (clickedDuct == null && itemService.isWrench(interaction.item) && interaction.p.isSneaking()) {
                    //wrench click

                    WorldUtils.startShowHiddenDuctsProcess(interaction.p, globalDuctManager, threadService, transportPipes, generalConf, playerSettingsService);

                    interaction.cancel = true;
                    interaction.successful = true;
                    return;
                }

                // ********************** DUCT OBFUSCATION ****************************
                if (!interaction.p.isSneaking() && canBeUsedToObfuscate(interaction.item.getType())) {
                    // block can be used to obfuscate and player is not sneaking
                    // this block will be used to obfuscate the duct
                    Duct relativeDuct = null;
                    Block ductBlock;
                    if (clickedDuct != null) {
                        ductBlock = clickedDuct.getBlockLoc().toBlock(interaction.p.getWorld());
                    }
                    else if (interaction.clickedBlock != null) {
                        ductBlock = interaction.clickedBlock.getRelative(interaction.blockFace);
                        relativeDuct = globalDuctManager.getDuctAtLoc(interaction.clickedBlock.getRelative(interaction.blockFace).getLocation());
                    }
                    else {
                        return;
                    }
                    if (protectionUtils.canBuild(interaction.p, ductBlock, interaction.item, interaction.hand)) {

                        BlockData oldBlockData = ductBlock.getBlockData().clone();
                        BlockData blockData = interaction.item.getType().createBlockData();
                        setDirectionalBlockFace(ductBlock.getLocation(), blockData, interaction.p);
                        ductBlock.setBlockData(blockData, true);

                        BlockPlaceEvent event = new BlockPlaceEvent(ductBlock, ductBlock.getState(), ductBlock.getRelative(BlockFace.DOWN), interaction.item, interaction.p, true, interaction.hand);
                        Bukkit.getPluginManager().callEvent(event);

                        if (!event.isCancelled()) {
                            if (clickedDuct != null) {
                                clickedDuct.obfuscatedWith(blockData);
                                decreaseHandItem(interaction.p, interaction.hand);
                            }
                            else if (relativeDuct != null) {
                                relativeDuct.obfuscatedWith(blockData);
                                decreaseHandItem(interaction.p, interaction.hand);
                            }
                            else {
                                ductBlock.setBlockData(oldBlockData, true);
                            }
                        }
                        else {
                            ductBlock.setBlockData(oldBlockData, true);
                        }
                    }

                    interaction.cancel = true;
                    interaction.successful = true;
                    return;
                }

                // ********************** PREPARATIONS FOR DUCT / BLOCK PLACE ****************************
                Block relativeBlock = null;
                // If duct clicked, get block relative to clicked duct
                if (clickedDuct != null) {
                    relativeBlock = HitboxUtils.getRelativeBlockOfDuct(globalDuctManager, interaction.p, clickedDuct.getBlockLoc().toBlock(interaction.p.getWorld()));
                }
                // Otherwise, if block clicked, get block relative to clicked block
                else if (interaction.clickedBlock != null) {
                    relativeBlock = interaction.clickedBlock.getRelative(interaction.blockFace);
                }
                // If hand item is not a duct, duct was not clicked, and there's a relative block and the relative block is not a duct
                if (itemDuctType == null && clickedDuct == null && (relativeBlock != null && globalDuctManager.getDuctAtLoc(relativeBlock.getLocation()) == null)) {
                	return;
                }
                // If there's a relative block, and it's either solid or a duct
                if (relativeBlock != null && (relativeBlock.getType().isSolid() || globalDuctManager.getDuctAtLoc(relativeBlock.getLocation()) != null)) {
                    relativeBlock = null;
                }
                // If duct was not clicked, a block was clicked, the clicked block is interactable, and the player is not sneaking
                if (clickedDuct == null && interaction.clickedBlock != null && interactables.contains(interaction.clickedBlock.getType()) && !interaction.p.isSneaking()) {
                	// Stairs are considered interactable for some weird reason so ignore those
                    if (!(interaction.clickedBlock.getBlockData() instanceof Stairs)) {
                    	return;
                    }
                }
                // Otherwise, if there is no relative block or a duct was clicked and hand item is not a duct or solid block
                else if (relativeBlock == null || (clickedDuct != null && !manualPlaceable)) {
                    // Don't prevent players from eating or bone mealing
                    if (!interaction.item.getType().isEdible() && interaction.item.getType() != Material.BONE_MEAL) {
                        interaction.denyBlockUse = true;
                    }
                    return;
                }

                // ********************** DUCT AND BLOCK PLACE ****************************
                if (manualPlaceable) {
                    // DUCT PLACEMENT
                    if (itemDuctType != null) {
                        // At this point, relativeBlock is only null if we can't place a block there
                        if (relativeBlock != null && protectionUtils.canBuild(interaction.p, relativeBlock, interaction.item, interaction.hand)) {
                            boolean lwcAllowed = true;
                            for (TPDirection dir : TPDirection.values()) {
                                if (WorldUtils.lwcProtection(relativeBlock.getRelative(dir.getBlockFace()))) {
                                    lwcAllowed = false;
                                }
                            }
                            if (lwcAllowed) {
                                Duct duct = globalDuctManager.createDuctObject(itemDuctType, new BlockLocation(relativeBlock.getLocation()), relativeBlock.getWorld(), relativeBlock.getChunk());
                                globalDuctManager.registerDuct(duct);
                                globalDuctManager.updateDuctConnections(duct);
                                globalDuctManager.registerDuctInRenderSystems(duct, true);
                                globalDuctManager.updateNeighborDuctsConnections(duct);
                                globalDuctManager.updateNeighborDuctsInRenderSystems(duct, true);

                                decreaseHandItem(interaction.p, interaction.hand);
                                
                                DuctPlaceEvent event = new DuctPlaceEvent(interaction.p, duct.getBlockLoc());
                                Bukkit.getPluginManager().callEvent(event);
                            } else {
                                LangConf.Key.PROTECTED_BLOCK.sendMessage(interaction.p);
                            }
                        }

                        interaction.cancel = true;
                        interaction.successful = true;
                    }
                    // BLOCK PLACEMENT NEXT TO DUCT
                    else if (clickedDuct != null) {
                        BlockData blockData = interaction.item.getType().createBlockData();
                        setDirectionalBlockFace(relativeBlock.getLocation(), blockData, interaction.p);
                        Block fakeBlock = transportPipes.getFakeBlock().getBlock(relativeBlock.getWorld(), relativeBlock.getLocation(), interaction.item.getType());
                        fakeBlock.setBlockData(blockData, false);

                        if (protectionUtils.canBuild(interaction.p, fakeBlock, interaction.item, interaction.hand)) {
                            BlockData oldBlockData = relativeBlock.getBlockData().clone();
                            relativeBlock.setBlockData(blockData, true);

                            BlockPlaceEvent event = new BlockPlaceEvent(relativeBlock, relativeBlock.getState(), clickedDuct.getBlockLoc().toBlock(relativeBlock.getWorld()), interaction.item, interaction.p, true, interaction.hand);
                            Bukkit.getPluginManager().callEvent(event);

                            if (!event.isCancelled()) {
                                Material blockType = relativeBlock.getType();
                                // If the block to place is not solid, slab, stair, impermeable, glowstone, and container
                                if (!blockType.isOccluding() && !Tag.SLABS.isTagged(blockType) && !Tag.STAIRS.isTagged(blockType)
                                        && !Tag.IMPERMEABLE.isTagged(blockType) && blockType != Material.GLOWSTONE &&
                                        !WorldUtils.isContainerBlock(relativeBlock)) {
                                    relativeBlock.setBlockData(oldBlockData, true);
                                }
                                else {
                                    // create TransportPipesContainer from placed block if it is such
                                    if (WorldUtils.isContainerBlock(interaction.item)) {
                                        tpContainerListener.updateContainerBlock(relativeBlock, true, true);
                                    }

                                    decreaseHandItem(interaction.p, interaction.hand);
                                }
                            }
                            else {
                                relativeBlock.setBlockData(oldBlockData, true);
                            }
                        }

                        interaction.cancel = true;
                        interaction.successful = true;
                    }
                }

            }
        } else if (interaction.action == Action.LEFT_CLICK_AIR || interaction.action == Action.LEFT_CLICK_BLOCK) {
            Duct clickedDuct = HitboxUtils.getDuctLookingTo(globalDuctManager, interaction.p, interaction.clickedBlock);
            // duct destruction
            if (clickedDuct != null) {
                BlockLocation clickedDuctLocation = clickedDuct.getBlockLoc();
                if (protectionUtils.canBreak(interaction.p, clickedDuctLocation.toBlock(interaction.p.getWorld()))) {
                    globalDuctManager.unregisterDuct(clickedDuct);
                    globalDuctManager.unregisterDuctInRenderSystem(clickedDuct, true);
                    globalDuctManager.updateNeighborDuctsConnections(clickedDuct);
                    globalDuctManager.updateNeighborDuctsInRenderSystems(clickedDuct, true);
                    globalDuctManager.playDuctDestroyActions(clickedDuct, interaction.p);
                    
                    DuctBreakEvent event = new DuctBreakEvent(interaction.p, clickedDuctLocation);
                    Bukkit.getPluginManager().callEvent(event);
                }

                interaction.cancel = true;
                interaction.successful = true;
            }
        }
    }

    private boolean canBeUsedToObfuscate(Material type) {
        return (type.isOccluding() || Tag.IMPERMEABLE.isTagged(type) || type == Material.GLOWSTONE) &&
                !type.isInteractable() && !type.hasGravity();
    }

    private void setDirectionalBlockFace(Location b, BlockData bd, Player p) {
        if (bd instanceof Directional) {
            Vector dir = new Vector(b.getX() + 0.5d, b.getY() + 0.5d, b.getZ() + 0.5d);
            dir.subtract(p.getEyeLocation().toVector());
            double absX = Math.abs(dir.getX());
            double absY = Math.abs(dir.getY());
            double absZ = Math.abs(dir.getZ());
            if (((Directional) bd).getFaces().contains(BlockFace.UP) && ((Directional) bd).getFaces().contains(BlockFace.DOWN)) {
                if (absX >= absY && absX >= absZ) {
                    ((Directional) bd).setFacing(dir.getX() > 0 ? BlockFace.WEST : BlockFace.EAST);
                } else if (absY >= absX && absY >= absZ) {
                    ((Directional) bd).setFacing(dir.getY() > 0 ? BlockFace.DOWN : BlockFace.UP);
                } else {
                    ((Directional) bd).setFacing(dir.getZ() > 0 ? BlockFace.NORTH : BlockFace.SOUTH);
                }
            } else {
                if (absX >= absZ) {
                    ((Directional) bd).setFacing(dir.getX() > 0 ? BlockFace.WEST : BlockFace.EAST);
                } else {
                    ((Directional) bd).setFacing(dir.getZ() > 0 ? BlockFace.NORTH : BlockFace.SOUTH);
                }
            }
        }
    }

    private void decreaseHandItem(Player p, EquipmentSlot hand) {
        if (p.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        ItemStack item = hand == EquipmentSlot.HAND ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand();
        if (item.getAmount() <= 1) {
            item = null;
        } else {
            item.setAmount(item.getAmount() - 1);
        }
        if (hand == EquipmentSlot.HAND) p.getInventory().setItemInMainHand(item);
        else p.getInventory().setItemInOffHand(item);
    }

    private static class Interaction {
        final Player p;
        final EquipmentSlot hand;
        final ItemStack item;
        final Block clickedBlock;
        final BlockFace blockFace;
        final Action action;
        boolean cancel;
        boolean denyBlockUse;
        boolean successful = false;

        Interaction(Player p, EquipmentSlot hand, ItemStack item, Block clickedBlock, BlockFace blockFace, Action action) {
            this.p = p;
            this.hand = hand;
            this.item = item;
            this.clickedBlock = clickedBlock;
            this.blockFace = blockFace;
            this.action = action;
        }
    }

}
