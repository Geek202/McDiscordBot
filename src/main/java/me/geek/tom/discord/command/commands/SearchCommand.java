package me.geek.tom.discord.command.commands;

import com.jagrosh.jdautilities.menu.Paginator;
import com.mojang.brigadier.CommandDispatcher;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.command.MessageSender;
import me.geek.tom.discord.error.CommandInvokationException;
import me.geek.tom.discord.search.ClassFieldDump;
import me.geek.tom.discord.search.ClassSearch;
import me.geek.tom.discord.search.MethodSearch;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static me.geek.tom.discord.command.CommandParser.argument;
import static me.geek.tom.discord.command.CommandParser.literal;

/**
 * Command that allows searching of the Minecraft JAR.
 */
public class SearchCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<MessageSender> dispatcher) {
        dispatcher.register(
                literal("search")
                    .then(literal("classname") // Search for classes.
                        .then(argument("term", greedyString()) // What classes to search for.
                            .executes(ctx -> {
                                try {
                                    // Do the search
                                    String term = getString(ctx, "term");
                                    List<String> res = ClassSearch.doSearch(term);

                                    User user = ctx.getSource().getAuthor();
                                    sendResults(
                                            createTopResultEmbed(res, DiscordBot.user(user), term, "Classes"),
                                            res, user, ctx.getSource().getMessage().getChannel()
                                    );
                                } catch (IOException e) {
                                    // Wrap exception so the CommandParser can handle it better.
                                    throw new CommandInvokationException(e);
                                }
                                return 0;
                            })
                        )
                    ).then(literal("fields")
                        .then(argument("class", greedyString())
                                .executes(ctx -> {
                                    try {
                                        String cls = getString(ctx, "class");
                                        List<String> res = ClassFieldDump.get(cls);

                                        User user = ctx.getSource().getAuthor();
                                        sendResults(
                                                createTopResultEmbed(res, DiscordBot.user(user), cls, "Fields"),
                                                res, user, ctx.getSource().getMessage().getChannel()
                                        );
                                    } catch (IOException e) {
                                        throw new CommandInvokationException(e);
                                    }
                                    return 0;
                                })
                        )
                ).then(literal("methods")
                        .then(argument("class", greedyString())
                                .executes(ctx -> {
                                    try {
                                        String cls = getString(ctx, "class");
                                        List<String> res = MethodSearch.doSearch(cls)
                                                .stream()
                                                .filter(s -> !s.contains("private lambda$"))
                                                .filter(s -> !s.contains("private static lambda$"))
                                                .collect(Collectors.toList());

                                        User user = ctx.getSource().getAuthor();
                                        sendResults(
                                                createTopResultEmbed(res, DiscordBot.user(user), cls, "Fields"),
                                                res, user, ctx.getSource().getMessage().getChannel()
                                        );
                                    } catch (IOException e) {
                                        throw new CommandInvokationException(e);
                                    }
                                    return 0;
                                })
                        )
                )
        );

        dispatcher.register(
                literal("forge_ver")
                    .executes(ctx -> {
                        User user = ctx.getSource().getAuthor();
                        ctx.getSource().getMessage().getChannel().sendMessage(
                                DiscordBot.versionEmbed(DiscordBot.CONFIG.getForgeVersion(), DiscordBot.user(user), "Forge Version")).queue();

                        return 0;
                    }
                )
        );
    }

    private MessageEmbed createTopResultEmbed(List<String> results, String user, String term, String type) {
        EmbedBuilder builder = new EmbedBuilder();
        DiscordBot.makeBotEmbed(builder); // Configure title and footer.

        builder .setTitle("Search results")
                .addField("Requested by:", "```"+user+"```", true)
                .addField("Term:", "```"+term+"```", true)
                .addField("Type:", "```"+type+"```", true)
                .addField("Result count:", "```"+results.size()+"```", false);

        return builder.build();
    }

    private void sendResults(MessageEmbed topEmbed, List<String> results, User user, MessageChannel channel) {
        Paginator.Builder builder = new Paginator.Builder()
                .setEventWaiter(DiscordBot.waiter)        // event thing
                .setColumns(1)                            // Only one column pls
                .setItemsPerPage(15)                      // 15 items means less pages
                .waitOnSinglePage(true)                   // Should it immediatly timeout if there is one page
                .useNumberedItems(false)                  // No item numbers pls
                .showPageNumbers(true)                    // What page are you on?
                .setTimeout(60, TimeUnit.SECONDS)  // Don't keep the pager alive for too long.
                .setColor(Color.GREEN)                    // Green!
                .setText("Results:")                      // Title, is in the message text above the embed
                .setUsers(user)                           // Only the original user pls
                .setFinalAction(m -> {                    // Cleanup
                    try {
                        m.clearReactions().queue();
                    } catch (PermissionException e) {
                        m.delete().queue();
                    } });
        builder.clearItems();
        if (!results.isEmpty())
            results.stream().map(s -> "`"+s+"`").forEach(builder::addItems);
        else
            builder.addItems("No results!");

        channel.sendMessage(topEmbed).queue();
        builder.build().paginate(channel, 1);
    }
}
