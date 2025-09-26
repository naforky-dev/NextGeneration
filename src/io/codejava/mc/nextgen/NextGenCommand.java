package io.codejava.mc.nextgen;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Biome;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.util.StructureSearchResult;
import org.bukkit.util.BiomeSearchResult;
import org.bukkit.util.Vector;
import org.bukkit.WorldBorder;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class NextGenCommand implements CommandExecutor, TabCompleter {

    // The main class is now NextGen
    private final NextGen plugin;

    public NextGenCommand(NextGen plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // ... (The rest of the onCommand logic is the same as before) ...
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용 가능합니다.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                handleStart(player);
                break;
            case "abort":
                handleAbort(player);
                break;
            case "reload":
                handleReload(player);
                break;
            case "border":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /nextgen border <size>");
                    return true;
                }
                handleBorder(player, args[1]);
                break;
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void handleStart(Player player) {
        if (plugin.isGameActive()) {
            player.sendMessage(ChatColor.RED + "게임이 이미 실행중입니다!");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "[NextGen] 게임을 시작합니다! 필수 구조물을 검색 중...");

        CompletableFuture.runAsync(() -> {
            World overworld = Bukkit.getWorld("world");
            World nether = Bukkit.getWorld("world_nether");

            if (overworld == null || nether == null) {
                player.sendMessage(ChatColor.RED + "[NextGen] 오류: 다음 월드를 찾지 못했습니다. (world, world_nether)");
                return;
            }

            StructureSearchResult strongholdLoc = overworld.locateNearestStructure(player.getLocation(), Structure.STRONGHOLD, 10000, false);
            Location stronghold = null;
            if (strongholdLoc != null) {
                //stronghold = strongholdLoc.getLocation();
                stronghold = strongholdLoc != null ? strongholdLoc.getLocation() : null;
            }
            //Location fortressLoc = nether.locateNearestStructure(player.getLocation(), Structure.NETHER_FORTRESS, 5000, false);
            Location fortressLoc = null;
            StructureSearchResult result = nether.locateNearestStructure(player.getLocation(), Structure.FORTRESS, 5000, false);
            
            if (result != null) {
                //fortressLoc = result.getLocation();
                fortressLoc = result != null ? result.getLocation() : null;
            }
            BiomeSearchResult warpedForestLoc = nether.locateNearestBiome(player.getLocation(), 5000, 1, 1, Biome.WARPED_FOREST);
            Location warpedForest = null;
            if (warpedForestLoc != null) {
                //warpedForest = warpedForestLoc.getLocation();
                warpedForest = warpedForestLoc != null ? warpedForestLoc.getLocation() : null;
            }
            
            if (strongholdLoc == null || fortressLoc == null || warpedForestLoc == null) {
                player.sendMessage(ChatColor.RED + "[NextGen] 필수 구조물을 찾지 못했거나 일부만 찾았습니다. 게임을 종료합니다.");
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                int size = plugin.getBorderSize();
                
                overworld.getWorldBorder().setCenter(stronghold);
                overworld.getWorldBorder().setSize(size);
                
                Location netherCenter = fortressLoc.clone().add(warpedForest).multiply(0.5);
                nether.getWorldBorder().setCenter(netherCenter);
                nether.getWorldBorder().setSize(size);

                plugin.setGameActive(true);
                Bukkit.broadcast(Component.text("게임 시작! 크기 " + size + "x" + size + "의 월드보더가 생성되었습니다!", NamedTextColor.GREEN));
                
                // --- NEW: Teleport all players to a random, safe location ---
                teleportAllPlayersRandomly(overworld);
            });

        }).exceptionally(ex -> {
            player.sendMessage(ChatColor.DARK_RED + "[NextGen] > [NGenError] An unexpected error occurred while searching for structures.");
            player.sendMessage(ChatColor.DARK_RED + "필수 구조물을 검색하는 도중 예기치 못한 오류가 발생했습니다.");
            ex.printStackTrace();
            return null;
        });
    }

    private void teleportAllPlayersRandomly(World world) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double size = border.getSize();
        double radius = size / 2.0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            // Using ThreadLocalRandom is better for multithreading contexts
            double randomX = center.getX() + (ThreadLocalRandom.current().nextDouble() * size - radius);
            double randomZ = center.getZ() + (ThreadLocalRandom.current().nextDouble() * size - radius);

            // Find the highest solid block (ignoring leaves and glass) at this location
            // This is a much safer way to find the ground
            int y = world.getHighestBlockYAt((int) randomX, (int) randomZ, HeightMap.MOTION_BLOCKING_NO_LEAVES);

            Location teleportLocation = new Location(world, randomX, y + 1.0, randomZ);

            // Use Paper's async teleport for better performance 🪄
            p.teleportAsync(teleportLocation);
            p.sendMessage(ChatColor.AQUA + "모든 플레이어가 렌덤 위치로 텔레포트되었습니다!");
        }
    }
    
    // ... (The other handleAbort, handleReload, handleBorder, sendHelp, and onTabComplete methods are the same) ...
    private void handleAbort(Player player) {
        if (!plugin.isGameActive()) {
            player.sendMessage(ChatColor.RED + "중단할 게임이 없습니다.");
            return;
        }
        plugin.setGameActive(false);
        for (World world : Bukkit.getWorlds()) {
            world.getWorldBorder().reset();
        }
        Bukkit.broadcast(Component.text(player.getName() + "이(가) 게임을 중단하였습니다. 월드보더가 제거됩니다.", NamedTextColor.YELLOW));
    }

    private void handleReload(Player player) {
        player.sendMessage(ChatColor.GOLD + "서버를 새로고침합니다.");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "reload confirm");
    }

    private void handleBorder(Player player, String sizeArg) {
        try {
            int size = Integer.parseInt(sizeArg);
            if (size < 500 || size > 5000) {
                player.sendMessage(ChatColor.RED + "월드보더의 크기는 최소 500, 최대 5000이어야 합니다.");
                return;
            }
            plugin.setBorderSize(size);
            player.sendMessage(ChatColor.GREEN + "다음 게임에서 월드보더가 " + size + "x" + size + " 크기로 생성됩니다.");
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "'" + sizeArg + "'은(는) 유효한 숫자가 아닙니다.");
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "[NextGen] v1.0-java");
        player.sendMessage(ChatColor.YELLOW + "/nextgen start" + ChatColor.GRAY + " - 게임 시작");
        player.sendMessage(ChatColor.YELLOW + "/nextgen abort" + ChatColor.GRAY + " - 게임 중단");
        player.sendMessage(ChatColor.YELLOW + "/nextgen border <500-5000>" + ChatColor.GRAY + " - 월드보더 크기 변경");
        player.sendMessage(ChatColor.YELLOW + "/nextgen reload" + ChatColor.GRAY + " - 서버 새로고침");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "abort", "border", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("border")) {
            return Arrays.asList("1000", "1500", "2000");
        }
        return null;
    }
}