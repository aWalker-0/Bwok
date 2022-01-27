package com.awok.bwok;

import com.awok.bwok.command.CommandContext;
import com.awok.bwok.command.ICommand;
import com.awok.bwok.command.commands.HelpCommand;
import com.awok.bwok.command.commands.PingCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CommandManager {
    private final List<ICommand> commands = new ArrayList<>();

    // We add all commands we make here
    public CommandManager() {
        addCommand(new PingCommand());
        addCommand(new HelpCommand(this));
    }

    // Add command method (checks if 'newCommand' already exists)
    private void addCommand(ICommand newCommand) {
        // 'nameFound' true if name of command already exists
        boolean nameFound = this.commands.stream().anyMatch(
                (it) -> it.getName().equalsIgnoreCase(newCommand.getName())
        );

        // Throw exception if command in 'newCommand' already exists (if 'nameFound' true)
        if (nameFound) {
            throw new IllegalArgumentException("A command with this name is already present");
        }

        // Add command to list 'commands' if 'nameFound' false
        commands.add(newCommand);
    }


    public List<ICommand> getCommands() {
        return commands;
    }


    @Nullable
    public ICommand getCommand(String search) {
        // Standardize search parameter
        String searchLower = search.toLowerCase();

        // Parse list 'commands', returning command searched; return null if not found
        for (ICommand command : this.commands) {
            if (command.getName().equals(search) || command.getAliases().contains(searchLower)) {
                return command;
            }
        }
        return null;
    }


    void handle(MessageReceivedEvent event) {
        String[] split = event.getMessage().getContentRaw()
                .replaceFirst("(?i)" + Pattern.quote(Config.get("prefix")), "")
                        .split("\\s+");

        String invoke = split[0].toLowerCase();
        ICommand command = this.getCommand(invoke);

        if (command != null) {
            event.getChannel().sendTyping().queue();
            List<String> args = Arrays.asList(split).subList(1, split.length);

            CommandContext commandContext = new CommandContext(event, args);

            command.handle(commandContext);

        }
    }
}
