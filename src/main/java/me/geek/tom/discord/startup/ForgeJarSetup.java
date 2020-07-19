package me.geek.tom.discord.startup;

import me.geek.tom.discord.Logging;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.model.build.BuildEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ForgeJarSetup {

    private static final Logger LOGGER = LogManager.getLogger();

    public static File setupForge(String mappings, String forge, String mc) throws IOException {
        File mdkDir = extractMinimalMdkZip(mappings, forge, mc);
        File gradleHome = executeBuild(new File(mdkDir, "ForgeMdkMinimal"));
        assert gradleHome != null;
        String forgeVersionName = mc + "-" + forge + "_mapped_snapshot_" + mappings;
        File forgeJar = gradleHome.toPath()
                .resolve("caches")
                .resolve("forge_gradle")
                .resolve("minecraft_user_repo")
                .resolve("net")
                .resolve("minecraftforge")
                .resolve("forge")
                .resolve(forgeVersionName)
                .resolve("forge-" + forgeVersionName + ".jar")
                .toFile();

        LOGGER.info(Logging.SETUP, "Copying forge jar...");
        return copyForgeJar(forgeVersionName, forgeJar);
    }

    private static File copyForgeJar(String forgeVersionName, File forgeJar) throws IOException {
        File finalForgeJar = new File(".", forgeVersionName + ".jar");
        FileUtils.copyFile(forgeJar, finalForgeJar);
        return finalForgeJar;
    }

    private static File executeBuild(File mdkDir) {
        LOGGER.info(Logging.SETUP, "Running gradle build to generate forge JAR... ");
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(mdkDir)
                .connect()) {
            BuildEnvironment env = connection.getModel(BuildEnvironment.class);
            File gradleUserHome = env.getGradle().getGradleUserHome();
            LOGGER.info(Logging.SETUP_ENV, "Build environment:");
            LOGGER.info(Logging.SETUP_ENV, "\tJava:");
            LOGGER.info(Logging.SETUP_ENV, "\t\tJava home: " + env.getJava().getJavaHome());
            LOGGER.info(Logging.SETUP_ENV, "\tGradle:");
            LOGGER.info(Logging.SETUP_ENV, "\t\tGradle version: " + env.getGradle().getGradleVersion());
            LOGGER.info(Logging.SETUP_ENV, "\t\tGradle user home: " + env.getGradle().getGradleUserHome());

            connection.newBuild()
                    .forTasks("build")
                    .setStandardOutput(System.out)
                    .addProgressListener((ProgressListener) event ->
                            LOGGER.debug(Logging.SETUP, event.getDescriptor().getDisplayName()))
                    .run();
            return gradleUserHome;
        } catch (BuildException e) {
            LOGGER.fatal(Logging.SETUP, "Failed to build forge JAR!", e);
            return null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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
