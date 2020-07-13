package me.geek.tom.discord.startup;

import com.google.gson.Gson;
import com.mojang.brigadier.StringReader;
import me.geek.tom.discord.DiscordBot;
import me.geek.tom.discord.search.MappingsSearch;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Handles the download and merging of MCP mappings.
 */
public class MappingsDownloader {
    public static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();

    public static int mappingsCount = 0;

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

        // READ NOTCH DATA.
        List<String> notch = new ArrayList<>(Files.readAllLines(notchSrg.toPath()));

        // PARSE NOTCH DATA.
        String currentClass = null;
        List<MappingsSearch.ClassMapping> classMappings =
                notch.stream()
                        .filter(s->!s.startsWith("\t"))
                        .map(s-> { String[] pts = s.split(" "); return new MappingsSearch.ClassMapping(pts[0], pts[1]); })
                        .collect(Collectors.toList());

        Map<String, FieldMapping> incompleteFields = new HashMap<>();
        Map<String, MethodMapping> incompleteMethods = new HashMap<>();
        for (String line : notch) {
            if (!line.startsWith("\t")) { // class
                currentClass = line.split(" ")[1].replace("/", ".");
            } else { // Field or method
                if (currentClass == null)
                    throw new IOException("Not a valid TSRG file, line: " + line);
                String[] pts = line.trim().split(" ");
                if (pts.length == 2) { // FIELD
                    String obf = pts[0];
                    String srg = pts[1];
                    FieldMapping mapping = new FieldMapping();
                    mapping.notch = obf;
                    mapping.srg = srg;
                    mapping.className = currentClass;
                    incompleteFields.put(srg, mapping);
                } else { // METHOD
                    String obf = pts[0];
                    String signature = MappingsData.remapDesc(pts[1], classMappings);
                    String srg = pts[2];
                    MethodMapping mapping = new MethodMapping();
                    mapping.notch = obf;
                    mapping.srg = srg;
                    mapping.className = currentClass;
                    mapping.signature = signature;

                    incompleteMethods.put(srg, mapping);
                }
            }
        }
        classMappings.clear(); // keep it tidy.

        // PARSE MCP DATA

        // PARSE MCP METHODS
        List<String> mcpMethods = Files.readAllLines(new File(mcpDir, "methods.csv").toPath());
        List<MethodMapping> completeMethods = new ArrayList<>();
        for (String method : mcpMethods) {
            if (method.contains("searge,name,side,desc"))continue; // SKIP FIRST LINE
            String desc = method.substring(method.lastIndexOf(",")+1);
            String[] pts = method.split(",");
            String srg = pts[0];
            String mcp = pts[1];
            MethodMapping mapping = incompleteMethods.get(srg);
            if (mapping == null) continue;
            mapping.mcp = mcp;
            mapping.desc = desc;
            completeMethods.add(mapping);
        }

        // be nice to the gc and don't leave junk behind.
        mcpMethods.clear();
        incompleteMethods.clear();

        // PARSE MCP FIELDS

        List<String> mcpFields = Files.readAllLines(new File(mcpDir, "fields.csv").toPath());
        List<FieldMapping> completeFields = new ArrayList<>();
        for (String field : mcpFields) {
            if (field.contains("searge,name,side,desc"))continue; // SKIP FIRST LINE
            String desc = field.substring(field.lastIndexOf(",")+1);
            String[] pts = field.split(",");
            String srg = pts[0];
            String mcp = pts[1];
            FieldMapping mapping = incompleteFields.get(srg);
            if (mapping == null) continue;
            mapping.mcp = mcp;
            mapping.desc = desc;
            completeFields.add(mapping);
        }

        // be nice to the gc and don't leave junk behind.
        mcpFields.clear();
        incompleteFields.clear();

        List<String> outputMethods = completeMethods.stream().map(MethodMapping::toString).collect(Collectors.toList());
        List<String> outputFields = completeFields.stream().map(FieldMapping::toString).collect(Collectors.toList());
        List<String> outputLines = new ArrayList<>(outputMethods);
        outputLines.addAll(outputFields);
        mappingsCount += outputLines.size();
        FileUtils.writeLines(output, outputLines);

