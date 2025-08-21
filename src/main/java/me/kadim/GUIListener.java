package me.kadim;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final KadimPlugin plugin;

    public GUIListener(KadimPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String clickedInventoryTitle = event.getView().getTitle();

        String topKdHeaderGui = plugin.getRawMesaj("top-kd-header-gui");
        String topDailyKdHeaderGui = plugin.getRawMesaj("top-daily-kd-header-gui");
        String topWeeklyKdHeaderGui = plugin.getRawMesaj("top-weekly-kd-header-gui");
        String topMonthlyKdHeaderGui = plugin.getRawMesaj("top-monthly-kd-header-gui");
        String topPlaytimeHeaderGui = plugin.getRawMesaj("top-playtime-header-gui");
        String topDailyPlaytimeHeaderGui = plugin.getRawMesaj("top-daily-playtime-header-gui");
        String topWeeklyPlaytimeHeaderGui = plugin.getRawMesaj("top-weekly-playtime-header-gui");
        String topMonthlyPlaytimeHeaderGui = plugin.getRawMesaj("top-monthly-playtime-header-gui");

        if (clickedInventoryTitle.equals(topKdHeaderGui) ||
            clickedInventoryTitle.equals(topDailyKdHeaderGui) ||
            clickedInventoryTitle.equals(topWeeklyKdHeaderGui) ||
            clickedInventoryTitle.equals(topMonthlyKdHeaderGui) ||
            clickedInventoryTitle.equals(topPlaytimeHeaderGui) ||
            clickedInventoryTitle.equals(topDailyPlaytimeHeaderGui) ||
            clickedInventoryTitle.equals(topWeeklyPlaytimeHeaderGui) ||
            clickedInventoryTitle.equals(topMonthlyPlaytimeHeaderGui)) {
            
            event.setCancelled(true);
        }
    }
}