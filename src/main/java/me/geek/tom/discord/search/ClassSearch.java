package me.geek.tom.discord.search;

import me.geek.tom.discord.DiscordBot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Searches for classes in a Jar
 */
public class ClassSearch {

    public static List<String> doSearch(String term) throws IOException {
        JarFile jar = new JarFile(new File(DiscordBot.CONFIG.getForgeVersion()+".jar"));
        List<String> ret = new ArrayList<>();

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.getName().endsWith(".class")) continue;
            if (entry.getName().replace(".class", "").contains(term))
                ret.add(entry.getName().replace(".class", "").replace("/", "."));
        }
        return ret;
    }

}
