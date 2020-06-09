package me.geek.tom.discord.search;

import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.startup.MappingsDownloader;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MappingsSearch {

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
}
