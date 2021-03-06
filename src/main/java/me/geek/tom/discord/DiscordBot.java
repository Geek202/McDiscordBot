package me.geek.tom.discord;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.geek.tom.discord.command.CommandParser;
import me.geek.tom.discord.config.Config;
import me.geek.tom.discord.error.ErrorHandler;
import me.geek.tom.discord.startup.ForgeJarSetup;
import me.geek.tom.discord.startup.MappingsDownloader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
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
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DiscordBot extends ListenerAdapter {

    public static Config.ConfigData CONFIG;

    private static final File CONFIG_FILE = new File("./config.toml");

    public static Logger LOGGER = LogManager.getLogger();

    private final CommandParser parser = new CommandParser();
    public static final EventWaiter waiter = new EventWaiter();
    public static MappingsDownloader.MappingsData MAPPINGS;
    public static File FORGE_JAR;

    private static JDA jda;

    private static List<Supplier<Activity>> ACTIVITIES;
    private static int currentActivity = 0;

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

        FORGE_JAR = ForgeJarSetup.setupForge(CONFIG.getForgeMappings(), CONFIG.getForgeVersion(), CONFIG.getForgeMcVersion());

        ACTIVITIES = new ArrayList<>();
        ACTIVITIES.add(()-> Activity.listening("to "+CONFIG.getCommandPrefix()+"help..."));
        ACTIVITIES.add(()-> Activity.playing("with "+CONFIG.getForgeVersion()));
        ACTIVITIES.add(()-> {
            int userCount = 0;
            int guildCount = 0;
            for (Guild guild : jda.getGuilds()) {
                userCount += guild.getMemberCount();
                guildCount++;
            }
            return Activity.watching(userCount + " users on " + guildCount + " guilds!");
        });
        ACTIVITIES.add(()-> Activity.watching(MappingsDownloader.mappingsCount + " lines of mappings."));

        JDABuilder builder = JDABuilder.createDefault(CONFIG.getBotToken());
        builder.setActivity(ACTIVITIES.get(currentActivity).get());
        currentActivity++;
        currentActivity%=ACTIVITIES.size();

        DiscordBot bot = new DiscordBot();
        builder.addEventListeners(bot, waiter);
        jda = builder.build();
        jda.awaitReady();
        activityUpdator.scheduleAtFixedRate(DiscordBot::updateStatus, CONFIG.getBotActivityUpdateRate(), CONFIG.getBotActivityUpdateRate(), TimeUnit.SECONDS);
    }

    private static void updateStatus() {
        jda.getPresence().setActivity(ACTIVITIES.get(currentActivity).get());
        currentActivity++;
        currentActivity%=ACTIVITIES.size();
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
                event.getChannel().sendMessage(ErrorHandler.createCommandSyntaxEmbed(e, event.getAuthor(), event.getMessage().getContentDisplay())).queue();
            } catch (Exception e) {
                event.getChannel().sendMessage(ErrorHandler.createErrorEmbed(e, event.getAuthor(), event.getMessage().getContentDisplay())).queue();
            }
        }
    }

    public static void makeBotEmbed(EmbedBuilder builder) {
        builder .setAuthor(user(jda.getSelfUser()))
                .setFooter(jda.getSelfUser().getName()+" - Powered by Java and Brigadier! - Tom_The_Geek#8559")
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
