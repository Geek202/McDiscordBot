package me.geek.tom.discord.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static me.geek.tom.discord.DiscordBot.FORGE_VERSION;

public class ClassSearch {

    public static List<String> doSearch(String term) throws IOException {
        JarFile jar = new JarFile(new File(FORGE_VERSION+".jar"));
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
