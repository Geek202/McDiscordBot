package me.geek.tom.discord.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.geek.tom.discord.command.MessageSender;

/**
 * Represents a registerable command
 */
public interface ICommand {

    /**
     * Register this command onto the given dispatcher.
     * @param dispatcher The dispatcher to register the command to.
     */
    void register(CommandDispatcher<MessageSender> dispatcher);
}
