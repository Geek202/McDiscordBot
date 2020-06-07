package me.geek.tom.discord;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.geek.tom.discord.command.CommandParser;
import me.geek.tom.discord.config.Config;
import me.geek.tom.discord.error.ErrorHandler;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.time.Instant;

public class DiscordBot extends ListenerAdapter {

    public static String BOT_MASTER;
    public static String FORGE_VERSION;
    public static boolean ROBOTS_ALLOWED;

    private static final File CONFIG_FILE = new File("./config.toml");

    public static Logger LOGGER = LogManager.getLogger();

    private final CommandParser parser = new CommandParser();
    public static final EventWaiter waiter = new EventWaiter();

    public static void main(String[] args) throws Exception {
        LOGGER.info(Logging.LAUNCH, "Starting bot...");

        Config.ConfigData data = Config.loadConfig(CONFIG_FILE);
        if (!data.isConfigured()) {
            LOGGER.error(Logging.LAUNCH, "Please modify " + CONFIG_FILE.getPath() + " to configure all the things.");
            return;
        }

        BOT_MASTER = data.getBotMaster();
        FORGE_VERSION = data.getForgeVersion();
        ROBOTS_ALLOWED = data.botListenToRobots();
        JDABuilder builder = JDABuilder.createDefault(data.getBotToken());
        builder.setActivity(Activity.playing("with "+FORGE_VERSION));

        DiscordBot bot = new DiscordBot();
        builder.addEventListeners(bot, waiter);
        JDA jda = builder.build();
        jda.awaitReady();
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        LOGGER.info(Logging.BOT, "Bot ready on " + event.getGuildTotalCount() + " guilds!");
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!ROBOTS_ALLOWED && event.getAuthor().isBot()) return;
        String msg = event.getMessage().getContentStripped();
        if (msg.startsWith("|")) {
            LOGGER.info(Logging.COMMAND, "Got message from " + user(event.getAuthor()) + " with content: " + event.getMessage().getContentRaw() + " AND ITS A COMMAND!");
            try {
                parser.handle(event.getMessage());
            } catch (CommandSyntaxException e) {
                event.getChannel().sendMessage(ErrorHandler.createCommandSyntaxEmbed(e, event.getAuthor())).queue();
            } catch (Exception e) {
                event.getChannel().sendMessage(ErrorHandler.createErrorEmbed(e, event.getAuthor(), event.getMessage().getContentDisplay())).queue();
            }
        }
    }

    public static void makeBotEmbed(EmbedBuilder builder) {
        builder .setAuthor("Tom_The_Bot#8678")
                .setFooter("Tom_The_Bot - Powered by Java and brigadier! - Tom_The_Geek#8559")
                .setTimestamp(Instant.now());
    }

    public static String trim(String msg) {
        if (msg.length() > 1000)
            return msg.substring(0, 998)+"...";
        return msg;
    }

    public static String user(User user) {
        return user.getName()+"#"+user.getDiscriminator();
    }
}
