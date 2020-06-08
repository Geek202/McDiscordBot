package me.geek.tom.discord.search;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.geek.tom.discord.DiscordBot;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static me.geek.tom.discord.search.AsmUtil.access;
import static org.objectweb.asm.Opcodes.ASM8;

public class MethodSearch {

    public static List<String> doSearch(String clazz) throws IOException, CommandSyntaxException {
        String filename = clazz.replace(".", "/");
        if (!filename.endsWith(".class"))
            filename = filename + ".class";
        JarFile jar = new JarFile(new File(DiscordBot.CONFIG.getForgeVersion()+".jar"));
        JarEntry entry = jar.getJarEntry(filename);
        if (entry == null)
            throw new SimpleCommandExceptionType(() -> "Failed to locate class: " + clazz).create();

        List<String> ret = new ArrayList<>();
        MVisit visit = new MVisit(ret);

        ClassReader reader = new ClassReader(jar.getInputStream(entry));
        reader.accept(visit, 0);
        return ret;
    }

    private static class MVisit extends ClassVisitor {
        private List<String> res;

        public MVisit(List<String> res) {
            super(ASM8);
            this.res = res;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            res.add(access(access) + name + descriptor);
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

}
