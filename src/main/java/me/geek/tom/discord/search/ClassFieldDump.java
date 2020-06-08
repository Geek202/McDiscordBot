package me.geek.tom.discord.search;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.geek.tom.discord.DiscordBot;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.objectweb.asm.Opcodes.ASM8;

public class ClassFieldDump {

    public static List<String> get(String clazz) throws IOException, CommandSyntaxException {
        String filename = clazz.replace(".", "/");
        if (!filename.endsWith(".class"))
            filename = filename + ".class";
        JarFile jar = new JarFile(new File(DiscordBot.CONFIG.getForgeVersion()+".jar"));
        JarEntry entry = jar.getJarEntry(filename);
        if (entry == null)
            throw new SimpleCommandExceptionType(() -> "Failed to locate class: " + clazz).create();

        List<String> ret = new ArrayList<>();
        FVisit visit = new FVisit(ret);

        ClassReader reader = new ClassReader(jar.getInputStream(entry));
        reader.accept(visit, 0);
        return ret;
    }

    private static class FVisit extends ClassVisitor {
        private final List<String> res;

        private FVisit(List<String> res) {
            super(ASM8);
            this.res = res;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            res.add(AsmUtil.access(access) + descriptor + " " + name);

            return super.visitField(access, name, descriptor, signature, value);
        }
    }

}
