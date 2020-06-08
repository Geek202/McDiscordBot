package me.geek.tom.discord.startup;

import com.google.gson.Gson;
import me.geek.tom.discord.DiscordBot;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MappingsDownloader {
    public static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();

    public static MappingsData setupMcp() throws IOException {
        LOGGER.info("Setting up MCP mapping data...");
        LOGGER.info("Downloading MCPConfig for Minecraft version " + DiscordBot.CONFIG.getForgeMcVersion());
        File mcpConfig = downloadConfig();
        assert mcpConfig != null; // This should never fail, but we have it here just in case.
        LOGGER.info("MCPConfig downloaded to "+mcpConfig);
        LOGGER.info("Downloading MCP CSV names version "+DiscordBot.CONFIG.getForgeMappings()+"...");
        File mappingsZip = downloadMappings();
        assert mappingsZip != null; // again, this should also never fail.
        LOGGER.info("Downloaded MCP CSV zip to "+mappingsZip);
        LOGGER.info("Extracting NOTCH->SRG tsrg...");
        File notchSrg = extractNotchSrgTsrg(mcpConfig);
        LOGGER.info("Extracted NOTCH->SRG mappings to "+notchSrg);
        LOGGER.info("Extracting MCP names...");
        File mcpDir = extractMcp(mappingsZip);
        LOGGER.info("Extracted MCP names to "+mcpDir);
        LOGGER.info("Merging mappings into a joined file");
        MappingsData data = mergeMappings(notchSrg, mcpDir);
        LOGGER.info("Mappings have been sucessfully merged!");
        LOGGER.info("Mappings data collected successfully, setup is complete!");
        return data;
    }

    private static MappingsData mergeMappings(File notchSrg, File mcpDir) throws IOException {
        File classes = copyClassnames(notchSrg, mcpDir);
        File mcpMerged = mergeMcpData(notchSrg, mcpDir);
        return new MappingsData(classes, mcpMerged, new File(mcpDir, "params.csv"));
    }

    private static File mergeMcpData(File notchSrg, File mcpDir) throws IOException {
        File output = new File("mappings", "mcp_joined_"+DiscordBot.CONFIG.getForgeMappings()+".csv");

        // READ NOTCH
        List<String> notch = Files.readAllLines(notchSrg.toPath()).stream()
                .filter(s->s.startsWith("\t"))
                .map(String::trim)
                .collect(Collectors.toList());

        // READ METHODS
        List<String> mcpMethods = Files.readAllLines(new File(mcpDir, "methods.csv").toPath());
        Map<String, MethodMapping> incompleteMethods = new HashMap<>();
        for (String method : mcpMethods) {
            String[] pts = method.split(",");
            String srg = pts[0];
            if (srg.equals("searge")) continue; // Skip first line.
            String mcp = pts[1];
            MethodMapping map = new MethodMapping();
            String desc = map.desc;
            try {
                desc = pts[3];
            } catch (ArrayIndexOutOfBoundsException ignored) {
            }
            map.desc = desc;
            map.srg = srg;
            map.mcp = mcp;
            incompleteMethods.put(srg, map);
        }
        mcpMethods.clear(); // be nice to the gc and don't leave junk behind.

        List<MethodMapping> completeMethods = new ArrayList<>();
        // ADD NOTCH DATA TO METHODS
        notch.stream().filter(s->s.split(" ").length==3)
                .forEach(s -> {
                    String[] pts = s.split(" ");
                    String obf = pts[0];
                    String sig = pts[1];
                    String srg = pts[2];

                    MethodMapping m = incompleteMethods.get(srg);
                    if (m != null) {
                        m.signature = sig;
                        m.notch = obf;
                        incompleteMethods.remove(srg);
                        completeMethods.add(m);
                    }
                });

        incompleteMethods.clear(); // dont have lots of junk, clean up.

        // READ FIELDS

        List<String> mcpFields = Files.readAllLines(new File(mcpDir, "methods.csv").toPath());
        Map<String, FieldMapping> incompleteFields = new HashMap<>();
        for (String field : mcpFields) {
            String[] pts = field.split(",");
            String srg = pts[0];
            if (srg.equals("searge")) continue; // Skip first line.
            String mcp = pts[1];
            FieldMapping map = new FieldMapping();
            String desc = map.desc;
            try {
                desc = pts[3];
            } catch (ArrayIndexOutOfBoundsException ignored) {
            }
            map.desc = desc;
            map.srg = srg;
            map.mcp = mcp;
            incompleteFields.put(srg, map);
        }
        mcpFields.clear();

        List<FieldMapping> completeFields = new ArrayList<>();
        notch.stream().filter(s->s.split(" ").length==2).forEach(s -> {
            String[] pts = s.split(" ");
            String obf = pts[0];
            String srg = pts[1];
            FieldMapping m = incompleteFields.get(srg);
            if (m != null) {
                m.notch = obf;
                completeFields.add(m);
            }
        });

        List<String> outputMethods = completeMethods.stream().map(MethodMapping::toString).collect(Collectors.toList());
        List<String> outputFields = completeFields.stream().map(FieldMapping::toString).collect(Collectors.toList());
        List<String> outputLines = new ArrayList<>(outputMethods);
        outputLines.addAll(outputFields);
        FileUtils.writeLines(output, outputLines);

        return output;
    }

    private static File copyClassnames(File notchSrg, File mcpDir) throws IOException {
        List<String> classes = Files.readAllLines(notchSrg.toPath()).stream()
                .filter(s->!s.startsWith("\t")) // Only class name lines
                .map(String::trim) // Do this after the \t check, as this would strip it
                .filter(s->!s.isEmpty())
                .map(s->s.replace(" ", ",")) // Convert it to CSV-like data
                .collect(Collectors.toList());
        File out = new File(mcpDir, "classes.csv");
        FileUtils.writeLines(out, classes);
        return out;
    }

    private static File extractMcp(File mappingsZip) throws IOException {
        ZipFile zip = new ZipFile(mappingsZip);
        File outputDir = new File("mappings", DiscordBot.CONFIG.getForgeMappings());
        String[] mcp = new String[] { "fields.csv", "methods.csv", "params.csv" };
        for (String name : mcp) {
            ZipEntry entry = zip.getEntry(name);
            assert entry != null; // WTF is going on if this fails.
            InputStream in = zip.getInputStream(entry);
            FileUtils.copyInputStreamToFile(in, new File(outputDir, name));
            in.close();
        }
        zip.close();
        return outputDir;
    }

    private static File extractNotchSrgTsrg(File mcpConfig) throws IOException {
        ZipFile zip = new ZipFile(mcpConfig);
        ZipEntry entry = zip.getEntry("config/joined.tsrg");
        assert entry != null; // not a valid MCPConfig? WTF is happening rn.
        InputStream in = zip.getInputStream(entry);
        File notchSrg = new File("mappings", "notchToSrg-"+DiscordBot.CONFIG.getForgeMcVersion()+".tsrg");
        FileUtils.copyInputStreamToFile(in, notchSrg);
        in.close();
        zip.close();
        return notchSrg;
    }

    private static File downloadConfig() throws IOException {
        String MC_VERSION = DiscordBot.CONFIG.getForgeMcVersion();
        File mcpConfig = new File("mappings", "mcpconfig_"+MC_VERSION+".zip");
        try {
            FileUtils.copyURLToFile(
                    new URL("https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/"+MC_VERSION+"/mcp_config-"+MC_VERSION+".zip"),
                    mcpConfig
            );
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        return mcpConfig;
    }
    
    private static File downloadMappings() throws IOException {
        String MCP_VERSION = DiscordBot.CONFIG.getForgeMappings();
        String MC_VERSION = DiscordBot.CONFIG.getForgeMcVersion();
        File mappings = new File("mappings", "mcp_snapshot-"+MCP_VERSION+".zip");
        try {
            URL url = new URL("https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_snapshot/"+MCP_VERSION+"/mcp_snapshot-"+MCP_VERSION+".zip");
            FileUtils.copyURLToFile(url, mappings);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        return mappings;
    }

    public static class MethodMapping {
        public String notch;
        public String srg;
        public String mcp;
        public String desc;
        public String signature;

        public MethodMapping() {
            notch = "<PLACE_HOLDER>";
            srg = "<PLACE_HOLDER>";
            mcp = "<PLACE_HOLDER>";
            desc = "<PLACE_HOLDER>";
            signature = "<PLACE_HOLDER>";
        }

        public MethodMapping(String notch, String srg, String mcp, String desc, String signature) {
            this.notch = notch;
            this.srg = srg;
            this.mcp = mcp;
            this.desc = desc;
            this.signature = signature;
        }

        @Override
        public String toString() {
            return notch + "," + srg + "," + mcp + "," + desc + "," + signature;
        }
    }

    public static class FieldMapping {
        public String notch;
        public String srg;
        public String mcp;
        public String desc;

        public FieldMapping() {
            notch = "<PLACE_HOLDER>";
            srg = "<PLACE_HOLDER>";
            mcp = "<PLACE_HOLDER>";
            desc = "<PLACE_HOLDER>";
        }

        public FieldMapping(String notch, String srg, String mcp, String desc) {
            this.notch = notch;
            this.srg = srg;
            this.mcp = mcp;
            this.desc = desc;
        }

        @Override
        public String toString() {
            return notch + "," + srg + "," + mcp + "," + desc;
        }
    }

    public static class MappingsData {
        private final File classMappingsFile;
        private final File joinedMcpFile;
        private final File mcpParamData;

        public MappingsData(File classMappingsFile, File joinedMcpFile, File mcpParamData) {
            this.classMappingsFile = classMappingsFile;
            this.joinedMcpFile = joinedMcpFile;
            this.mcpParamData = mcpParamData;
        }

        public File getClassMappingsFile() {
            return classMappingsFile;
        }

        public File getJoinedMcpFile() {
            return joinedMcpFile;
        }

        public File getMcpParamData() {
            return mcpParamData;
        }
    }
}
