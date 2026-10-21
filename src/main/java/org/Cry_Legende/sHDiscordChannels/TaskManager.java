package org.Cry_Legende.sHDiscordChannels;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    private final SHDiscordChannels plugin;
    private final DiscordManager discordManager;
    private final List<BukkitTask> activeTasks = new ArrayList<>();

    public TaskManager(SHDiscordChannels plugin, DiscordManager discordManager) {
        this.plugin = plugin;
        this.discordManager = discordManager;
    }

    public void loadTasks() {
        ConfigurationSection channelsSection = plugin.getConfig().getConfigurationSection("channels");
        if (channelsSection == null) return;

        for (String key : channelsSection.getKeys(false)) {
            String id = channelsSection.getString(key + ".id");
            String textTemplate = channelsSection.getString(key + ".text");
            int interval = channelsSection.getInt(key + ".update_interval", 600); // 10 min par défaut

            if (id != null && textTemplate != null) {
                scheduleChannelUpdate(id, textTemplate, interval);
            }
        }
    }

    private void scheduleChannelUpdate(String channelId, String textTemplate, int intervalSeconds) {
        // On utilise runTaskTimer (Sync) car PlaceholderAPI n'est pas thread-safe pour tous les placeholders
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            // 1. Récupération des données (SYNC - Main Thread)
            // On passe null comme joueur car ce sont généralement des stats globales serveur
            String parsedText = PlaceholderAPI.setPlaceholders(null, textTemplate);

            // 2. Envoi à Discord (ASYNC - Worker Thread)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                discordManager.updateVoiceChannelName(channelId, parsedText);
            });

        }, 20L * 10, 20L * intervalSeconds); // Premier délai 10s pour laisser le temps au serveur de boot

        activeTasks.add(task);
    }

    public void stopTasks() {
        for (BukkitTask task : activeTasks) {
            task.cancel();
        }
        activeTasks.clear();
    }
}