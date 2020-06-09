package me.geek.tom.discord.command.commands;

import com.jagrosh.jdautilities.menu.Paginator;
import com.mojang.brigadier.CommandDispatcher;
import javafx.util.Pair;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.command.MessageSender;
import me.geek.tom.discord.error.CommandInvokationException;
import me.geek.tom.discord.search.MappingsSearch;
import me.geek.tom.discord.startup.MappingsDownloader;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
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

        dispatcher.register(literal("mcp")
                .then(literal("method")
                    .then(argument("method", greedyString())
                        .executes(ctx -> {
                            try {
                                List<MappingsDownloader.MethodMapping> res = MappingsSearch.searchMethods(getString(ctx, "method"));
                                User user = ctx.getSource().getAuthor();
                                Paginator.Builder builder = new Paginator.Builder()
                                        .setEventWaiter(DiscordBot.waiter)        // event thing
                                        .setColumns(1)                            // Only one column pls
                                        .setItemsPerPage(10)                      // 10 items means less chance of overflow. :D
                                        .waitOnSinglePage(false)                  // Should it immediatly timeout if there is one page
                                        .useNumberedItems(false)                  // No item numbers pls
                                        .showPageNumbers(true)                    // What page are you on?
                                        .setTimeout(60, TimeUnit.SECONDS)  // Don't keep the pager alive for too long.
                                        .setColor(Color.YELLOW)                   // Yellow!
                                        .setText("MCP Methods:")                  // Title, is in the message text above the embed
                                        .setUsers(user)                           // Only the original user pls
                                        .setFinalAction(m -> {                    // Cleanup
                                            try {
                                                m.clearReactions().queue();
                                            } catch (PermissionException e) {
                                                m.delete().queue();
                                            } });
                                builder.clearItems();
                                if (!res.isEmpty())
                                    res.stream().map(MappingsDownloader.MethodMapping::toNiceString).forEach(builder::addItems);
                                else
                                    builder.addItems("No results. "+MappingsSearch.NO_RESULTS_EMOJI);
                                builder.build().paginate(ctx.getSource().getMessage().getChannel(), 1);
                            } catch (IOException e) {
                                throw new CommandInvokationException(e);
                            }

                            return 0;
                        })))
                .then(literal("field")
                        .then(argument("field", greedyString()).executes(ctx -> {
                            try {
                                List<MappingsDownloader.FieldMapping> res = MappingsSearch.searchFields(getString(ctx, "field"));
                                User user = ctx.getSource().getAuthor();
                                Paginator.Builder builder = new Paginator.Builder()
                                        .setEventWaiter(DiscordBot.waiter)        // event thing
                                        .setColumns(1)                            // Only one column pls
                                        .setItemsPerPage(10)                      // 10 items means less chance of overflow. :D
                                        .waitOnSinglePage(false)                  // Should it immediatly timeout if there is one page
                                        .useNumberedItems(false)                  // No item numbers pls
                                        .showPageNumbers(true)                    // What page are you on?
                                        .setTimeout(60, TimeUnit.SECONDS)  // Don't keep the pager alive for too long.
                                        .setColor(Color.YELLOW)                   // Yellow!
                                        .setText("MCP Fields:")                   // Title, is in the message text above the embed
                                        .setUsers(user)                           // Only the original user pls
                                        .setFinalAction(m -> {                    // Cleanup
                                            try {
                                                m.clearReactions().queue();
                                            } catch (PermissionException e) {
                                                m.delete().queue();
                                            } });
                                builder.clearItems();
                                if (!res.isEmpty())
                                    res.stream().map(MappingsDownloader.FieldMapping::toNiceString).forEach(builder::addItems);
                                else
                                    builder.addItems("No results. "+MappingsSearch.NO_RESULTS_EMOJI);
                                builder.build().paginate(ctx.getSource().getMessage().getChannel(), 1);
                            } catch (IOException e) {
                                throw new CommandInvokationException(e);
                            }

                            return 0;
                        })))
                .then(literal("class")
                        .then(argument("class", greedyString()).executes(ctx -> {
                            try {
                                List<Pair<String, String>> res = MappingsSearch.searchClasses(getString(ctx, "class"));
                                User user = ctx.getSource().getAuthor();
                                Paginator.Builder builder = new Paginator.Builder()
                                        .setEventWaiter(DiscordBot.waiter)        // event thing
                                        .setColumns(1)                            // Only one column pls
                                        .setItemsPerPage(10)                      // 10 items means less chance of overflow. :D
                                        .waitOnSinglePage(false)                  // Should it immediatly timeout if there is one page
                                        .useNumberedItems(false)                  // No item numbers pls
                                        .showPageNumbers(true)                    // What page are you on?
                                        .setTimeout(60, TimeUnit.SECONDS)  // Don't keep the pager alive for too long.
                                        .setColor(Color.YELLOW)                   // Yellow!
                                        .setText("MCP Classes:")                  // Title, is in the message text above the embed
                                        .setUsers(user)                           // Only the original user pls
                                        .setFinalAction(m -> {                    // Cleanup
                                            try {
                                                m.clearReactions().queue();
                                            } catch (PermissionException e) {
                                                m.delete().queue();
                                            } });
                                builder.clearItems();
                                if (!res.isEmpty())
                                    res.stream().map(this::classMapToString).forEach(builder::addItems);
                                else
                                    builder.addItems("No results. "+MappingsSearch.NO_RESULTS_EMOJI);
                                builder.build().paginate(ctx.getSource().getMessage().getChannel(), 1);
                            } catch (IOException e) {
                                throw new CommandInvokationException(e);
                            }

                            return 0;
                        })))
        );
    }

    private String classMapToString(Pair<String, String> mapping) {
        return "Class: `"+mapping.getKey()+"` -> `"+mapping.getValue()+"`";
    }
}
