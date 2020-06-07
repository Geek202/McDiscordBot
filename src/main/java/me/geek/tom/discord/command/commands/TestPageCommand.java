package me.geek.tom.discord.command.commands;

import com.jagrosh.jdautilities.menu.Paginator;
import com.mojang.brigadier.CommandDispatcher;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.command.MessageSender;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static me.geek.tom.discord.command.CommandParser.literal;

public class TestPageCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<MessageSender> dispatcher) {
        dispatcher.register(
                literal("testpage")
                    .executes(ctx -> {
                        List<String> datas = new ArrayList<>();
                        for (int i = 0; i < 100; i++) {
                            datas.add("Item number: " + i);
                        }

                        Paginator.Builder builder = new Paginator.Builder()
                                .setEventWaiter(DiscordBot.waiter)
                                .setColumns(1)
                                .setItemsPerPage(10)
                                .waitOnSinglePage(false)
                                .useNumberedItems(false)
                                .showPageNumbers(true)
                                .setTimeout(60, TimeUnit.SECONDS)
                                .setFinalAction(m -> {
                                    try {
                                        m.clearReactions().queue();
                                    } catch (PermissionException e) {
                                        m.delete().queue();
                                    } });
                        builder.clearItems();

                        datas.stream().forEach(builder::addItems);
                        Paginator p = builder.setColor(Color.GREEN).setText("THis is a test").setUsers(ctx.getSource().getAuthor())
                                .build();
                        p.paginate(ctx.getSource().getMessage().getChannel(), 1);

                        return 0;
                    })
        );
    }
}
