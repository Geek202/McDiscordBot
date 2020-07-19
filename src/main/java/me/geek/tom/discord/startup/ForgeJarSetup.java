package me.geek.tom.discord.startup;

import me.geek.tom.discord.Logging;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ForgeJarSetup {

    private static final Logger LOGGER = LogManager.getLogger();

    public static String setupForge(String mappings, String forge, String mc) throws IOException {
        File mdkDir = extractMinimalMdkZip(mappings, forge, mc);
        executeBuild(new File(mdkDir, "ForgeMdkMinimal"));
        //copyForgeJar();
        return "";
    }

    private static void executeBuild(File mdkDir) {
        LOGGER.info(Logging.SETUP, "Running gradle build to generate forge JAR... ");
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(mdkDir)
                .connect()) {
            connection.newBuild().forTasks("build").run();
        } catch (BuildException e) {
            LOGGER.error(Logging.SETUP, "Failed to build forge JAR!", e);
        }
    }

    private static File extractMinimalMdkZip(String mappings, String forge, String mc) throws IOException {
        LOGGER.info(Logging.SETUP, "Extracting MinimalMdk zip...");
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
            if (entry.isDirectory()) {
                File dir = new File(output, entry.getName());
                if (!dir.exists())
                    dir.mkdirs();
            } else {
                in = zip.getInputStream(entry);
                File o = new File(output, entry.getName());
                if (!o.getParentFile().exists())
                    o.getParentFile().mkdirs();
                FileUtils.copyInputStreamToFile(in, o);
            }
        }
        zip.close();


        String content = FileUtils.readFileToString(new File(output, "ForgeMdkMinimal/build.gradle"), Charset.defaultCharset());
        content = content.replace("$$MAPPINGS$$", mappings)
                         .replace("$$MCVER$$", mc)
                         .replace("$$FORGEVER$$", forge);
        FileUtils.writeStringToFile(new File(output, "ForgeMdkMinimal/build.gradle"), content, Charset.defaultCharset());

        LOGGER.info(Logging.SETUP, "Extracted to " + output);
        return output;
    }

}
