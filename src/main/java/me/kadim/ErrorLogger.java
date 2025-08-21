package me.kadim;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class ErrorLogger {
    private final JavaPlugin plugin;
    private Logger logger;
    private FileHandler fileHandler;

    public ErrorLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        setupLogger();
    }

    private void setupLogger() {
        try {
            File logsFolder = new File(plugin.getDataFolder(), "LOG");
            if (!logsFolder.exists()) {
                logsFolder.mkdirs();
            }

            String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            File logFile = new File(logsFolder, "errors-" + date + ".log");

            fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.SEVERE);

            logger = Logger.getLogger(plugin.getName() + "-ErrorLogger");
            logger.addHandler(fileHandler);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create error log file: " + e.getMessage());
        }
    }

    public void logError(String error) {
        if (logger != null) {
            logger.severe(error);
        } else {
            plugin.getLogger().severe(error);
        }
    }

    public void close() {
        if (fileHandler != null) {
            fileHandler.close();
        }
    }
}