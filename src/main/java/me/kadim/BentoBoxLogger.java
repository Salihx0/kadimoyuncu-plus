package me.kadim;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class BentoBoxLogger {
    private final JavaPlugin plugin;
    private Logger logger;
    private FileHandler fileHandler;

    public BentoBoxLogger(JavaPlugin plugin) {
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
            File logFile = new File(logsFolder, "bento-integration-" + date + ".log");

            fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            fileHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return new SimpleDateFormat("HH:mm:ss").format(new Date(record.getMillis()))
                            + " | " + ChatColor.stripColor(formatMessage(record)) + "\n";
                }
            });
            fileHandler.setLevel(Level.INFO);

            logger = Logger.getLogger(plugin.getName() + "-BentoBoxLogger");
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create bento-integration log file: " + e.getMessage());
        }
    }

    public void log(String logMessage) {
        if (logger != null) {
            logger.info(logMessage);
        } else {
            plugin.getLogger().warning("BentoBoxLogger not initialized. Logging to console: " + logMessage);
        }
    }

    public void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}
