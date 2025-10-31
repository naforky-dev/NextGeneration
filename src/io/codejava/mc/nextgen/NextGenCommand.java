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

import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import io.papermc.paper.event.player.PlayerRespawnEvent; // for onPlayerRespawn

public class NextGenCommand implements CommandExecutor, TabCompleter {

    private final NextGen plugin;
    private boolean showTimer = false;
    private Duration endActivationDuration = Duration.ofHours(2);
    private Instant endActivationStart = null;
    private BossBar bossBar = null;
    private boolean endActivated = false;
    public boolean isEndActivated() {
        return endActivated;
    }

    public Duration getEndActivationLeft() {
        if (endActivationStart == null) return Duration.ZERO;
        Duration left = Duration.between(Instant.now(), endActivationStart.plus(endActivationDuration));
        return left.isNegative() ? Duration.ZERO : left;
    }

    public void startEndActivationTimer() {
        endActivationStart = Instant.now();
        endActivated = false;
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkEndActivationTimer, 20L, 20L);
    }

    private boolean thirtyWarned = false;
    private boolean tenWarned = false;

    private void checkEndActivationTimer() {
        if (endActivationStart == null || endActivated) return;
        Duration left = getEndActivationLeft();
        playerShowTimer.put(player, Boolean.valueOf(show));
        updateBossBar();
        if (seconds <= 0) {
            endActivated = true;
            Bukkit.broadcast(Component.text("엔드가 활성화되었습니다!", NamedTextColor.LIGHT_PURPLE));
            if (bossBar != null) bossBar.setVisible(false);
            return;
        }
        if (!thirtyWarned && seconds <= 1800) {
            Bukkit.broadcast(Component.text("엔드 활성화까지 30분 남았습니다!", NamedTextColor.YELLOW));
            thirtyWarned = true;
        }
        if (!tenWarned && seconds <= 600) {
            Bukkit.broadcast(Component.text("엔드 활성화까지 10분 남았습니다!", NamedTextColor.RED));
            tenWarned = true;
        }
    }
    private final Map<Player, Boolean> playerShowTimer = new HashMap<>();

    public NextGenCommand(NextGen plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
            case "showtimer":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /nextgen showtimer <true|false>", NamedTextColor.RED));
                    return true;
                }
                handleShowTimer(player, args[1]);
                break;
            case "endactivationtime":
                if (args.length < 2) {
                    player.sendMessage(Component.text("Usage: /nextgen endactivationtime <duration>", NamedTextColor.RED));
                    return true;
                }
                handleEndActivationTime(player, args[1]);
                break;
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // **PaperMC Check:** Use isBedSpawn() or isAnchorSpawn() for a reliable check.
        // If neither is true, or if isMissingRespawnBlock() is true (e.g., bed was obstructed/destroyed),
        // the player is respawning at the world spawn or another non-bed/anchor location.
        // The most direct way to check for a *failed* bed respawn is often:
        boolean hasRespawnPoint = event.isBedSpawn() || event.isAnchorSpawn();

        if (!hasRespawnPoint || event.isMissingRespawnBlock()) {
            
            // Player does not have a valid respawn point, so calculate a random location
            Location randomSpawn = findRandomSafeLocationInWorldBorder(player.getWorld());

            if (randomSpawn != null) {
                // Set the new respawn location
                event.setRespawnLocation(randomSpawn);
                player.sendMessage(Component.text("리스폰 설정이 되어있는 침대가 없어 랜덤 위치에서 스폰됩니다.", NamedTextColor.YELLOW));
            } else {
                // Handle the rare case where no safe location was found
                player.sendMessage(Component.text("스폰할 적절한 랜덤 위치를 찾지 못하여 월드 스폰에서 스폰됩니다.", NamedTextColor.RED));
            }
        }
    }

    // --- Helper Method to Find Random Location ---
    
    // You should put this method in your main plugin class or a utility class
    // and make the world configurable if needed.
    private Location findRandomSafeLocationInWorldBorder(World world) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double size = border.getSize() / 2.0; // Half the size for radius
        
        int maxAttempts = 50;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < maxAttempts; i++) {
            // Calculate random X and Z coordinates within the world border radius
            double minX = center.getX() - size + 1; // +1 to stay inside
            double maxX = center.getX() + size - 1; // -1 to stay inside
            double minZ = center.getZ() - size + 1; // +1 to stay inside
            double maxZ = center.getZ() + size - 1; // -1 to stay inside

            double randomX = random.nextDouble(minX, maxX);
            double randomZ = random.nextDouble(minZ, maxZ);
            
            // Find a safe Y level (e.g., top-most block that is safe)
            int y = world.getHighestBlockYAt((int) randomX, (int) randomZ);
            
            Location location = new Location(world, randomX, y, randomZ);

            // Important: You must ensure the location is "safe" (not in lava, water, a wall, etc.)
            if (isSafeSpawnLocation(location)) {
                // Center the player on the block and add a small Y offset (0.5 to be sure)
                return location.add(0.5, 0.5, 0.5); 
            }
        }
        
        return null; // Return null if no safe location is found after max attempts
    }
    
    // --- Basic Safety Check ---
    private boolean isSafeSpawnLocation(Location loc) {
        if (loc.getBlockY() < loc.getWorld().getMinHeight() + 2) return false;
        
        org.bukkit.block.Block feet = loc.getBlock();
        org.bukkit.block.Block head = feet.getRelative(0, 1, 0);
        org.bukkit.block.Block under = feet.getRelative(0, -1, 0);

        // Check if the two upper blocks are not solid/safe and the block under is solid/safe to stand on
        return !feet.getType().isSolid() && !head.getType().isSolid() && under.getType().isSolid() && !under.isLiquid();
    }

    private void handleTime(Player player) {
        if (endActivationStart == null) {
            player.sendMessage(Component.text("엔드 활성화 타이머가 시작되지 않았습니다.", NamedTextColor.RED));
            return;
        }
        Duration left = Duration.between(Instant.now(), endActivationStart.plus(endActivationDuration));
        if (left.isNegative() || left.isZero()) {
            player.sendMessage(Component.text("엔드가 이미 활성화되었습니다!", NamedTextColor.GREEN));
            return;
        }
        String msg = "엔드 활성화까지 " + formatDuration(left) + " 남았습니다.";
        player.sendMessage(Component.text(msg, NamedTextColor.YELLOW));
    }

    private void handleShowTimer(Player player, String value) {
        boolean show;
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            show = Boolean.parseBoolean(value);
        } else {
            player.sendMessage(Component.text("true 또는 false만 입력 가능합니다.", NamedTextColor.RED));
            return;
        }
        playerShowTimer.put(player, show);
        if (show) {
            showBossBar(player);
            player.sendMessage(Component.text("타이머가 보스바에 표시됩니다.", NamedTextColor.YELLOW));
        } else {
            hideBossBar(player);
            player.sendMessage(Component.text("타이머 보스바가 숨겨집니다.", NamedTextColor.YELLOW));
        }
    }

    private void handleEndActivationTime(Player player, String durationStr) {
        Duration duration = parseDuration(durationStr);
        if (duration == null || duration.isZero() || duration.isNegative()) {
            player.sendMessage(Component.text("올바른 시간 형식이 아닙니다. 예시: 2h10m10s", NamedTextColor.RED));
            return;
        }
        endActivationDuration = duration;
        player.sendMessage(Component.text("엔드 활성화까지의 시간이 " + formatDuration(duration) + "로 설정되었습니다.", NamedTextColor.GREEN));
    }

    private Duration parseDuration(String input) {
        Pattern pattern = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");
        Matcher matcher = pattern.matcher(input);
        if (matcher.matches()) {
            int hours = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
            int minutes = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int seconds = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        }
        return null;
    }

    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        } else if (minutes > 0) {
            return minutes + "분 " + seconds + "초";
        } else {
            return seconds + "초";
        }
    }

    private void showBossBar(Player player) {
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar("엔드 활성화까지...", BarColor.YELLOW, BarStyle.SOLID);
        }
        bossBar.addPlayer(player);
        updateBossBar();
    }

    private void hideBossBar(Player player) {
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    private void updateBossBar() {
        if (bossBar == null || endActivationStart == null) return;
        Duration left = Duration.between(Instant.now(), endActivationStart.plus(endActivationDuration));
        if (left.isNegative() || left.isZero()) {
            bossBar.setVisible(false);
            return;
        }
        String title;
        long hours = left.toHours();
        long minutes = (left.toMinutes() % 60);
        long seconds = (left.getSeconds() % 60);
        if (hours > 0) {
            title = "엔드 활성화까지 " + hours + "시간 " + minutes + "분";
        } else {
            title = "엔드 활성화까지 " + minutes + "분 " + seconds + "초";
        }
        bossBar.setTitle(title);
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double)left.getSeconds() / endActivationDuration.getSeconds())));
        bossBar.setVisible(true);
    }

    private void handleStart(Player player) {
        if (plugin.isGameActive()) {
            player.sendMessage(Component.text("게임이 이미 실행 중입니다!", NamedTextColor.RED));
            return;
        }

    player.sendMessage(Component.text("게임을 시작합니다. 필요한 구조물 검색 중...", NamedTextColor.YELLOW));
    startEndActivationTimer();
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
        player.sendMessage(Component.text("[NextGen] v1.3-java", NamedTextColor.GOLD));
        player.sendMessage(Component.text("/nextgen start - 게임 시작", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/nextgen abort - 게임 중단", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/nextgen border <500-5000> - 월드보더 크기 변경", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/nextgen reload - 서버 새로고침", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/nextgen showtimer - 엔드 활성화 타이머", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/nextgen endactivationtime - 엔드 활성화 시간", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("/nextgen portaldeath - 엔드 비활성화시 포탈 사망 설정", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "abort", "border", "reload", "showtimer", "endactivationtime", "portaldeath");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("border")) {
            return Arrays.asList("1000", "1500", "2000");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("showtimer")) {
            return Arrays.asList(true, false);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("endactivationtime")) {
            return Arrays.asList("2h", "1h30m", "45m", "10m");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("portaldeath")) {
            return Arrays.asList(true, false);
        }
        return null;
    }
}