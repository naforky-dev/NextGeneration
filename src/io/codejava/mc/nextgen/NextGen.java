package io.codejava.mc.nextgen;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.block.Action;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

// Imports needed for the respawn logic
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import java.util.concurrent.ThreadLocalRandom;


import java.time.Duration;
import java.util.Objects;


public class NextGen extends JavaPlugin implements Listener {
    // portaldeath 옵션 상태
    // --- UPDATED: This is now just a default, will be overwritten by config ---
    private boolean portalDeathEnabled = true;

    // --- NEW: Field to store the default duration from config ---
    private Duration defaultEndActivationDuration = Duration.ofHours(2);


    public boolean isPortalDeathEnabled() {
        return portalDeathEnabled;
    }

    public void setPortalDeathEnabled(boolean enabled) {
        this.portalDeathEnabled = enabled;
    }

    // 문자열을 Duration으로 변환 (NextGenCommand와 동일)
    private Duration parseDuration(String input) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");
        java.util.regex.Matcher matcher = pattern.matcher(input);
        if (matcher.matches()) {
            int hours = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
            int minutes = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int seconds = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        }
        return null;
    }

    // Duration을 한글로 포맷 (NextGenCommand와 동일)
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
    // 엔드 활성화 타이머 상태 공유용
    public boolean isEndActivated() {
        if (getCommandExecutor() instanceof NextGenCommand cmd) {
            return cmd.isEndActivated();
        }
        return false;
    }

    public Duration getEndActivationLeft() {
        if (getCommandExecutor() instanceof NextGenCommand cmd) {
            return cmd.getEndActivationLeft();
        }
        return Duration.ZERO;
    }
    
    // --- NEW: Getter for the command class to read the config value ---
    public Duration getDefaultEndActivationDuration() {
        return defaultEndActivationDuration;
    }

    private NextGenCommand getCommandExecutor() {
        return (NextGenCommand) Objects.requireNonNull(getCommand("nextgen")).getExecutor();
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 엔드 포탈 프레임 상호작용 차단 제거: 이제 플레이어가 정상적으로 눈/진주를 넣을 수 있음
    }

    @EventHandler
    public void onPlayerPortal(org.bukkit.event.player.PlayerPortalEvent event) {
        if (event.getTo() == null) return;
        org.bukkit.Location to = event.getTo();
        if (to.getWorld() == null) return;
        if (!isGameActive()) return;
        Player player = event.getPlayer();
        // 엔드로 이동 시도
        if (to.getWorld().getEnvironment() == org.bukkit.World.Environment.THE_END) {
            if (!isEndActivated()) {
                event.setCancelled(true);
                player.sendMessage(Component.text("엔드가 활성화되지 않아 엔드 이동이 불가능합니다.", NamedTextColor.RED));
                if (!isPortalDeathEnabled()) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, 200, 1));
                    player.sendMessage(Component.text("portaldeath가 비활성화되어 화염 저항이 부여됩니다. (10초)", NamedTextColor.GRAY));
                }
                return;
            }
            // 엔드가 활성화된 경우: 아무런 추가 조치 없이 자연스럽게 이동
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (isEndActivated()) return;
        if (!isGameActive()) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.ENDER_PEARL) {
            event.setCancelled(true);
            Duration left = getEndActivationLeft();
            String msg = "엔드가 비활성화 상태입니다. 활성화까지 " + formatDuration(left) + " 남았습니다.";
            player.sendMessage(Component.text(msg, NamedTextColor.RED));
        }
    }
    
    // --- START: MOVED FROM NextGenCommand.java ---
    // This logic now correctly resides in the main plugin class which implements Listener
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // **PaperMC Check:** Use isBedSpawn() or isAnchorSpawn() for a reliable check.
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
    
    // --- END: MOVED FROM NextGenCommand.java ---
    

    private boolean gameActive = false;
    // --- UPDATED: This is now just a default, will be overwritten by config ---
    private int borderSize = 1000; // Default border size

    @Override
    public void onEnable() {
        // --- NEW: Load and save config ---
        saveDefaultConfig(); // Creates config.yml if it doesn't exist
        
        // Load values from config, using old values as fallbacks
        this.borderSize = getConfig().getInt("nextgen-settings.default-border-size", 1000);
        this.portalDeathEnabled = getConfig().getBoolean("nextgen-settings.portal-death-on-entry", true);
        
        String durationStr = getConfig().getString("nextgen-settings.default-end-time", "2h");
        Duration loadedDuration = parseDuration(durationStr);
        if (loadedDuration != null && !loadedDuration.isZero()) {
            this.defaultEndActivationDuration = loadedDuration;
        } else {
            getLogger().warning("Invalid default-end-time in config.yml! Using default 2h.");
            this.defaultEndActivationDuration = Duration.ofHours(2);
        }
        
        // Log the loaded values
        getLogger().info("Loaded default border size: " + this.borderSize);
        getLogger().info("Loaded default end time: " + durationStr);
        getLogger().info("Loaded portal death setting: " + this.portalDeathEnabled);
        // --- END NEW ---


        // Register the event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register the command executor and pass an instance of this main class
        // This allows the command class to access our gameActive and borderSize variables
        NextGenCommand commandExecutor = new NextGenCommand(this);
        Objects.requireNonNull(getCommand("nextgen")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("nextgen")).setTabCompleter(commandExecutor);
        // Register '/t' as an independent command handled by the same executor
        if (getCommand("t") != null) {
            Objects.requireNonNull(getCommand("t")).setExecutor(commandExecutor);
            Objects.requireNonNull(getCommand("t")).setTabCompleter(commandExecutor);
        }

        getLogger().info("[NextGen] v1.2-java");
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        // Only trigger the win condition if the game is active
        if (!gameActive) {
            return;
        }

        NamespacedKey advancementKey = event.getAdvancement().getKey();

        // The advancement for picking up the dragon egg is "minecraft:end/dragon_egg"
        if (advancementKey.toString().equals("minecraft:end/dragon_egg")) {
            Player winner = event.getPlayer();
            String winnerName = winner.getName();

            // --- NEW: Title Logic ---
            // Create the title text using Paper's modern Adventure API
            final Component mainTitle = Component.text(winnerName + " 승리!", NamedTextColor.YELLOW);
            
            // Define how long the title should fade in, stay, and fade out
            final Title.Times times = Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(4), Duration.ofSeconds(1));
            
            // Create the title object
            final Title title = Title.title(mainTitle, Component.empty(), times); // Component.empty() means no subtitle

            // Show the title to every player online
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.showTitle(title);
            }
            
            // We can keep the chat message too for a permanent record
            Bukkit.broadcast(Component.text(winnerName + "이(가) 가장 먼저 목표 발전 과제를 달성했습니다!", NamedTextColor.GOLD));

            // End the game
            Bukkit.broadcast(Component.text("게임을 종료합니다.", NamedTextColor.GRAY));
            setGameActive(false);
            Bukkit.getWorlds().forEach(world -> world.getWorldBorder().reset());
        }
    }
    
    // --- NEW: Method to reload config values ---
    public void reloadPluginConfig() {
        reloadConfig(); // This re-reads config.yml from disk
        
        // Now, re-load all the values into the variables
        this.borderSize = getConfig().getInt("nextgen-settings.default-border-size", 1000);
        this.portalDeathEnabled = getConfig().getBoolean("nextgen-settings.portal-death-on-entry", true);
        
        String durationStr = getConfig().getString("nextgen-settings.default-end-time", "2h");
        Duration loadedDuration = parseDuration(durationStr);
        if (loadedDuration != null && !loadedDuration.isZero()) {
            this.defaultEndActivationDuration = loadedDuration;
        } else {
            getLogger().warning("Invalid default-end-time in config.yml! Using default 2h.");
            this.defaultEndActivationDuration = Duration.ofHours(2);
        }
        
        // Also update the command executor's copy of the duration
        getCommandExecutor().setDefaultEndActivationDuration(this.defaultEndActivationDuration);

        getLogger().info("Config reloaded.");
    }
    

    // --- Getter and Setter Methods ---
    // These methods allow the NextGenCommand class to safely access and modify the state.

    public boolean isGameActive() {
        return gameActive;
    }

    public void setGameActive(boolean active) {
        this.gameActive = active;
    }

    public int getBorderSize() {
        return borderSize;
    }

    public void setBorderSize(int size) {
        this.borderSize = size;
    }
}