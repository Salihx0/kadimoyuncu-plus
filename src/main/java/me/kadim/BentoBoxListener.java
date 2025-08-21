package me.kadim;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import world.bentobox.bentobox.api.events.island.IslandCreatedEvent;

import java.util.UUID;

public class BentoBoxListener implements Listener {

    private final KadimPlugin plugin;

    public BentoBoxListener(KadimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onIslandCreate(IslandCreatedEvent event) {
        if (!plugin.getConfig().getBoolean("bento-box-integration.enabled", false)) {
            return;
        }

        UUID playerUUID = event.getPlayerUUID();
        if (playerUUID == null) {
            return;
        }

        String rewardClaimedPath = "players." + playerUUID.toString() + ".bento-reward-claimed";
        if (plugin.getDataConfig().getBoolean(rewardClaimedPath, false)) {
            return;
        }

        String commandToRun = plugin.getConfig().getString("bento-box-integration.first-island-command");

        if (commandToRun == null || commandToRun.isEmpty()) {
            return;
        }

        String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
        if (playerName == null) {
            return;
        }

        commandToRun = commandToRun.replace("{player}", playerName);

        final String finalCommand = commandToRun;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);

            plugin.getDataConfig().set(rewardClaimedPath, true);
            plugin.markDataAsChanged();

            String logMessage = playerName + " adlı oyuncu ilk adasını oluşturduğu için rütbe komutu çalıştırıldı: " + finalCommand;

            // Logu dosyaya kaydet
            plugin.bentoBoxLogger.log(logMessage);

            // Ayar açıksa konsola da yazdır
            if (plugin.getConfig().getBoolean("bento-box-integration.log-to-console", false)) {
                plugin.getLogger().info(ChatColor.stripColor(logMessage));
            }
        });
    }
}
