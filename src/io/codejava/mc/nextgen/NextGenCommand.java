package io.codejava.mc.nextgen;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Biome;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.StructureSearchResult;
import org.bukkit.WorldBorder;


import java.util.Arrays;
import java.util.List;
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
                    player.sendMessage(Component.text("Usage: /nextgen border <size>", NamedTextColor.RED));
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
            player.sendMessage(Component.text("게임이 이미 실행 중입니다!", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("게임을 시작합니다. 필요한 구조물 검색 중...", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("구조물 검색은 다소 시간이 걸릴 수 있습니다.", NamedTextColor.YELLOW));

        World overworld = Bukkit.getWorld("world");
        World nether = Bukkit.getWorld("world_nether");

        if (overworld == null || nether == null) {
            player.sendMessage(Component.text("오류: (world, world_nether)를 찾지 못했습니다.", NamedTextColor.RED));
            return;
        }

        // 1. Stronghold 탐색 (즉시)
        StructureSearchResult strongholdResult = overworld.locateNearestStructure(player.getLocation(), Structure.STRONGHOLD, 10000, false);
        final Location strongholdLoc = (strongholdResult != null) ? strongholdResult.getLocation() : null;
        if (strongholdLoc == null) {
            player.sendMessage(Component.text("[NextGen] > [NGenError] Stronghold를 찾지 못했습니다.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("엔드 유적 위치를 찾았습니다. 5초 후 다음 구조물 검색을 시작합니다...", NamedTextColor.YELLOW));

        // 2. Fortress 탐색 (5초 후)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            StructureSearchResult fortressResult = nether.locateNearestStructure(player.getLocation(), Structure.FORTRESS, 5000, false);
            final Location fortressLoc = (fortressResult != null) ? fortressResult.getLocation() : null;
            if (fortressLoc == null) {
                player.sendMessage(Component.text("[NextGen] > [NGenError] Fortress를 찾지 못했습니다.", NamedTextColor.RED));
                return;
            }
            player.sendMessage(Component.text("네더 포트리스 위치를 찾았습니다. 5초 후 바이옴 검색을 시작합니다...", NamedTextColor.YELLOW));

            // 3. Warped Forest 바이옴 탐색 (5초 후)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location warpedForestLoc = nether.locateNearestBiome(player.getLocation(), Biome.WARPED_FOREST, 5000, 1);
                if (warpedForestLoc == null) {
                    player.sendMessage(Component.text("[NextGen] > [NGenError] 뒤틀린 숲 바이옴을 찾지 못했습니다.", NamedTextColor.RED));
                    return;
                }

                int size = plugin.getBorderSize();
                overworld.getWorldBorder().setCenter(strongholdLoc);
                overworld.getWorldBorder().setSize(size);
                Location netherCenter = fortressLoc.clone().add(warpedForestLoc).multiply(0.5);
                nether.getWorldBorder().setCenter(netherCenter);
                nether.getWorldBorder().setSize(size);
                plugin.setGameActive(true);
                Bukkit.broadcast(Component.text("게임이 시작되었습니다! " + size + "x" + size + " 크기의 월드보더가 생성되었습니다.", NamedTextColor.GREEN));
                teleportAllPlayersRandomly(overworld);
            }, 100L); // 5초(100틱) 후
        }, 100L); // 5초(100틱) 후
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
            p.sendMessage(Component.text("모든 플레이어가 렌덤 위치로 텔레포트되었습니다!", NamedTextColor.AQUA));
        }
    }
    
    // ... (The other handleAbort, handleReload, handleBorder, sendHelp, and onTabComplete methods are the same) ...
    private void handleAbort(Player player) {
        if (!plugin.isGameActive()) {
            player.sendMessage(Component.text("중단할 게임이 없습니다.", NamedTextColor.RED));
            return;
        }
        plugin.setGameActive(false);
        for (World world : Bukkit.getWorlds()) {
            world.getWorldBorder().reset();
        }
        Bukkit.broadcast(Component.text(player.getName() + "이(가) 게임을 중단하였습니다. 월드보더가 제거됩니다.", NamedTextColor.YELLOW));
    }

    private void handleReload(Player player) {
    player.sendMessage(Component.text("서버를 새로고침합니다.", NamedTextColor.GOLD));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "reload confirm");
    }

    private void handleBorder(Player player, String sizeArg) {
        try {
            int size = Integer.parseInt(sizeArg);
            if (size < 500 || size > 5000) {
                player.sendMessage(Component.text("월드보더의 크기는 최소 500, 최대 5000이어야 합니다.", NamedTextColor.RED));
                return;
            }
            plugin.setBorderSize(size);
            player.sendMessage(Component.text("다음 게임에서 월드보더가 " + size + "x" + size + " 크기로 생성됩니다.", NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("'" + sizeArg + "'은(는) 유효한 숫자가 아닙니다.", NamedTextColor.RED));
        }
    }

    private void sendHelp(Player player) {
    player.sendMessage(Component.text("[NextGen] v1.0-java", NamedTextColor.GOLD));
    player.sendMessage(Component.text("/nextgen start - 게임 시작", NamedTextColor.YELLOW));
    player.sendMessage(Component.text("/nextgen abort - 게임 중단", NamedTextColor.YELLOW));
    player.sendMessage(Component.text("/nextgen border <500-5000> - 월드보더 크기 변경", NamedTextColor.YELLOW));
    player.sendMessage(Component.text("/nextgen reload - 서버 새로고침", NamedTextColor.YELLOW));
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