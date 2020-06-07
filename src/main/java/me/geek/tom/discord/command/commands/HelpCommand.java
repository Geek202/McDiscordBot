package me.geek.tom.discord.command.commands;

import com.jagrosh.jdautilities.menu.Paginator;
import com.mojang.brigadier.CommandDispatcher;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.command.MessageSender;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static me.geek.tom.discord.command.CommandParser.literal;

public class HelpCommand implements ICommand
{
    @Override
    public void register(CommandDispatcher<MessageSender> dispatcher) {
        dispatcher.register(literal("help")
                .executes(ctx -> {
                    User user = ctx.getSource().getAuthor();
                    String[] help = dispatcher.getAllUsage(dispatcher.getRoot(), ctx.getSource(), true);
                    Paginator.Builder builder = new Paginator.Builder()
                            .setEventWaiter(DiscordBot.waiter)        // event thing
                            .setColumns(1)                            // Only one column pls
                            .setItemsPerPage(15)                      // 15 items means less pages
                            .waitOnSinglePage(false)                  // Should it immediatly timeout if there is one page
                            .useNumberedItems(false)                  // No item numbers pls
                            .showPageNumbers(true)                    // What page are you on?
                            .setTimeout(60, TimeUnit.SECONDS)  // Don't keep the pager alive for too long.
                            .setColor(Color.BLUE)                     // Green!
                            .setText("Available commands:")           // Title, is in the message text above the embed
                            .setUsers(user)                           // Only the original user pls
                            .setFinalAction(m -> {                    // Cleanup
                                try {
                                    m.clearReactions().queue();
                                } catch (PermissionException e) {
                                    m.delete().queue();
                                } });
                    builder.clearItems();
                    Arrays.stream(help)
                        .map(s -> "|"+s)
                        .map(s -> "`"+s+"`")
                        .forEach(builder::addItems);
                    builder.build().paginate(ctx.getSource().getMessage().getChannel(), 1);

                    return 0;
                })
        );
    }
}
