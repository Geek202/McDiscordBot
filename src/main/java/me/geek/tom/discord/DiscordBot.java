package me.geek.tom.discord;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.geek.tom.discord.command.CommandParser;
import me.geek.tom.discord.config.Config;
import me.geek.tom.discord.error.ErrorHandler;
import me.geek.tom.discord.startup.MappingsDownloader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiscordBot extends ListenerAdapter {

    public static Config.ConfigData CONFIG;

    private static final File CONFIG_FILE = new File("./config.toml");

    public static Logger LOGGER = LogManager.getLogger();

    private final CommandParser parser = new CommandParser();
    public static final EventWaiter waiter = new EventWaiter();
    public static MappingsDownloader.MappingsData MAPPINGS;

    private static JDA jda;

    private static final ScheduledExecutorService activityUpdator = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Activity-Update-Thread.");
        thread.setDaemon(true);
        return thread;
    });

    public static void main(String[] args) throws Exception {
        LOGGER.info(Logging.LAUNCH, "Starting bot...");

        CONFIG = Config.loadConfig(CONFIG_FILE);
        if (!CONFIG.isConfigured()) {
            LOGGER.error(Logging.LAUNCH, "Please modify " + CONFIG_FILE.getPath() + " to configure all the things.");
            return;
        }

        MAPPINGS = MappingsDownloader.setupMcp();

        JDABuilder builder = JDABuilder.createDefault(CONFIG.getBotToken());
        builder.setActivity(Activity.listening("to "+CONFIG.getCommandPrefix()+"help..."));

        DiscordBot bot = new DiscordBot();
        builder.addEventListeners(bot, waiter);
        jda = builder.build();
        jda.awaitReady();
        // TODO: Random status message.
        //activityUpdator.scheduleAtFixedRate(DiscordBot::updateStatus, 1, 1, TimeUnit.MINUTES);
    }

    private static void updateStatus() {
    }

    public static MessageEmbed versionEmbed(String version, String user, String type) {
        EmbedBuilder builder = new EmbedBuilder();
        makeBotEmbed(builder); // Configure title and footer.

        builder .setTitle("Search results")
                .addField("Requested by:", "```"+user+"```", true)
                .addField("Type:", "```"+type+"```", true)
                .addField("Result:", "```"+version+"```", false);

        return builder.build();
    }

    @Override
    public void onReady(@Nonnull ReadyEvent event) {
        LOGGER.info(Logging.BOT, "Bot ready on " + event.getGuildTotalCount() + " guilds!");
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (!CONFIG.botListenToRobots() && event.getAuthor().isBot()) return;
        String msg = event.getMessage().getContentStripped();
        if (msg.startsWith(CONFIG.getCommandPrefix())) {
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
