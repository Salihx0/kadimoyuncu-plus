package me.kadim;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.ChatColor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class KillDeathListener implements Listener {
    private final KadimPlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat weekFormat = new SimpleDateFormat("yyyy-ww");
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");

    public KillDeathListener(KadimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            Player dead = event.getEntity();
            Player killer = dead.getKiller();

            UUID deadUUID = dead.getUniqueId();
            Date now = new Date();
            String today = dateFormat.format(now);
            String thisWeek = weekFormat.format(now);
            String thisMonth = monthFormat.format(now);

            // Ölen oyuncunun istatistiklerini güncelle
            int oldDeaths = plugin.getDataConfig().getInt("players." + deadUUID + ".deaths", 0);
            plugin.getDataConfig().set("players." + deadUUID + ".deaths", oldDeaths + 1);

            plugin.getDataConfig().set("players." + deadUUID + ".daily." + today + ".deaths",
                    plugin.getDataConfig().getInt("players." + deadUUID + ".daily." + today + ".deaths", 0) + 1);
            plugin.getDataConfig().set("players." + deadUUID + ".weekly." + thisWeek + ".deaths",
                    plugin.getDataConfig().getInt("players." + deadUUID + ".weekly." + thisWeek + ".deaths", 0) + 1);
            plugin.getDataConfig().set("players." + deadUUID + ".monthly." + thisMonth + ".deaths",
                    plugin.getDataConfig().getInt("players." + deadUUID + ".monthly." + thisMonth + ".deaths", 0) + 1);
            plugin.markDataAsChanged();

            // Gelişmiş Ölüm Logu için mesajı hazırla
            String deathCause = dead.getLastDamageCause() != null ? dead.getLastDamageCause().getCause().name() : "UNKNOWN";
            String deadKD = String.format("%.2f", plugin.getPlayerKD(deadUUID));
            String deadInventory = getPlayerInventoryAsString(dead);
            
            String deathLog = plugin.getRawMesaj("advanced-log-death")
                    .replace("%dead_player%", dead.getName())
                    .replace("%dead_kd_score%", deadKD)
                    .replace("%death_cause%", deathCause)
                    .replace("%last_damage_source%", deathCause) // Bu satırda bir hata olabilir, genellikle aynıdır.
                    .replace("%dead_inventory%", deadInventory);
            
            // --- YENİ LOGLAMA SİSTEMİ ---
            // 1. Dosyaya her zaman kaydet
            plugin.killLogger.logKillDeath(deathLog);
            
            // 2. Ayar açıksa konsola yazdır
            if (plugin.getConfig().getBoolean("settings.log-kills-to-console", true)) {
                plugin.getLogger().info(ChatColor.stripColor(plugin.renkCevir(deathLog)));
            }
            // --- BİTTİ ---


            if (killer != null && killer != dead) {
                UUID killerUUID = killer.getUniqueId();
                // Öldüren oyuncunun istatistiklerini güncelle
                plugin.getDataConfig().set("players." + killerUUID + ".kills", 
                        plugin.getDataConfig().getInt("players." + killerUUID + ".kills", 0) + 1);

                plugin.getDataConfig().set("players." + killerUUID + ".daily." + today + ".kills",
                        plugin.getDataConfig().getInt("players." + killerUUID + ".daily." + today + ".kills", 0) + 1);
                plugin.getDataConfig().set("players." + killerUUID + ".weekly." + thisWeek + ".kills",
                        plugin.getDataConfig().getInt("players." + killerUUID + ".weekly." + thisWeek + ".kills", 0) + 1);
                plugin.getDataConfig().set("players." + killerUUID + ".monthly." + thisMonth + ".kills",
                        plugin.getDataConfig().getInt("players." + killerUUID + ".monthly." + thisMonth + ".kills", 0) + 1);
                plugin.markDataAsChanged();

                // Gelişmiş Kill Logu için mesajı hazırla
                String killerWeapon = "YOK";
                ItemStack mainHand = killer.getInventory().getItemInMainHand();
                if (mainHand.getType() != Material.AIR) {
                    killerWeapon = mainHand.hasItemMeta() && mainHand.getItemMeta().hasDisplayName() ?
                                   mainHand.getItemMeta().getDisplayName() + " (" + mainHand.getType().name() + ")" :
                                   mainHand.getType().name();
                }
                String killerInventory = getPlayerInventoryAsString(killer);
                String killerKD = String.format("%.2f", plugin.getPlayerKD(killerUUID));

                String killLog = plugin.getRawMesaj("advanced-log-kill")
                        .replace("%killer_player%", killer.getName())
                        .replace("%killer_kd_score%", killerKD)
                        .replace("%dead_player%", dead.getName())
                        .replace("%dead_kd_score%", deadKD)
                        .replace("%killer_weapon%", killerWeapon)
                        .replace("%killer_inventory%", killerInventory);
                
                // --- YENİ LOGLAMA SİSTEMİ ---
                // 1. Dosyaya her zaman kaydet
                plugin.killLogger.logKillDeath(killLog);

                // 2. Ayar açıksa konsola yazdır
                if (plugin.getConfig().getBoolean("settings.log-kills-to-console", true)) {
                    plugin.getLogger().info(ChatColor.stripColor(plugin.renkCevir(killLog)));
                }
                // --- BİTTİ ---
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error in KillDeathListener: " + e.getMessage());
            plugin.errorLogger.logError("Error in KillDeathListener: " + e.getMessage());
        }
    }

    private String getPlayerInventoryAsString(Player player) {
        PlayerInventory inv = player.getInventory();
        StringBuilder sb = new StringBuilder();
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                sb.append(item.getType().name()).append("x").append(item.getAmount()).append(", ");
            }
        }
        return sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "BOS";
    }
}