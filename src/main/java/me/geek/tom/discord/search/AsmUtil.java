package me.geek.tom.discord.search;

import static org.objectweb.asm.Opcodes.*;

public class AsmUtil {

    public static String access(int access) {
        StringBuilder builder = new StringBuilder();

        if ((access & ACC_PRIVATE) != 0)
            builder.append("private ");
        if ((access & ACC_PROTECTED) != 0)
            builder.append("protected ");
        if ((access & ACC_PUBLIC) != 0)
            builder.append("public ");
        if ((access & ACC_STATIC) != 0)
            builder.append("static ");
        if ((access & ACC_ABSTRACT) != 0)
            builder.append("abstract ");
        if ((access & ACC_FINAL) != 0)
            builder.append("final ");
        if ((access & ACC_NATIVE) != 0)
            builder.append("native ");

        return builder.toString();
    }

}
