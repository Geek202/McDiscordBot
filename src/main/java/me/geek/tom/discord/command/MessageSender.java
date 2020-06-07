package me.geek.tom.discord.command;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class MessageSender {

    private final Message message;
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
