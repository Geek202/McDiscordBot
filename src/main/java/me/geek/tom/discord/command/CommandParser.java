package me.geek.tom.discord.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.command.commands.HelpCommand;
import me.geek.tom.discord.command.commands.InfoCommand;
import me.geek.tom.discord.command.commands.MappingsCommand;
import me.geek.tom.discord.command.commands.SearchCommand;
import net.dv8tion.jda.api.entities.Message;

/**
 * Handles the execution of the bots commands. Commands are also registered in the constructor.
 */
public class CommandParser {

    /**
     * The dispatcher that is used for handling all commands
     */
    private final CommandDispatcher<MessageSender> dispatcher;

    /**
     * Creates a new command parser and registers all commands.
     */
    public CommandParser() {
        dispatcher = new CommandDispatcher<>();
        dispatcher.register(literal("bye")
                .requires(sender -> sender.getAuthor().getId().equals(DiscordBot.CONFIG.getBotMaster()))
                .executes(ctx -> {
                    ctx.getSource().getMessage().addReaction("\uD83D\uDC4B").queue();
                    ctx.getSource().getMessage().getChannel().sendMessage("Goodbye...").queue();
                    ctx.getSource().getMessage().getJDA().shutdown();
                    return 0;
                })
        );
        new SearchCommand().register(dispatcher);
        new HelpCommand().register(dispatcher);
        new MappingsCommand().register(dispatcher);
        new InfoCommand().register(dispatcher);
    }

    /**
     * Helper method to fix issues relating to generic types
     *
     * @param name name of the literal
     * @return a new {@link LiteralArgumentBuilder<MessageSender>}
     */
    public static LiteralArgumentBuilder<MessageSender> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    /**
     * Helper method again.
     *
     * @param name Name of the argument
     * @param type Type of argument (eg {@link com.mojang.brigadier.arguments.StringArgumentType})
     * @param <T> The argument's return type
     * @return The new {@link RequiredArgumentBuilder}
     */
    public static <T> RequiredArgumentBuilder<MessageSender, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    /**
     * Handles an incoming command
     *
     * @param msg THe message that triggered the command
     * @throws CommandSyntaxException If the command is invalid.
     */
    public void handle(Message msg) throws CommandSyntaxException {
        dispatcher.execute(msg.getContentRaw().substring(1), new MessageSender(msg, msg.getAuthor()));
    }
}
