package me.kadim;

import org.bukkit.ChatColor;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class KillLogger {
    private final KadimPlugin plugin;
    private Logger logger;
    private FileHandler fileHandler; // Dosya yöneticisini tutmak için değişken eklendi

    public KillLogger(KadimPlugin plugin) {
        this.plugin = plugin;
        setupLogger();
    }

    private void setupLogger() {
        try {
            File logsFolder = new File(plugin.getDataFolder(), "LOGS");
            if (!logsFolder.exists()) {
                logsFolder.mkdirs();
            }

            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File logFile = new File(logsFolder, "kills-deaths-" + date + ".log");

            // fileHandler nesnesini değişkene ata
            fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            fileHandler.setFormatter(new SimpleFormatter() {
                 @Override
                 public String format(LogRecord record) {
                     // Log formatını daha temiz hale getirelim
                     return new SimpleDateFormat("HH:mm:ss").format(new Date(record.getMillis())) 
                            + " | " + ChatColor.stripColor(formatMessage(record)) + "\n";
                 }
            });
            fileHandler.setLevel(Level.INFO);

            logger = Logger.getLogger(plugin.getName() + "-KillLogger");
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create kill log file: " + e.getMessage());
        }
    }

    public void logKillDeath(String logMessage) {
        if (logger != null) {
            // Renk kodları zaten formatlayıcıda temizleniyor, burada tekrar temizlemeye gerek yok.
            logger.info(plugin.renkCevir(logMessage));
        } else {
            plugin.getLogger().warning("KillLogger not initialized. Logging to console: " + logMessage);
        }
    }

    /**
     * Sunucu kapanırken log dosyasını güvenli bir şekilde kapatır.
     */
    public void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}