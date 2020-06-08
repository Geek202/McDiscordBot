package me.geek.tom.discord.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.command.commands.HelpCommand;
import me.geek.tom.discord.command.commands.MappingsCommand;
import me.geek.tom.discord.command.commands.SearchCommand;
import net.dv8tion.jda.api.entities.Message;

public class CommandParser {

    private CommandDispatcher<MessageSender> dispatcher;

    public CommandParser() {
        dispatcher = new CommandDispatcher<>();
        /*dispatcher.register(literal("test").executes(ctx -> {
            ctx.getSource().getMessage().getChannel().sendMessage("Test!").queue();
            return 0;
        }));*/
        dispatcher.register(literal("bye")
                .requires(sender -> sender.getAuthor().getId().equals(DiscordBot.CONFIG.getBotMaster()))
                .executes(ctx -> {
                    ctx.getSource().getMessage().addReaction("\uD83D\uDC4B").queue();
                    ctx.getSource().getMessage().getChannel().sendMessage("Goodbye...").queue();
                    ctx.getSource().getMessage().getJDA().shutdown();
                    return 0;
                })
        );
        /*dispatcher.register(literal("err")
                .requires(sender -> sender.getAuthor().getId().equals(DiscordBot.CONFIG.getBotMaster()))
                .executes(ctx -> {
                    throw new CommandInvokationException(new RuntimeException("This is a test error message lol"));
                })
        );*/
        new SearchCommand().register(dispatcher);
        new HelpCommand().register(dispatcher);
        new MappingsCommand().register(dispatcher);
        //new TestPageCommand().register(dispatcher);
    }

    public static LiteralArgumentBuilder<MessageSender> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<MessageSender, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public void handle(Message msg) throws CommandSyntaxException {
        dispatcher.execute(msg.getContentRaw().substring(1), new MessageSender(msg, msg.getAuthor()));
    }
}
