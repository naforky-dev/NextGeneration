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
            String playerName = event.getPlayer().getName();

            // Broadcast a fancy message to everyone on the server! 🏆
            Bukkit.broadcastMessage(ChatColor.GOLD + "========================================");
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + playerName + "이(가) 가장 먼저 발전 과제를 달성했습니다!");
            Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "게임을 종료합니다.");
            Bukkit.broadcastMessage(ChatColor.GOLD + "========================================");

            // The game is now over
            setGameActive(false);
            // You might want to remove the world border here as well
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