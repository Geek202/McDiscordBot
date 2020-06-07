package me.geek.tom.discord.error;

public class CommandInvokationException extends RuntimeException {

    public CommandInvokationException(Throwable t) {
        super(t);
    }

}
