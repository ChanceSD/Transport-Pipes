package de.robotricker.transportpipes.utils;

import de.robotricker.transportpipes.PlayerSettingsService;
import de.robotricker.transportpipes.ThreadService;
import de.robotricker.transportpipes.TransportPipes;
import de.robotricker.transportpipes.config.GeneralConf;
import de.robotricker.transportpipes.config.LangConf;
import de.robotricker.transportpipes.duct.Duct;
import de.robotricker.transportpipes.duct.manager.GlobalDuctManager;
import de.robotricker.transportpipes.location.BlockLocation;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

public class WorldUtils {

    private static final Map<Player, Integer> hidingDuctsTimers = new HashMap<>();

    /**
     * THREAD-SAFE
     */
    public static List<Player> getPlayerList(World world) {
        // Bukkit.getOnlinePlayers is the only thread safe playerlist getter
        List<Player> playerList = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Objects.equals(p.getLocation().getWorld(), world)) {
                playerList.add(p);
            }
        }
        return playerList;
    }

    public static boolean isContainerBlock(Material material) {
        return switch (material) {
            case DISPENSER, CHEST, BARREL, FURNACE, BLAST_FURNACE, SMOKER, TRAPPED_CHEST, DROPPER, HOPPER, BREWING_STAND, SHULKER_BOX, WHITE_SHULKER_BOX, BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, GRAY_SHULKER_BOX, BROWN_SHULKER_BOX, CYAN_SHULKER_BOX, GREEN_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, LIME_SHULKER_BOX, MAGENTA_SHULKER_BOX, ORANGE_SHULKER_BOX, PINK_SHULKER_BOX, PURPLE_SHULKER_BOX, RED_SHULKER_BOX, YELLOW_SHULKER_BOX -> true;
            default -> false;
        };
    }

    public static boolean lwcProtection(Block b) {
        if (Bukkit.getPluginManager().isPluginEnabled("LWC")) {
            try {
                com.griefcraft.model.Protection protection = com.griefcraft.lwc.LWC.getInstance().findProtection(b);
                return protection != null && protection.getType() != com.griefcraft.model.Protection.Type.PUBLIC;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static void startShowHiddenDuctsProcess(Player player, GlobalDuctManager globalDuctManager, ThreadService threadService, TransportPipes transportPipes, GeneralConf generalConf, PlayerSettingsService playerSettingsService) {

        int renderDistance = playerSettingsService.getOrCreateSettingsConf(player).getRenderDistance();

        if (hidingDuctsTimers.containsKey(player)) {
            return;
        }
        Set<Duct> showingDucts = new HashSet<>();
        Map<BlockLocation, Duct> ductMap = globalDuctManager.getDucts(player.getWorld());
        for (BlockLocation bl : ductMap.keySet()) {
            Duct duct = ductMap.get(bl);
            Block ductBlock = duct.getBlockLoc().toBlock(duct.getWorld());
            if (ductBlock.getLocation().distance(player.getLocation()) > renderDistance) {
                continue;
            }
            if (duct.obfuscatedWith() != null && ductBlock.getBlockData().getMaterial() != Material.BARRIER) {
                showingDucts.add(duct);
                ductBlock.setBlockData(Material.BARRIER.createBlockData(), false);
                threadService.tickDuctSpawnAndDespawn(duct);
            }
        }

        int duration = generalConf.getShowHiddenDuctsTime();
        int[] task = new int[]{0};
        hidingDuctsTimers.put(player, duration);

        task[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(transportPipes, () -> {

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(LangConf.Key.SHOW_HIDDEN_DUCTS.get(hidingDuctsTimers.get(player))));

            if (hidingDuctsTimers.get(player) == 0) {
                hidingDuctsTimers.remove(player);
                for (Duct duct : showingDucts) {
                    if (globalDuctManager.getDuctAtLoc(duct.getWorld(), duct.getBlockLoc()) != duct) {
                        continue;
                    }
                    Block ductBlock = duct.getBlockLoc().toBlock(duct.getWorld());
                    if (ductBlock.getBlockData().getMaterial() == Material.BARRIER) {
                        ductBlock.setBlockData(duct.obfuscatedWith() == null ? Material.AIR.createBlockData() : duct.obfuscatedWith(), false);
                        threadService.tickDuctSpawnAndDespawn(duct);
                    }
                }
                Bukkit.getScheduler().cancelTask(task[0]);
            } else {
                hidingDuctsTimers.put(player, hidingDuctsTimers.get(player) - 1);
            }
        }, 0L, 20L);

    }

}
