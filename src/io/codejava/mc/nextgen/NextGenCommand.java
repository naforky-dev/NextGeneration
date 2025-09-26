package io.codejava.mc.nextgen;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.structure.StructureType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NextGenCommand implements CommandExecutor, TabCompleter {

    private final NextGen plugin;

    // Constructor to get the instance of our main plugin class
    public NextGenCommand(DragonWin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("[NextGen] 이 명령어는 플레이어만 사용이 가능합니다.");
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
            player.sendMessage(ChatColor.RED + "[NextGen] 게임이 이미 실행중입니다.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "[NextGen] 게임을 시작합니다. 현재 월드에서 필요한 구조물 검색 중...");

        // Run the search asynchronously to avoid freezing the server
        CompletableFuture.runAsync(() -> {
            World overworld = Bukkit.getWorld("world");
            World nether = Bukkit.getWorld("world_nether");

            if (overworld == null || nether == null) {
                player.sendMessage(ChatColor.RED + "[NextGen] > [NGenError] 오류: 필수 월드를 찾지 못했습니다. (world, world_nether)");
                return;
            }

            // --- Overworld Setup ---
            // Find the nearest stronghold (엔드 유적/End Ruins)
            Location strongholdLoc = overworld.locateNearestStructure(player.getLocation(), Structure.STRONGHOLD, 10000, false);
            if (strongholdLoc == null) {
                player.sendMessage(ChatColor.RED + "현재 월드에서 엔드 유적을 찾지 못했습니다. 게임을 종료합니다.");
                return;
            }

            // --- Nether Setup ---
            // Find the nearest fortress and warped forest (blue forest with Endermen)
            Location fortressLoc = nether.locateNearestStructure(player.getLocation(), Structure.NETHER_FORTRESS, 5000, false);
            // The biome you're looking for is WARPED_FOREST
            Location warpedForestLoc = nether.locateNearestBiome(player.getLocation(), "minecraft:warped_forest", 5000, 1, 1);
            
            if (fortressLoc == null || warpedForestLoc == null) {
                player.sendMessage(ChatColor.RED + "네더 월드에서 푸른 숲과 포트리스 유적을 찾지 못했거나 존재하지만 현재 월드보더 크기를 벗어납니다. 게임을 종료합니다.");
                return;
            }

            // Structures are found, now set everything up on the main server thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                int size = plugin.getBorderSize();
                
                // Set Overworld border centered on the stronghold
                overworld.getWorldBorder().setCenter(strongholdLoc);
                overworld.getWorldBorder().setSize(size);
                
                // For the Nether, we'll center the border between the two points
                Location netherCenter = fortressLoc.clone().add(warpedForestLoc).multiply(0.5);
                nether.getWorldBorder().setCenter(netherCenter);
                nether.getWorldBorder().setSize(size); // Nether border is 1:1 with Overworld

                plugin.setGameActive(true);
                Bukkit.broadcastMessage(ChatColor.GREEN + "게임 시작! " + size + "x" + size + " 크기의 월드보더가 생성되었습니다!");
                //Bukkit.broadcastMessage(ChatColor.AQUA + "Find the Stronghold centered at X: " + strongholdLoc.getBlockX() + ", Z: " + strongholdLoc.getBlockZ() + " in the Overworld!");
            });

        }).exceptionally(ex -> {
            player.sendMessage(ChatColor.DARK_RED + "필요한 구조물을 찾는 도중 예기치 못한 오류가 발생했습니다.");
            ex.printStackTrace();
            return null;
        });
    }

    private void handleAbort(Player player) {
        if (!plugin.isGameActive()) {
            player.sendMessage(ChatColor.RED + "중단할 게임이 없습니다.");
            return;
        }
        plugin.setGameActive(false);
        // Reset borders in all worlds
        for (World world : Bukkit.getWorlds()) {
            world.getWorldBorder().reset();
        }
        Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + "이(가) 게임을 중단했습니다. 월드보더가 제거됩니다.");
    }

    private void handleReload(Player player) {
        player.sendMessage(ChatColor.GOLD + "서버를 새로고침합니다.");
        // Using dispatchCommand is safer than Bukkit.reload()
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
            player.sendMessage(ChatColor.GREEN + "다음 게임의 월드보더 크기가 " + size + "x" + size + "으로 설정되었습니다.");
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
            return Arrays.asList("1000", "1500", "2000"); // Suggest some common sizes
        }
        return null; // No other suggestions
    }
}