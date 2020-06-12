package me.geek.tom.discord.error;

/**
 * Thrown when a command fails in execution.
 * This is used to forward events to the EventHandler so the user can be informed.
 */
public class CommandInvokationException extends RuntimeException {
    public CommandInvokationException(Throwable t) {
        super(t);
    }
}
