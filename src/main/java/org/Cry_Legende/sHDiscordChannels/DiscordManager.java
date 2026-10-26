package org.Cry_Legende.sHDiscordChannels;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordManager {

    private final SHDiscordChannels plugin;
    private JDA jda;
    private final Set<String> pendingRenames = ConcurrentHashMap.newKeySet();

    public DiscordManager(SHDiscordChannels plugin, String token) {
        this.plugin = plugin;
        // CreateLight est suffisant car on n'a pas besoin de cache message/membre
        this.jda = JDABuilder.createLight(token, Collections.emptyList())
            .enableIntents(GatewayIntent.GUILD_VOICE_STATES) // Utile si on veut voir qui est dedans plus tard
            .build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Met à jour le nom d'un channel vocal.
     * Cette méthode doit être appelée en ASYNC pour ne pas bloquer le serveur si l'API lag.
     */
    public void updateVoiceChannelName(String channelId, String newName) {
        if (jda == null) return;

        VoiceChannel channel = jda.getVoiceChannelById(channelId);

        if (channel == null) {
            plugin.getLogger().warning("Channel Discord introuvable pour l'ID: " + channelId);
            return;
        }

        // Optimisation: On ne fait l'appel API que si le nom est différent
        if (channel.getName().equals(newName)) {
            return;
        }

        // Un seul renommage en attente par channel : Discord n'en autorise que 2 par 10 minutes, JDA met les suivants
        // en file et le nom en cache ne change qu'à l'exécution, donc chaque tour en ajoutait un, déjà périmé.
        // Le tour suivant enverra la valeur à jour.
        if (!pendingRenames.add(channelId)) {
            return;
        }
        try {
            // Queue l'action pour que JDA gère le rate limit interne
            channel.getManager().setName(newName).queue(
                success -> pendingRenames.remove(channelId),
                error -> {
                    pendingRenames.remove(channelId);
                    plugin.getLogger().warning("Impossible de renommer le channel " + channelId + ": " + error.getMessage());
                }
            );
        } catch (Exception e) {
            pendingRenames.remove(channelId);
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}
