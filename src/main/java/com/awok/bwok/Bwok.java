package com.awok.bwok;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;

public class Bwok {

    private Bwok() throws LoginException {
        String token = System.getenv("DISCORD_BOT_TOKEN");

        // We only need 2 gateway intents enabled for this example:
        EnumSet<GatewayIntent> intents = EnumSet.of(
                // We need messages in guilds to accept commands from users
                GatewayIntent.GUILD_MESSAGES,
                // We need voice states to connect to the voice channel
                GatewayIntent.GUILD_VOICE_STATES,
                // Track where members join
                GatewayIntent.GUILD_MEMBERS,
                // Allow to use guild emojis
                GatewayIntent.GUILD_EMOJIS

        );

        // Build bot entity with these characteristics
        JDA jda = JDABuilder.createDefault(Config.get("TOKEN"), intents)
                .addEventListeners(new Listener())
                .setActivity(Activity.listening("perpetual *yeah*'ing"))
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .enableCache(CacheFlag.VOICE_STATE)
                .build();
    }

    public static void main(String[] args) throws LoginException {
        new Bwok();
    }
}
