package me.geek.tom.discord.search;

import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.startup.MappingsDownloader;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Allows the searching of mcp mappings in the bot's merged format
 */
public class MappingsSearch {

    /**
     * Emoji to show when no results are found.
     */
    public static final String NO_RESULTS_EMOJI = "\uD83D\uDE2F";

    public static List<MappingsDownloader.MethodMapping> searchMethods(String method) throws IOException {
        Stream<MappingsDownloader.MethodMapping> methods = Files.readAllLines(DiscordBot.MAPPINGS.getJoinedMcpFile().toPath()).stream()
                .filter(s->s.startsWith("METHOD ")).map(MappingsDownloader.MethodMapping::fromString);

        return methods.filter(s->
                s.srg.equals(method) || s.mcp.equals(method)
        ).collect(Collectors.toList());
    }

    public static List<MappingsDownloader.FieldMapping> searchFields(String field) throws IOException {
        Stream<MappingsDownloader.FieldMapping> fields = Files.readAllLines(DiscordBot.MAPPINGS.getJoinedMcpFile().toPath()).stream()
                .filter(s->s.startsWith("FIELD ")).map(MappingsDownloader.FieldMapping::fromString);

        return fields.filter(s->
                        s.srg.equals(field) || s.mcp.equals(field)
        ).collect(Collectors.toList());
    }

    public static List<ClassMapping> searchClasses(String cls) throws IOException {
        Stream<ClassMapping> classes = Files.readAllLines(DiscordBot.MAPPINGS.getClassMappingsFile().toPath()).stream()
                .map(s-> { String[] pts = s.split(","); return new ClassMapping(pts[0], pts[1]); });

        return classes.filter(p -> p.getMcp().equals(cls)).collect(Collectors.toList());
    }

    public static class ClassMapping {
        private final String notch;
        private final String mcp;

        public ClassMapping(String notch, String mcp) {
            this.notch = notch;
            this.mcp = mcp;
        }

        public String getMcp() {
            return mcp;
        }

        public String getNotch() {
            return notch;
        }
    }
}
