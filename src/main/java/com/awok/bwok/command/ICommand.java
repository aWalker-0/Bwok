package com.awok.bwok.command;

import java.util.List;

public interface ICommand {
    void handle(CommandContext commandContext);

    String getName();

    String getHelp();

    default List<String> getAliases() {
        return List.of();
    }
}
