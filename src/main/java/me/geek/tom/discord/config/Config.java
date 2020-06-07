package me.geek.tom.discord.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import java.io.File;

public class Config {

    /**
     * The default value for config entries.
     */
    private static final String DEFAULT_VALUE = "\\__CHANGE_ME__/";

    /**
     * Loads the config from a file
     * @param file The file to load from
     * @return The loaded {@link ConfigData}
     */
    public static ConfigData loadConfig(File file) {
        CommentedFileConfig config = CommentedFileConfig.builder(file).autosave().build();
        config.load();
        ConfigSpec spec = new ConfigSpec();

        spec.define("bot.token", DEFAULT_VALUE);
        spec.define("bot.master", DEFAULT_VALUE);
        spec.define("bot.listenToRobots", false);

        spec.define("forge.version", DEFAULT_VALUE);

        spec.correct(config);

        config.setComment("bot", "Settings for the bot user");
        config.setComment("bot.token", "The token the bot uses to login - from discord.com/developers");
        config.setComment("bot.master", "The user id of the owner of this bot.");
        config.setComment("bot.listenToRobots", "Should the bot listen to commands/messages from other bots.");

        config.setComment("forge", "Settings for the Forge JAR to use");
        config.setComment("forge.version", "The version of Forge to use. Will be used as the filename like <version>.jar");

        config.save();

        return new ConfigData(config.get("bot.token"), config.get("bot.master"), config.get("bot.listenToRobots"), config.get("forge.version"));
    }

    /**
     * Represents the contents of a config file.
     */
    public static class ConfigData {
        /**
         * The token used to authenticate with discord.
         */
        private final String botToken;
        /**
         * The user id of the bot's owner
         */
        private final String botMaster;
        /**
         * Should the bot listen to messages from other bots.
         */
        private final boolean botListenToRobots;
        /**
         * The name of the Forge JAR to use.
         */
        private final String forgeVersion;

        private ConfigData(String botToken, String botMaster, boolean botListenToRobots, String forgeVersion) {
            this.botToken = botToken;
            this.botMaster = botMaster;
            this.botListenToRobots = botListenToRobots;
            this.forgeVersion = forgeVersion;
        }

        public String getBotToken() {
            return botToken;
        }

        public String getBotMaster() {
            return botMaster;
        }

        public String getForgeVersion() {
            return forgeVersion;
        }

        public boolean isConfigured() {
            return  !botToken.equals(DEFAULT_VALUE) &&
                    !botMaster.equals(DEFAULT_VALUE) &&
                    !forgeVersion.equals(DEFAULT_VALUE);
        }

        public boolean botListenToRobots() {
            return botListenToRobots;
        }
    }

}
