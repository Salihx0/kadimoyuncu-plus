package me.kadim;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /kopro client <oyuncuismi> komutunu işleyen sınıf
 */
public class ClientVersionCommand implements TabExecutor {
    private final KadimPlugin plugin;

    public ClientVersionCommand(KadimPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Yetki kontrolü
        if (!sender.hasPermission("kadimoyuncuplus.kopro.use")) {
            sender.sendMessage(plugin.renkCevir("&#FF5555Bu komutu kullanmak için gerekli yetkiye sahip değilsiniz: &#FFCC00kadimoyuncuplus.kopro.use"));
            return true;
        }

        // Komut kullanımı: /kopro client <oyuncuismi>
        if (args.length < 2 || !args[0].equalsIgnoreCase("client")) {
            sender.sendMessage(plugin.renkCevir("&#FFCC00Kullanım: &#FFBB00/kopro client <oyuncuismi>"));
            return true;
        }

        String playerName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        
        // Oyuncu kontrolü
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getMesaj("player-not-found"));
            return true;
        }

        // Oyuncunun client sürümünü data.yml'den al
        String clientVersion = plugin.getDataConfig().getString("players." + target.getUniqueId() + ".clientversion", "Bilinmiyor");
        
        // Sonucu gönder
        String message = plugin.renkCevir("&#00FF00Oyuncu: &#FFFFFF" + target.getName() + " &#00FF00- Client Sürümü: &#FFFF00" + clientVersion);
        sender.sendMessage(plugin.getRawMesaj("prefix") + message);
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("kadimoyuncuplus.kopro.use")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            // İlk argüman için "client" önerisi
            return Arrays.asList("client").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("client")) {
            // İkinci argüman için online oyuncu isimleri
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
