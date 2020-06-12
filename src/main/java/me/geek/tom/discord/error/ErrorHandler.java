package me.geek.tom.discord.error;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.geek.tom.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * Generates error embeds.
 */
public class ErrorHandler {

    /**
     * Creates an embed for any {@link Exception} type.
     *
     * @param e The {@link Exception}
     * @param author The {@link User} who invoked the command.
     * @param command The command that the user typed to cause the error
     * @return A {@link MessageEmbed} displaying the error message
     */
    public static MessageEmbed createErrorEmbed(Exception e, User author, String command) {
        EmbedBuilder builder = new EmbedBuilder();

        Collector col = new Collector();
        e.printStackTrace(new PrintWriter(col));
        String stack = col.getResult();
        stack = DiscordBot.trim(stack);

        builder .setTitle("Tom_The_Bot Error!")

                .setColor(Color.RED)

                .setDescription("An error occured while processing this command:")
                .addField("User", "```"+DiscordBot.user(author)+"```", true)
                .addField("Command", "```"+command+"```", true)
                .addField("Message", "```"+e.getLocalizedMessage()+"```", false)
                .addField("Stacktrace", "```"+stack+"```", false);
        DiscordBot.makeBotEmbed(builder);

        return builder.build();
    }

    /**
     * Creates an embed for a {@link CommandSyntaxException}.
     *
     * @param e The {@link CommandSyntaxException}
     * @param author The {@link User} who invoked the command.
     * @param command The command that the user typed to cause the error
     * @return A {@link MessageEmbed} displaying the error message
     */
    public static MessageEmbed createCommandSyntaxEmbed(CommandSyntaxException e, User author, String command) {
        EmbedBuilder builder = new EmbedBuilder();

        builder .setTitle("Command Error!")

                .setColor(Color.RED)

                .addField("User", "```"+DiscordBot.user(author)+"```", true)
                .addField("Command", "```"+command+"```", true)

                .addField("Message:", "```"+e.getRawMessage().getString()+"```", false)
                .addField("Where:", "```"+e.getContext()+"```", false);
        DiscordBot.makeBotEmbed(builder);

        return builder.build();
    }

    private static class Collector extends Writer {

        private StringBuffer buf;
        private String result;

        public Collector() {
            buf = new StringBuffer();
            result = "";
        }

        @Override
        public void write(@NotNull char[] cbuf, int off, int len) throws IOException {
            if (buf == null)
                throw new IOException("Collector has been closed!");
            buf.append(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            if (buf == null)
                throw new IOException("Collector has been closed!");
            
        }

        @Override
        public void close() {
            result = buf.toString();
            buf = null;
        }

        public String getResult() {
            if (buf != null)
                return buf.toString();
            return result;
        }
    }
}
