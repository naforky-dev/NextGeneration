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

import java.time.Duration;
import java.util.Objects;

public class NextGen extends JavaPlugin implements Listener {

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

    private NextGenCommand getCommandExecutor() {
        return (NextGenCommand) Objects.requireNonNull(getCommand("nextgen")).getExecutor();
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isEndActivated()) return;
        if (!isGameActive()) return;
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.END_PORTAL_FRAME) {
                event.setCancelled(true);
                Duration left = getEndActivationLeft();
                String msg = "엔드가 비활성화 상태입니다. 활성화까지 " + formatDuration(left) + " 남았습니다.";
                player.sendMessage(Component.text(msg, NamedTextColor.RED));
            }
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

    private boolean gameActive = false;
    private int borderSize = 1000; // Default border size

    @Override
    public void onEnable() {
        // Register the event listener
        getServer().getPluginManager().registerEvents(this, this);

        // Register the command executor and pass an instance of this main class
        // This allows the command class to access our gameActive and borderSize variables
        NextGenCommand commandExecutor = new NextGenCommand(this);
        Objects.requireNonNull(getCommand("nextgen")).setExecutor(commandExecutor);
        Objects.requireNonNull(getCommand("nextgen")).setTabCompleter(commandExecutor);

        getLogger().info("[NextGen] v1.1-java");
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