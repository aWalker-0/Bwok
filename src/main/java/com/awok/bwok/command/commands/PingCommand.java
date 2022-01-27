package com.awok.bwok.command.commands;

import com.awok.bwok.Config;
import com.awok.bwok.command.CommandContext;
import com.awok.bwok.command.ICommand;
import net.dv8tion.jda.api.JDA;

public class PingCommand implements ICommand {
    @Override
    public void handle(CommandContext commandContext) {
        JDA jda = commandContext.getJDA();
        jda.getRestPing().queue(
                (ping) -> commandContext.getChannel()
                        .sendMessageFormat("Reset ping: %sms\nWS ping: %sms", ping, jda.getGatewayPing()).queue()
        );
    }

    @Override
    public String getHelp() {
        return "**Desc**: Shows the current ping from the bot to the discord servers\n" +
                "**Usage**: `" + Config.get("prefix") +  "ping`";
    }

    @Override
    public String getName() {
        return "ping";
    }
}
