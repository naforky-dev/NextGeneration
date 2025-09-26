package io.codejava.mc.nextgen;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class NextGen extends JavaPlugin implements Listener {

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

        getLogger().info("[NextGen] v1.0-java");
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