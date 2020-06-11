package me.geek.tom.discord.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.command.MessageSender;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.Color;

import static me.geek.tom.discord.command.CommandParser.literal;

public class InfoCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<MessageSender> dispatcher) {
        LiteralCommandNode<MessageSender> authors = dispatcher.register(literal("authors").executes(ctx -> {
            ctx.getSource().getMessage().getChannel().sendMessage(createAuthorEmbed().build()).queue();
            return 0;
        }));
        dispatcher.register(literal("credits").redirect(authors));
    }

    private EmbedBuilder createAuthorEmbed() {
        EmbedBuilder builder = new EmbedBuilder();
        DiscordBot.makeBotEmbed(builder);

        builder.setColor(Color.MAGENTA);
        builder.setTitle("Authors!");
        builder.setDescription("Here is a list of all people involved in McBot:");
        builder.addField("Tom_The_Geek", "Created the bot", true);
        builder.addField("suppergerrie2", "Helped test and suggested ideas", true);

        return builder;
    }
}
