package me.geek.tom.discord.startup;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ForgeJarSetup {

    public static String setupForge() throws IOException {
        File mdkDir = extractMinimalMdkZip();
        System.out.println(mdkDir);
        //executeBuild();
        //copyForgeJar();
        return "";
    }

    private static File extractMinimalMdkZip() throws IOException {
        File output = new File(".");
        if (!output.exists())
            output.mkdirs();
        InputStream in = ForgeJarSetup.class.getClassLoader().getResourceAsStream("ForgeMdkMinimal.zip");
        Objects.requireNonNull(in);
        File tmp = new File("MdkTmp.zip");
        FileUtils.copyInputStreamToFile(in, tmp);
        ZipFile zip = new ZipFile(tmp);
        Enumeration<? extends ZipEntry> e = zip.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            in = zip.getInputStream(entry);
            File o = new File(output, entry.getName());
            if (!o.getParentFile().exists())
                o.getParentFile().mkdirs();
            FileUtils.copyInputStreamToFile(in, o);
        }
        zip.close();
        return output;
    }

}
