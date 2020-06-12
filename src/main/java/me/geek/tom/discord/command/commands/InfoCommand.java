package me.geek.tom.discord.command.commands;

import com.jagrosh.jdautilities.menu.Paginator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.command.MessageSender;
import me.geek.tom.discord.error.CommandInvokationException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.kohsuke.github.*;

import java.awt.Color;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static me.geek.tom.discord.command.CommandParser.literal;

/**
 * Adds a command that allows listing contributors and commits on the bot's GitHub
 */
public class InfoCommand implements ICommand {

    /**
     * An instance of the GitHub api.
     */
    private static GitHub github;

    static {
        try {
            github = new GitHubBuilder().build();
        } catch (IOException e) {
            e.printStackTrace();
            github = null;
        }
    }

    /**
     * Registers the commands for {@code |author} and {@code |credits},
     * and also registers {@code |github} if the {@link GitHub} object was created successfully.
     *
     * @param dispatcher The dispatcher to register the command to.
     */
    @Override
    public void register(CommandDispatcher<MessageSender> dispatcher) {
        dispatcher.register(literal("authors").executes(this::authors));
        dispatcher.register(literal("credits").executes(this::authors));
        if (!(github == null)) {
            // register github commands.
            dispatcher.register(literal("github")
                    .then(literal("contrib").executes(this::contributors))
                    .then(literal("changes").executes(this::commits))
            );
        }
    }

    /**
     * If you add something, credit yourself here
     * @return The {@link EmbedBuilder} for the {@code |authors} command.
     */
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

    /**
     * Handles the command {@code |authors}
     *
     * @param ctx CommandContext
     * @return 0
     */
    private int authors(CommandContext<MessageSender> ctx) {
        ctx.getSource().getMessage().getChannel().sendMessage(createAuthorEmbed().build()).queue();
        return 0;
    }

    /**
     * Handles the command {@code |github commits}
     *
     * @param ctx CommandContext
     * @return 0
     */
    private int commits(CommandContext<MessageSender> ctx) {
        try {
            User user = ctx.getSource().getAuthor();
            Paginator.Builder builder = new Paginator.Builder()
                    .setEventWaiter(DiscordBot.waiter)        // event thing
                    .setColumns(1)                            // Only one column pls
                    .setItemsPerPage(10)                      // 15 items means less pages
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
                        }
                    });
            builder.clearItems();

            GHRepository repo = github.getRepository("Geek202/McDiscordBot");
            PagedIterable<GHCommit> commits = repo.listCommits();
            builder.setText("Commits for Geek202/McDiscordBot");
            for (GHCommit commit : commits) {
                builder.addItems("`"+commit.getSHA1().substring(0, 7)+"` `" + commit.getAuthor().getName() + "`: `" + commit.getCommitShortInfo().getMessage() + "`");
            }
            builder.build().paginate(ctx.getSource().getMessage().getChannel(), 1);
        } catch (IOException e) {
            throw new CommandInvokationException(e);
        }

        return 0;
    }

    /**
     * Handles the command {@code |github contribs}
     *
     * @param ctx CommandContext
     * @return 0
     */
    private int contributors(CommandContext<MessageSender> ctx) {
        try {
            EmbedBuilder builder = new EmbedBuilder();
            DiscordBot.makeBotEmbed(builder);

            GHRepository repo = github.getRepository("Geek202/McDiscordBot");
            PagedIterable<GHRepository.Contributor> contributors = repo.listContributors();
            builder.setTitle("Contributors for Geek202/McDiscordBot", "https://github.com/Geek202/McDiscordBot");
            for (GHRepository.Contributor contributor : contributors) {
                builder.addField(contributor.getName(), contributor.getContributions() + " contributions.", true);
            }
            ctx.getSource().getMessage().getChannel().sendMessage(builder.build()).queue();
        } catch (IOException e) {
            throw new CommandInvokationException(e); // delegate to error handler.
        }

        return 0;
    }
}
