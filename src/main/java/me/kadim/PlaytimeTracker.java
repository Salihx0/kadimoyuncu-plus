package me.kadim;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeTracker implements Listener {

    private final KadimPlugin plugin;
    private final FileConfiguration dataConfig;
    private final Map<UUID, Long> playerJoinTimes;
    private BukkitTask saveTask;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat weekFormat = new SimpleDateFormat("yyyy-ww");
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");

    public PlaytimeTracker(KadimPlugin plugin) {
        this.plugin = plugin;
        this.dataConfig = plugin.getDataConfig();
        this.playerJoinTimes = new HashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        String currentName = dataConfig.getString("players." + uuid + ".name");
        if (currentName == null || !currentName.equals(player.getName())) {
            dataConfig.set("players." + uuid + ".name", player.getName());
            plugin.markDataAsChanged();
        }

        playerJoinTimes.put(uuid, System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        savePlayerPlaytime(player.getUniqueId());
        playerJoinTimes.remove(player.getUniqueId());
    }

    public void startPlaytimeSaveTask() {
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                savePlayerPlaytime(onlinePlayer.getUniqueId());
                String currentName = dataConfig.getString("players." + onlinePlayer.getUniqueId() + ".name");
                if (currentName == null || !currentName.equals(onlinePlayer.getName())) {
                    dataConfig.set("players." + onlinePlayer.getUniqueId() + ".name", onlinePlayer.getName());
                    plugin.markDataAsChanged();
                }
            }
        }, 20L * 60, 20L * 60);
    }

    public void cancelTasks() {
        if (saveTask != null) {
            saveTask.cancel();
        }
    }

    public void saveAllOnlinePlaytime() {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            savePlayerPlaytime(onlinePlayer.getUniqueId());
            String currentName = dataConfig.getString("players." + onlinePlayer.getUniqueId() + ".name");
            if (currentName == null || !currentName.equals(onlinePlayer.getName())) {
                dataConfig.set("players." + onlinePlayer.getUniqueId() + ".name", onlinePlayer.getName());
                plugin.markDataAsChanged();
            }
        }
    }

    private void savePlayerPlaytime(UUID uuid) {
        if (!playerJoinTimes.containsKey(uuid)) {
            return;
        }

        long joinTime = playerJoinTimes.get(uuid);
        long currentTime = System.currentTimeMillis();
        long sessionDurationSeconds = (currentTime - joinTime) / 1000;

        playerJoinTimes.put(uuid, currentTime);

        if (sessionDurationSeconds <= 0) return;

        long currentTotalPlaytime = dataConfig.getLong("players." + uuid + ".playtime.total", 0);
        dataConfig.set("players." + uuid + ".playtime.total", currentTotalPlaytime + sessionDurationSeconds);

        Date now = new Date();
        String today = dateFormat.format(now);
        String thisWeek = weekFormat.format(now);
        String thisMonth = monthFormat.format(now);

        long currentDailyPlaytime = dataConfig.getLong("players." + uuid + ".playtime.daily." + today, 0);
        dataConfig.set("players." + uuid + ".playtime.daily." + today, currentDailyPlaytime + sessionDurationSeconds);

        long currentWeeklyPlaytime = dataConfig.getLong("players." + uuid + ".playtime.weekly." + thisWeek, 0);
        dataConfig.set("players." + uuid + ".playtime.weekly." + thisWeek, currentWeeklyPlaytime + sessionDurationSeconds);

        long currentMonthlyPlaytime = dataConfig.getLong("players." + uuid + ".playtime.monthly." + thisMonth, 0);
        dataConfig.set("players." + uuid + ".playtime.monthly." + thisMonth, currentMonthlyPlaytime + sessionDurationSeconds);

        plugin.markDataAsChanged();
    }
}