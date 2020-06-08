package me.geek.tom.discord.command.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.command.MessageSender;
import net.dv8tion.jda.api.EmbedBuilder;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static me.geek.tom.discord.command.CommandParser.argument;
import static me.geek.tom.discord.command.CommandParser.literal;

public class MappingsCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<MessageSender> dispatcher) {
        dispatcher.register(literal("mappings_ver").executes(ctx -> {
            ctx.getSource().getMessage().getChannel().sendMessage(
                    DiscordBot.versionEmbed(
                            DiscordBot.CONFIG.getForgeMappings() + " for MC " + DiscordBot.CONFIG.getForgeMcVersion(),
                            DiscordBot.user(ctx.getSource().getAuthor()),
                            "Mappings version"
                    )
            ).queue();
            return 0;
        }));

        dispatcher.register(literal("mcpm")
                .then(argument("method", greedyString())
                        .executes(ctx -> {
                            EmbedBuilder builder = new EmbedBuilder();
                            DiscordBot.makeBotEmbed(builder);
                            builder.addField("Results go here...", "Results go here...", false);

                            ctx.getSource().getMessage().getChannel().sendMessage(builder.build()).queue();
                            return 0;
                        })));
    }
}
