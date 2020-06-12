package me.geek.tom.discord.command;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

/**
 * Represents the contex that triggered a command.
 */
public class MessageSender {

    /**
     * The {@link Message} that triggered the command
     */
    private final Message message;
    /**
     * The {@link User} who sent the message
     */
    private final User author;

    public MessageSender(Message message, User author) {
        this.message = message;
        this.author = author;
    }

    public Message getMessage() {
        return message;
    }

    public User getAuthor() {
        return author;
    }
}
