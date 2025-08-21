package me.kadim;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KadimPlugin extends JavaPlugin {

    public ErrorLogger errorLogger;
    public KillLogger killLogger;
    public BentoBoxLogger bentoBoxLogger; // YENİ
    private FileConfiguration dil;
    private File dilDosyasi;
    private File dataFile;
    private FileConfiguration dataConfig;
    private boolean dataChanged = false;
    private Map<String, String> cachedMessages;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat weekFormat = new SimpleDateFormat("yyyy-ww");
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("yyyy-MM");

    private PlaytimeTracker playtimeTracker;

    // Leaderboard Caches
    private volatile List<Map.Entry<UUID, Double>> cachedTopKD = Collections.emptyList();
    private volatile Map<String, List<Map.Entry<UUID, Double>>> cachedTopTimedKD = new ConcurrentHashMap<>();
    private volatile List<Map.Entry<UUID, Long>> cachedTopPlaytime = Collections.emptyList();
    private volatile Map<String, List<Map.Entry<UUID, Long>>> cachedTopTimedPlaytime = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        errorLogger = new ErrorLogger(this);
        killLogger = new KillLogger(this);
        bentoBoxLogger = new BentoBoxLogger(this); // YENİ
        cachedMessages = new ConcurrentHashMap<>();

        try {
            if (!setupPlaceholderAPI()) return;

            setupConfigurations();
            setupDataFile();

            playtimeTracker = new PlaytimeTracker(this);
            getServer().getPluginManager().registerEvents(playtimeTracker, this);
            playtimeTracker.startPlaytimeSaveTask();

            registerPlaceholders();
            registerCommandsAndListeners();
            setupBentoBoxIntegration();

            startAutoSaveTask();
            startLeaderboardCacheTask();

            String enabledMessage = getRawMesaj("plugin-enabled").replace("%version%", getDescription().getVersion());
            getLogger().info(ChatColor.stripColor(renkCevir(enabledMessage)));
        } catch (Exception e) {
            errorLogger.logError("Plugin enable error: " + e.getMessage());
            getLogger().log(Level.SEVERE, "Full stack trace for plugin enable error:", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (playtimeTracker != null) {
            playtimeTracker.saveAllOnlinePlaytime();
            playtimeTracker.cancelTasks();
        }
        saveDataFile();
        Bukkit.getScheduler().cancelTasks(this);
        if (errorLogger != null) {
            errorLogger.close();
        }
        if (killLogger != null) {
            killLogger.close();
        }
        if (bentoBoxLogger != null) { // YENİ
            bentoBoxLogger.close();
        }
    }

    private void setupConfigurations() {
        updateConfigurationFile("config.yml");
        updateConfigurationFile("dil.yml");
        
        reloadConfig();
        dilDosyasi = new File(getDataFolder(), "dil.yml");
        dil = YamlConfiguration.loadConfiguration(dilDosyasi);
        cachedMessages.clear();
    }

    private void updateConfigurationFile(String fileName) {
        File configFile = new File(getDataFolder(), fileName);
        if (!configFile.exists()) {
            saveResource(fileName, false);
            return; 
        }

        try {
            FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);
            InputStream defaultStream = this.getResource(fileName);
            if (defaultStream == null) return;

            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            boolean modified = false;

            for (String key : defaultConfig.getKeys(true)) {
                if (!userConfig.contains(key)) {
                    userConfig.set(key, defaultConfig.get(key));
                    modified = true;
                }
            }
            
            if (modified) {
                userConfig.save(configFile);
            }
        } catch (IOException e) {
            errorLogger.logError("Could not update configuration file " + fileName + ": " + e.getMessage());
        }
    }
    
    private void registerCommandsAndListeners() {
        getCommand("kd").setExecutor(new KDCommand());
        getCommand("kdo").setExecutor(new KDOCommand());
        getCommand("kplus").setExecutor(new KadimCommand());
        getCommand("kopro").setExecutor(new ClientVersionCommand(this));

        getServer().getPluginManager().registerEvents(new KillDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ClientVersionListener(this), this);
    }

    private void setupBentoBoxIntegration() {
        if (getServer().getPluginManager().getPlugin("BentoBox") != null) {
            if (getConfig().getBoolean("bento-box-integration.enabled", false)) {
                getServer().getPluginManager().registerEvents(new BentoBoxListener(this), this);
                getLogger().info("BentoBox entegrasyonu başarıyla aktif edildi.");
            } else {
                getLogger().info("BentoBox entegrasyonu config dosyasında devre dışı bırakılmış.");
            }
        } else {
            getLogger().info("BentoBox eklentisi bulunamadı, entegrasyon devre dışı.");
        }
    }

    public String getMesaj(String key) {
        if (cachedMessages.containsKey(key)) {
            return cachedMessages.get(key);
        }
        String prefix = renkCevir(dil.getString("prefix", ""));
        String mesaj = dil.getString(key, "");
        String fullMessage = prefix + renkCevir(mesaj);
        cachedMessages.put(key, fullMessage);
        return fullMessage;
    }

    public String getRawMesaj(String key) {
        return renkCevir(dil.getString(key, ""));
    }

    private boolean setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("PlaceholderAPI not found, disabling plugin.");
            return false;
        }
        return true;
    }

    private void setupDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                errorLogger.logError("Failed to create data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public FileConfiguration getDataConfig() {
        return dataConfig;
    }

    public void saveDataFile() {
        if (!dataChanged) return;
        try {
            dataConfig.save(dataFile);
            dataChanged = false;
        } catch (IOException e) {
            errorLogger.logError("Failed to save data.yml: " + e.getMessage());
        }
    }

    public void markDataAsChanged() {
        this.dataChanged = true;
    }

    private void startAutoSaveTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (dataChanged) {
                saveDataFile();
                if (getConfig().getBoolean("settings.log-auto-save", true)) {
                    getLogger().info("data.yml automatically saved.");
                }
            }
        }, 20L * 60 * 5, 20L * 60 * 5);
    }

    private void startLeaderboardCacheTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::updateLeaderboardCache, 20L * 60, 20L * 60);
    }

    private void updateLeaderboardCache() {
        cachedTopKD = calculateTopKDPlayers(14);
        cachedTopTimedKD.put("daily", calculateTopTimedKDPlayers("daily", 14));
        cachedTopTimedKD.put("weekly", calculateTopTimedKDPlayers("weekly", 14));
        cachedTopTimedKD.put("monthly", calculateTopTimedKDPlayers("monthly", 14));
        cachedTopPlaytime = calculateTopPlaytimePlayers("overall", 14);
        cachedTopTimedPlaytime.put("daily", calculateTopPlaytimePlayers("daily", 14));
        cachedTopTimedPlaytime.put("weekly", calculateTopPlaytimePlayers("weekly", 14));
        cachedTopTimedPlaytime.put("monthly", calculateTopPlaytimePlayers("monthly", 14));
    }

    private void registerPlaceholders() {
        new PlaceholderExpansion() {
            @Override
            public boolean canRegister() { return true; }
            @Override
            public String getIdentifier() { return "kplus"; }
            @Override
            public String getAuthor() { return "ByNeels"; }
            @Override
            public String getVersion() { return KadimPlugin.this.getDescription().getVersion(); }

            @Override
            public String onPlaceholderRequest(Player player, String identifier) {
                if (player == null) return "";
                UUID uuid = player.getUniqueId();
                String lowerIdentifier = identifier.toLowerCase();

                switch (lowerIdentifier) {
                    case "kd":
                        double kd = getPlayerKD(uuid);
                        int kills = dataConfig.getInt("players." + uuid + ".kills", 0);
                        int deaths = dataConfig.getInt("players." + uuid + ".deaths", 0);

                        String kdColor = getKdColor(uuid);
                        String kdRatioStr = String.format("%.2f", kd);
                        String kdBar = getProgressBar(uuid);
                        String kdStars = getStarRating(uuid);

                        return new StringBuilder()
                                .append(kdColor)
                                .append(kdRatioStr)
                                .append(" §8(§a")
                                .append(kills)
                                .append("§7/§c")
                                .append(deaths)
                                .append("§8) ")
                                .append(kdBar)
                                .append(" ")
                                .append(kdStars)
                                .toString();

                    case "kd1":
                        double kd_kd1 = getPlayerKD(uuid);
                        int kills_kd1 = dataConfig.getInt("players." + uuid + ".kills", 0);
                        int deaths_kd1 = dataConfig.getInt("players." + uuid + ".deaths", 0);

                        String kdColor_kd1 = getKdColor(uuid);
                        String kdRatioStr_kd1 = String.format("%.2f", kd_kd1);
                        String kdStars_kd1 = getStarRating(uuid);

                        return new StringBuilder()
                                .append(kdColor_kd1)
                                .append(kdRatioStr_kd1)
                                .append(" §8(§a")
                                .append(kills_kd1)
                                .append("§7/§c")
                                .append(deaths_kd1)
                                .append("§8) ")
                                .append(kdStars_kd1)
                                .toString();

                    case "kills": 
                        return String.valueOf(dataConfig.getInt("players." + uuid + ".kills", 0));
                    case "deaths": 
                        return String.valueOf(dataConfig.getInt("players." + uuid + ".deaths", 0));
                    case "world":
                        String worldName = player.getWorld().getName();
                        if (worldName == null || worldName.isEmpty()) {
                            return "";
                        }
                        return worldName.substring(0, 1).toUpperCase() + worldName.substring(1);
                    case "kd_level_bar": 
                        return getProgressBar(uuid);
                    case "kd_stars": 
                        return getStarRating(uuid);
                    case "kd_color": 
                        return getKdColor(uuid);
                    case "playtime_total": 
                        return formatPlaytime(dataConfig.getLong("players." + uuid + ".playtime.total", 0));
                    case "playtime_daily": 
                        return formatPlaytime(dataConfig.getLong("players." + uuid + ".playtime.daily." + dateFormat.format(new Date()), 0));
                    case "playtime_weekly": 
                        return formatPlaytime(dataConfig.getLong("players." + uuid + ".playtime.weekly." + weekFormat.format(new Date()), 0));
                    case "playtime_monthly": 
                        return formatPlaytime(dataConfig.getLong("players." + uuid + ".playtime.monthly." + monthFormat.format(new Date()), 0));
                    default: 
                        return null;
                }
            }
        }.register();
    }
    
    public double getPlayerKD(UUID uuid) {
        int kills = dataConfig.getInt("players." + uuid.toString() + ".kills", 0);
        int deaths = dataConfig.getInt("players." + uuid.toString() + ".deaths", 1);
        if (deaths == 0) {
            return (double) kills;
        }
        return (double) kills / deaths;
    }

    public String formatPlaytime(long totalSeconds) {
        if (totalSeconds < 0) return "0s";
        long days = TimeUnit.SECONDS.toDays(totalSeconds);
        long hours = TimeUnit.SECONDS.toHours(totalSeconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("g ");
        if (hours > 0) sb.append(hours).append("s ");
        if (minutes > 0) sb.append(minutes).append("d ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        
        return sb.toString().trim();
    }

    public String getKdColor(UUID uuid) {
        double kd = getPlayerKD(uuid);
        ConfigurationSection barConfig = getConfig().getConfigurationSection("kd-progress-bar");
        if (barConfig == null) return "§a";

        List<Map<?, ?>> gradientConfig = barConfig.getMapList("gradient");
        if (gradientConfig == null || gradientConfig.isEmpty()) return "§a";

        double percent = Math.min(1.0, kd / 5.0);
        
        String finalColor = "§f";
        for (Map<?, ?> step : gradientConfig) {
            if (step.containsKey("threshold") && step.containsKey("color")) {
                double threshold = ((Number) step.get("threshold")).doubleValue();
                if (percent >= threshold) {
                    finalColor = ChatColor.valueOf(String.valueOf(step.get("color")).toUpperCase()).toString();
                }
            }
        }
        return finalColor;
    }
    
    public String getProgressBar(UUID uuid) {
        ConfigurationSection barConfig = getConfig().getConfigurationSection("kd-progress-bar");
        if (barConfig == null) return "";

        double kd = getPlayerKD(uuid);
        double maxKdForBar = 5.0; 
        int totalBars = barConfig.getInt("total-bars", 10);
        char filledChar = barConfig.getString("filled-char", "█").charAt(0);
        char emptyChar = barConfig.getString("empty-char", "░").charAt(0);
        
        ChatColor emptyColor = ChatColor.valueOf(barConfig.getString("empty-color", "GRAY").toUpperCase());

        List<Map<?, ?>> gradientConfig = barConfig.getMapList("gradient");
        return getProgressBar(kd, maxKdForBar, totalBars, filledChar, emptyChar, emptyColor, gradientConfig);
    }
    
    public String getProgressBar(double currentValue, double maxValue, int totalBars, char filledChar, char emptyChar, ChatColor emptyColor, List<Map<?, ?>> gradientConfig) {
        if (gradientConfig == null || gradientConfig.isEmpty()) return "";

        double percent = Math.min(1.0, currentValue / maxValue);
        int progressBars = (int) (totalBars * percent);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < totalBars; i++) {
            if (i < progressBars) {
                double currentBarPercent = (double) i / totalBars;
                ChatColor barColor = ChatColor.WHITE;

                for (Map<?, ?> step : gradientConfig) {
                    if (step.containsKey("threshold") && step.containsKey("color")) {
                        double threshold = ((Number) step.get("threshold")).doubleValue();
                        if (currentBarPercent >= threshold) {
                            barColor = ChatColor.valueOf(String.valueOf(step.get("color")).toUpperCase());
                        }
                    }
                }
                sb.append(barColor).append(filledChar);
            } else {
                sb.append(emptyColor).append(emptyChar);
            }
        }
        return sb.toString();
    }

    public String getStarRating(UUID uuid) {
        ConfigurationSection barConfig = getConfig().getConfigurationSection("kd-progress-bar");
        List<Map<?, ?>> gradientConfig = (barConfig != null) ? barConfig.getMapList("gradient") : new ArrayList<>();
        
        double kd = getPlayerKD(uuid);
        double maxKdForRating = 5.0;
        int maxStars = 5;
        
        double rating = Math.min(maxStars, (kd / maxKdForRating) * maxStars);

        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < maxStars; i++) {
            if (i < rating) {
                double currentStarPercent = (double) i / maxStars;
                ChatColor starColor = ChatColor.GOLD; 

                if (!gradientConfig.isEmpty()) {
                    for (Map<?, ?> step : gradientConfig) {
                        if (step.containsKey("threshold") && step.containsKey("color")) {
                            double threshold = ((Number) step.get("threshold")).doubleValue();
                            if (currentStarPercent >= threshold) {
                                starColor = ChatColor.valueOf(String.valueOf(step.get("color")).toUpperCase());
                            }
                        }
                    }
                }
                stars.append(starColor).append("★");
            } else {
                stars.append(ChatColor.GRAY).append("☆");
            }
        }
        return stars.toString();
    }
    
    public void openTopKdGUI(Player player, String periodType) {
        List<Map.Entry<UUID, Double>> topPlayers;
        String guiTitleKey;

        switch (periodType.toLowerCase()) {
            case "daily": topPlayers = cachedTopTimedKD.getOrDefault("daily", Collections.emptyList()); guiTitleKey = "top-daily-kd-header-gui"; break;
            case "weekly": topPlayers = cachedTopTimedKD.getOrDefault("weekly", Collections.emptyList()); guiTitleKey = "top-weekly-kd-header-gui"; break;
            case "monthly": topPlayers = cachedTopTimedKD.getOrDefault("monthly", Collections.emptyList()); guiTitleKey = "top-monthly-kd-header-gui"; break;
            default: topPlayers = cachedTopKD; guiTitleKey = "top-kd-header-gui"; break;
        }

        String guiTitle = getRawMesaj(guiTitleKey);
        Inventory gui = Bukkit.createInventory(null, 54, guiTitle);

        if (topPlayers.isEmpty()) {
            ItemStack noData = new ItemStack(Material.BARRIER);
            ItemMeta meta = noData.getItemMeta();
            meta.setDisplayName(getRawMesaj("no-top-players"));
            noData.setItemMeta(meta);
            gui.setItem(22, noData);
        } else {
            int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
            for (int i = 0; i < Math.min(topPlayers.size(), slots.length); i++) {
                Map.Entry<UUID, Double> entry = topPlayers.get(i);
                OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                String playerName = op.getName() != null ? op.getName() : "Bilinmeyen";
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwningPlayer(op);
                meta.setDisplayName(renkCevir("&#FFD700#" + (i + 1) + " " + playerName));
                meta.setLore(Arrays.asList(
                    renkCevir("&#00FF00KD Oranı: &#FFFFFF" + String.format("%.2f", entry.getValue())),
                    renkCevir("&#FFFFFFÖldürme: &#aaffaa" + dataConfig.getInt("players." + entry.getKey() + ".kills", 0)),
                    renkCevir("&#FFFFFFÖlme: &#ffaaaa" + dataConfig.getInt("players." + entry.getKey() + ".deaths", 0))
                ));
                skull.setItemMeta(meta);
                gui.setItem(slots[i], skull);
            }
        }
        player.openInventory(gui);
    }
    
    private List<Map.Entry<UUID, Double>> calculateTopKDPlayers(int limit) {
        Map<UUID, Double> playerKDs = new HashMap<>();
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection == null) return Collections.emptyList();
        for (String uuidStr : playersSection.getKeys(false)) {
            playerKDs.put(UUID.fromString(uuidStr), getPlayerKD(UUID.fromString(uuidStr)));
        }
        return playerKDs.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<Map.Entry<UUID, Double>> calculateTopTimedKDPlayers(String periodType, int limit) {
        Map<UUID, Double> playerKDs = new HashMap<>();
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection == null) return Collections.emptyList();
        String currentPeriod;
        Date now = new Date();
        switch (periodType.toLowerCase()) {
            case "daily": currentPeriod = dateFormat.format(now); break;
            case "weekly": currentPeriod = weekFormat.format(now); break;
            case "monthly": currentPeriod = monthFormat.format(now); break;
            default: return Collections.emptyList();
        }
        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            String path = "players." + uuidStr + "." + periodType + "." + currentPeriod;
            int kills = dataConfig.getInt(path + ".kills", 0);
            int deaths = dataConfig.getInt(path + ".deaths", 1);
            playerKDs.put(uuid, (deaths == 0) ? (double) kills : (double) kills / deaths);
        }
        return playerKDs.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<Map.Entry<UUID, Long>> calculateTopPlaytimePlayers(String periodType, int limit) {
        Map<UUID, Long> playerPlaytimes = new HashMap<>();
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection == null) return Collections.emptyList();
        String currentPeriod = null;
        Date now = new Date();
        switch (periodType.toLowerCase()) {
            case "daily": currentPeriod = dateFormat.format(now); break;
            case "weekly": currentPeriod = weekFormat.format(now); break;
            case "monthly": currentPeriod = monthFormat.format(now); break;
            case "overall": break;
            default: return Collections.emptyList();
        }
        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            long playtime;
            if ("overall".equalsIgnoreCase(periodType)) {
                playtime = dataConfig.getLong("players." + uuidStr + ".playtime.total", 0);
            } else {
                playtime = dataConfig.getLong("players." + uuidStr + ".playtime." + periodType + "." + currentPeriod, 0);
            }
            playerPlaytimes.put(uuid, playtime);
        }
        return playerPlaytimes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private class KDCommand implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("kadimoyuncuplus.kd.use")) {
                sender.sendMessage(getMesaj("no-permission-kd"));
                return true;
            }
            OfflinePlayer targetPlayer;
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(renkCevir("&#FF0000Konsol bir oyuncu belirtmelidir: &#FFBB00/kd <oyuncu>"));
                    return true;
                }
                targetPlayer = (Player) sender;
            } else {
                targetPlayer = Bukkit.getOfflinePlayer(args[0]);
                if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                    sender.sendMessage(getMesaj("player-not-found"));
                    return true;
                }
            }
            
            String detailedStats = PlaceholderAPI.setPlaceholders(targetPlayer, "%kplus_kd%");
            
            String messageKey = (sender instanceof Player && ((Player) sender).getUniqueId().equals(targetPlayer.getUniqueId())) ? "kd-self" : "kd-other";
            String messageTemplate = getRawMesaj(messageKey)
                    .replace("%player%", targetPlayer.getName())
                    .replace("%kplus_kd%", detailedStats);

            sender.sendMessage(getRawMesaj("prefix") + messageTemplate);
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }

    private class KDOCommand implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("kadimoyuncuplus.kdo.use")) { sender.sendMessage(getMesaj("no-permission-kdo")); return true; }
            if (args.length == 0) { sender.sendMessage(getMesaj("kdo-usage")); return true; }

            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "top":
                    if (!sender.hasPermission("kadimoyuncuplus.kdo.top")) { sender.sendMessage(getMesaj("no-permission-kdo-top")); return true; }
                    displayTopKD(sender, args);
                    break;
                case "reset":
                    if (!sender.hasPermission("kadimoyuncuplus.kdo.reset")) { sender.sendMessage(getMesaj("no-permission-kdo-reset")); return true; }
                    handleResetCommand(sender, args);
                    break;
                case "gui":
                    if (!(sender instanceof Player)) { sender.sendMessage(renkCevir("&#FF0000Bu komutu sadece oyuncular kullanabilir!")); return true; }
                    if (!sender.hasPermission("kadimoyuncuplus.kdo.gui")) { sender.sendMessage(getMesaj("no-permission-kdo-gui")); return true; }
                    openTopKdGUI((Player) sender, (args.length >= 2) ? args[1] : "overall");
                    break;
                case "playtime":
                    if (!sender.hasPermission("kadimoyuncuplus.kdo.playtime")) { sender.sendMessage(getMesaj("no-permission-kdo-playtime")); return true; }
                    displayTopPlaytime(sender, args);
                    break;
                default:
                    sender.sendMessage(getMesaj("kdo-usage"));
                    break;
            }
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
             if (args.length == 1) {
                List<String> completions = new ArrayList<>(Arrays.asList("top", "reset", "gui", "playtime"));
                completions.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
                return completions;
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("reset") && sender.hasPermission("kadimoyuncuplus.kdo.reset")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                } else if (Arrays.asList("top", "gui", "playtime").contains(args[0].toLowerCase())) {
                    return Arrays.asList("overall", "daily", "weekly", "monthly").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
            }
            return Collections.emptyList();
        }

        private void displayTopKD(CommandSender sender, String[] args) {
            String periodType = (args.length >= 2) ? args[1].toLowerCase() : "overall";
            List<Map.Entry<UUID, Double>> topPlayers;
            String headerKey;
            switch (periodType) {
                case "daily": topPlayers = cachedTopTimedKD.getOrDefault("daily", Collections.emptyList()); headerKey = "top-daily-kd-header"; break;
                case "weekly": topPlayers = cachedTopTimedKD.getOrDefault("weekly", Collections.emptyList()); headerKey = "top-weekly-kd-header"; break;
                case "monthly": topPlayers = cachedTopTimedKD.getOrDefault("monthly", Collections.emptyList()); headerKey = "top-monthly-kd-header"; break;
                default: topPlayers = cachedTopKD; headerKey = "top-kd-header"; break;
            }
            sender.sendMessage(getMesaj(headerKey));
            if (topPlayers.isEmpty()) { sender.sendMessage(getMesaj("no-top-players")); return; }
            int position = 1;
            for (Map.Entry<UUID, Double> entry : topPlayers) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                sender.sendMessage(PlaceholderAPI.setPlaceholders(op, getRawMesaj("top-kd-entry").replace("%position%", String.valueOf(position++)).replace("%player%", op.getName())));
            }
        }
        private void displayTopPlaytime(CommandSender sender, String[] args) {
            String periodType = (args.length >= 2) ? args[1].toLowerCase() : "overall";
            List<Map.Entry<UUID, Long>> topPlayers;
            String headerKey;
            switch (periodType) {
                case "daily": topPlayers = cachedTopTimedPlaytime.getOrDefault("daily", Collections.emptyList()); headerKey = "top-daily-playtime-header"; break;
                case "weekly": topPlayers = cachedTopTimedPlaytime.getOrDefault("weekly", Collections.emptyList()); headerKey = "top-weekly-playtime-header"; break;
                case "monthly": topPlayers = cachedTopTimedPlaytime.getOrDefault("monthly", Collections.emptyList()); headerKey = "top-monthly-playtime-header"; break;
                default: topPlayers = cachedTopPlaytime; headerKey = "top-playtime-header"; break;
            }
            sender.sendMessage(getMesaj(headerKey));
            if (topPlayers.isEmpty()) { sender.sendMessage(getMesaj("no-playtime-data")); return; }
            int position = 1;
            for (Map.Entry<UUID, Long> entry : topPlayers) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                String playerName = op.getName() != null ? op.getName() : "Bilinmeyen";
                String formattedTime = formatPlaytime(entry.getValue());
                sender.sendMessage(getRawMesaj("top-playtime-entry")
                        .replace("%position%", String.valueOf(position++))
                        .replace("%player%", playerName)
                        .replace("%kplus_playtime_total%", formattedTime));
            }
        }
        private void handleResetCommand(CommandSender sender, String[] args) {
            if (args.length < 2) { sender.sendMessage(getMesaj("kdo-usage")); return; }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) { sender.sendMessage(getMesaj("player-not-found")); return; }
            dataConfig.set("players." + target.getUniqueId().toString(), null);
            markDataAsChanged();
            saveDataFile();
            updateLeaderboardCache();
            sender.sendMessage(getMesaj("kdo-reset-success").replace("%player%", target.getName()));
        }
    }

    private class KadimCommand implements TabExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("kadimoyuncuplus.admin")) {
                    sender.sendMessage(getMesaj("no-permission-reload"));
                    return true;
                }
                setupConfigurations();
                updateLeaderboardCache();
                sender.sendMessage(getMesaj("reloaded"));
                return true;
            }
            sender.sendMessage(getMesaj("info").replace("%version%", getDescription().getVersion()));
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1 && "reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("kadimoyuncuplus.admin")) {
                return Collections.singletonList("reload");
            }
            return Collections.emptyList();
        }
    }
    
    public String renkCevir(String mesaj) {
        if (mesaj == null) return "";
        Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(mesaj);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "" + net.md_5.bungee.api.ChatColor.of(matcher.group().substring(1)));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}