        return output;
    }

    private static File copyClassnames(File notchSrg, File mcpDir) throws IOException {
        List<String> classes = Files.readAllLines(notchSrg.toPath()).stream()
                .filter(s->!s.startsWith("\t")) // Only class name lines.
                .map(String::trim) // Do this after the \t check, as this would strip it.
                .filter(s->!s.isEmpty())
                .map(s->s.replace(" ", ",")) // Convert it to CSV-like data.
                .map(s->s.replace("/", ".")) // Make nicer fully-qualified class names.
                .collect(Collectors.toList());
        File out = new File(mcpDir, "classes.csv");
        mappingsCount += classes.size();
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
        public String className;
        public String signature;

        public MethodMapping() {
            notch = "<PLACE_HOLDER>";
            srg = "<PLACE_HOLDER>";
            mcp = "<PLACE_HOLDER>";
            desc = "<PLACE_HOLDER>";
            className = "<PLACE_HOLDER>";
        }

        public MethodMapping(String notch, String srg, String mcp, String desc, String className, String signature) {
            this.notch = notch;
            this.srg = srg;
            this.mcp = mcp;
            this.desc = desc;
            this.className = className;
            this.signature = signature;
        }

        public static MethodMapping fromString(String line) {
            StringReader reader = new StringReader(line.replaceFirst("METHOD ", ""));

            int start = reader.getCursor();
            while (reader.canRead() && !(reader.peek() == ','))
                reader.skip();
            String classname = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            start = reader.getCursor();
            while (reader.canRead() && !(reader.peek() == ','))
                reader.skip();
            String notch = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            start = reader.getCursor();
            while (reader.canRead() && !(reader.peek() == ','))
                reader.skip();
            String srg = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            start = reader.getCursor();
            while (reader.canRead() && !(reader.peek() == ','))
                reader.skip();
            String mcp = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            start = reader.getCursor();
            while (reader.canRead() && !(reader.peek() == ','))
                reader.skip();
            String signature = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            String desc = reader.getRemaining();

            return new MethodMapping(notch, srg, mcp, desc.equals("<PLACE_HOLDER>") ? "" : desc, classname, signature);
        }

        @Override
        public String toString() {
            return "METHOD " + className + "," + notch + "," + srg + "," + mcp + "," + signature + "," + desc;
        }

        public String generateATLine() {
            return "AT: `public " + className + " " + srg + signature + " #" + mcp + "`"; // public <class> <srg> #<mcp>
        }

        public String toNiceString() {
            String descStr = desc.equals("") ? "\n" : "\nDescription: `"+desc.trim()+"`\n";
            return "Class: `"+className+"`\nMethod: `"+notch+"` -> `"+srg+"` -> `"+mcp+"`"+descStr+"\n"+generateATLine();
        }
    }

    public static class FieldMapping {
        public String notch;
        public String srg;
        public String mcp;
        public String desc;
        public String className;

        public FieldMapping() {
            notch = "<PLACE_HOLDER>";
            srg = "<PLACE_HOLDER>";
            mcp = "<PLACE_HOLDER>";
            desc = "<PLACE_HOLDER>";
            className = "<PLACE_HOLDER>";
        }

        public static FieldMapping fromString(String line) {
            StringReader reader = new StringReader(line.replaceFirst("FIELD ", ""));

            int start = reader.getCursor();
            while (reader.canRead() && !(reader.peek() == ','))
                reader.skip();
            String classname = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            start = reader.getCursor();
            while (reader.canRead() && !(reader.peek() == ','))
                reader.skip();
            String notch = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            start = reader.getCursor();
            while (reader.canRead() && !(reader.peek() == ','))
                reader.skip();
            String srg = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            start = reader.getCursor();
            while (reader.canRead() && !(reader.peek() == ','))
                reader.skip();
            String mcp = reader.getString().substring(start, reader.getCursor());
            reader.skip();

            String desc = reader.getRemaining();

            return new FieldMapping(notch, srg, mcp, desc.equals("<PLACE_HOLDER>") ? "" : desc, classname);
        }

        public FieldMapping(String notch, String srg, String mcp, String desc, String className) {
            this.notch = notch;
            this.srg = srg;
            this.mcp = mcp;
            this.desc = desc;
            this.className = className;
        }

        @Override
        public String toString() {
            return "FIELD " + className + "," + notch + "," + srg + "," + mcp + "," + desc;
        }

        public String generateATLine() {
            return "AT: `public " + className + " " + srg + " #" + mcp + "`"; // public <class> <srg> #<mcp>
        }

        public String toNiceString() {
            String descStr = desc.equals("") ? "\n" : "\nDescription: `"+desc.trim()+"`\n";
            return "Class: `"+className+"`\nField: `"+notch+"` -> `"+srg+"` -> `"+mcp+"`"+descStr+"\n"+generateATLine();
        }
    }

    public static class MappingsData {
        private final File classMappingsFile;
        private final File joinedMcpFile;
        private final File mcpParamData;

        private static Pattern CLASS = Pattern.compile("L(?<cls>[^;]+);");

        public static String remapDesc(String desc, List<MappingsSearch.ClassMapping> mappings) {
            Matcher matcher = CLASS.matcher(desc);

            StringBuffer buf = new StringBuffer();

            while (matcher.find()) {
                String cls = matcher.group("cls");
                List<MappingsSearch.ClassMapping> newCls = mappings.stream().filter(m->m.getNotch().equals(cls)).collect(Collectors.toList());

                matcher.appendReplacement(buf, Matcher.quoteReplacement("L" + (newCls.isEmpty() ? cls : newCls.get(0).getMcp()) + ";"));
            }
            matcher.appendTail(buf);
            return buf.toString();
        }

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
