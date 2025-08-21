package me.kadim;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Oyuncuların Minecraft client sürümünü tespit eden ve sürüme göre komutları çalıştıran listener
 */
public class ClientVersionListener implements Listener {
    private final KadimPlugin plugin;
    private boolean clientDetectorPlusWarningShown = false;

    public ClientVersionListener(KadimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String detectedVersion = detectClientVersion(player);
        
        // Tespit edilen sürümü data.yml'ye kaydet
        plugin.getDataConfig().set("players." + player.getUniqueId() + ".clientversion", detectedVersion);
        plugin.markDataAsChanged();
        
        // Tespit edilen sürüme göre join komutunu çalıştır
        String command = plugin.getConfig().getString("client-version-commands." + detectedVersion + ".join");
        if (command != null && !command.isEmpty()) {
            command = command.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String version = plugin.getDataConfig().getString("players." + player.getUniqueId() + ".clientversion", "Bilinmiyor");
        
        // Oyuncunun sürümüne göre quit komutunu çalıştır
        String command = plugin.getConfig().getString("client-version-commands." + version + ".quit");
        if (command != null && !command.isEmpty()) {
            command = command.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }

    /**
     * Oyuncunun Minecraft client sürümünü tespit eder
     * Önce kendi reflection yöntemini dener, başarısız olursa ClientDetectorPlus'ı dener
     */
    private String detectClientVersion(Player player) {
        // Birincil tespit yöntemi: Reflection kullanarak
        String version = detectVersionWithReflection(player);
        if (version != null && !version.equals("Bilinmiyor")) {
            return version;
        }

        // İkincil tespit yöntemi: ClientDetectorPlus eklentisi
        version = detectVersionWithClientDetectorPlus(player);
        if (version != null && !version.equals("Bilinmiyor")) {
            return version;
        }

        // Her iki yöntem de başarısız oldu
        if (!clientDetectorPlusWarningShown) {
            plugin.getLogger().warning("=================================================");
            plugin.getLogger().warning("ClientDetectorPlus bağlantısı yapılamadı,");
            plugin.getLogger().warning("sürüm tespiti daha az başarıyla sonuçlanacaktır.");
            plugin.getLogger().warning("=================================================");
            clientDetectorPlusWarningShown = true;
        }
        
        return "Bilinmiyor";
    }

    /**
     * Reflection kullanarak client sürümünü tespit etmeye çalışır
     */
    private String detectVersionWithReflection(Player player) {
        try {
            // CraftPlayer handle'ını al
            Method getHandle = player.getClass().getMethod("getHandle");
            Object entityPlayer = getHandle.invoke(player);
            
            // PlayerConnection field'ına eriş
            Field connectionField = null;
            try {
                connectionField = entityPlayer.getClass().getField("playerConnection");
            } catch (NoSuchFieldException e) {
                // 1.17+ için farklı field adı deneyebiliriz
                connectionField = entityPlayer.getClass().getField("connection");
            }
            Object playerConnection = connectionField.get(entityPlayer);
            
            // NetworkManager field'ına eriş
            Field networkField = null;
            try {
                networkField = playerConnection.getClass().getField("networkManager");
            } catch (NoSuchFieldException e) {
                // Alternatif field adları
                try {
                    networkField = playerConnection.getClass().getField("connection");
                } catch (NoSuchFieldException e2) {
                    networkField = playerConnection.getClass().getField("network");
                }
            }
            Object networkManager = networkField.get(playerConnection);
            
            // Protocol version'ı almaya çalış
            try {
                Field versionField = networkManager.getClass().getField("protocolVersion");
                Object protocolVersion = versionField.get(networkManager);
                return convertProtocolVersionToMinecraftVersion(protocolVersion.toString());
            } catch (NoSuchFieldException e) {
                // Alternatif field adları
                try {
                    Field versionField = networkManager.getClass().getField("version");
                    Object version = versionField.get(networkManager);
                    return convertProtocolVersionToMinecraftVersion(version.toString());
                } catch (NoSuchFieldException e2) {
                    // Son deneme
                    Method getVersionMethod = networkManager.getClass().getMethod("getVersion");
                    Object version = getVersionMethod.invoke(networkManager);
                    return convertProtocolVersionToMinecraftVersion(version.toString());
                }
            }
            
        } catch (Exception e) {
            // Reflection başarısız oldu, debug için log
            plugin.getLogger().fine("Reflection ile sürüm tespiti başarısız: " + e.getMessage());
        }
        
        return "Bilinmiyor";
    }

    /**
     * ClientDetectorPlus eklentisi kullanarak sürüm tespiti yapar
     */
    private String detectVersionWithClientDetectorPlus(Player player) {
        Plugin cdp = Bukkit.getPluginManager().getPlugin("ClientDetectorPlus");
        if (cdp != null && cdp.isEnabled()) {
            try {
                // ClientDetectorPlus API'sini kullan
                Class<?> apiClass = Class.forName("com.clientdetectorplus.api.ClientDetectorPlusAPI");
                Method getClientVersion = apiClass.getMethod("getClientVersion", Player.class);
                Object detected = getClientVersion.invoke(null, player);
                if (detected != null) {
                    return detected.toString();
                }
            } catch (Exception e) {
                plugin.getLogger().fine("ClientDetectorPlus API çağrısı başarısız: " + e.getMessage());
            }
        }
        return "Bilinmiyor";
    }

    /**
     * Protocol version numarasını Minecraft sürümüne çevirir
     */
    private String convertProtocolVersionToMinecraftVersion(String protocolVersion) {
        try {
            int protocol = Integer.parseInt(protocolVersion);
            
            // Minecraft sürüm mapping'i (güncel sürümler için)
            switch (protocol) {
                case 767: return "1.21.1";
                case 768: return "1.21.2";
                case 769: return "1.21.3";
                case 770: return "1.21.4";
                case 771: return "1.21.5";
                case 772: return "1.21.6";
                case 773: return "1.21.7";
                case 774: return "1.21.8";
                // Eski sürümler
                case 766: return "1.20.6";
                case 765: return "1.20.4";
                case 764: return "1.20.2";
                case 763: return "1.20.1";
                case 762: return "1.19.4";
                case 761: return "1.19.3";
                case 760: return "1.19.2";
                case 759: return "1.19";
                case 758: return "1.18.2";
                case 757: return "1.18";
                default:
                    // Bilinmeyen protocol version
                    plugin.getLogger().info("Bilinmeyen protocol version: " + protocol);
                    return "Bilinmiyor";
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().fine("Protocol version parse edilemedi: " + protocolVersion);
            return "Bilinmiyor";
        }
    }
}
