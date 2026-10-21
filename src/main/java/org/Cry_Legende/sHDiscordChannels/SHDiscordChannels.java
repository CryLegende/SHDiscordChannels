package org.Cry_Legende.sHDiscordChannels;

import org.bukkit.plugin.java.JavaPlugin;

public final class SHDiscordChannels extends JavaPlugin {

    private static SHDiscordChannels instance;
    private DiscordManager discordManager;
    private TaskManager taskManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Vérification de PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI introuvable ! Désactivation du plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialisation de JDA
        String token = getConfig().getString("discord.token");
        if (token == null || token.equals("VOTRE_TOKEN_BOT_ICI")) {
            getLogger().warning("Veuillez configurer le token du bot dans config.yml !");
            return;
        }

        getLogger().info("Connexion à Discord...");
        try {
            discordManager = new DiscordManager(this, token);
        } catch (Exception e) {
            getLogger().severe("Erreur lors de la connexion JDA: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Lancement des tâches de mise à jour
        taskManager = new TaskManager(this, discordManager);
        taskManager.loadTasks();

        getLogger().info("SHDiscordChannels activé avec succès !");
    }

    @Override
    public void onDisable() {
        if (taskManager != null) {
            taskManager.stopTasks();
        }
        if (discordManager != null) {
            discordManager.shutdown();
        }
        getLogger().info("SHDiscordChannels désactivé.");
    }

    public static SHDiscordChannels getInstance() {
        return instance;
    }
}