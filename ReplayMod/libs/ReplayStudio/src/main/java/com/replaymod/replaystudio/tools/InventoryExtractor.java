package com.replaymod.replaystudio.tools;

import com.github.steveice10.opennbt.tag.builtin.*;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketType;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;

import com.google.gson.JsonArray;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.util.Scanner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class InventoryExtractor {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("用法: java -jar inventory-tool.jar <mcpr路径> [时间] [版本]");
            return;
        }
        File mcpr = new File(args[0]);
        if (!mcpr.exists()) {
            System.out.println("文件不存在: " + mcpr.getAbsolutePath());
            return;
        }

        long timestampArg = -1;
        String mcVersion = null;
        if (args.length >= 2) {
            timestampArg = parseTimestamp(args[1]);
            if (timestampArg >= 0) {
                System.out.println("将在时间戳 " + timestampArg + "ms 处停止提取");
            } else {
                mcVersion = args[1];
            }
        }
        if (args.length >= 3) {
            mcVersion = args[2];
        }

        ensureRegistries(mcVersion);

        final long targetTimestamp = timestampArg;
        ReplayStudio studio = new ReplayStudio();
        try (ZipReplayFile replayFile = new ZipReplayFile(studio, mcpr)) {
            ReplayMetaData meta = replayFile.getMetaData();
            ProtocolVersion protocol = meta.getProtocolVersion();
            PacketTypeRegistry registry = PacketTypeRegistry.get(protocol, State.PLAY);
            Map<Integer, ItemStack> inventory = new LinkedHashMap<>();
            ItemIdMapResult itemIdMapResult = loadItemIdMap(mcpr);
            Map<Integer, String> itemIdMap = itemIdMapResult.map;
            long lastTime = 0;
            try (ReplayInputStream in = replayFile.getPacketData(registry)) {
                while (true) {
                    PacketData data = in.readPacket();
                    if (data == null) {
                        break;
                    }
                    Packet packet = data.getPacket();
                    lastTime = data.getTime();

                    if (targetTimestamp >= 0 && lastTime > targetTimestamp) {
                        break;
                    }

                    if (packet.getType() == PacketType.WindowItems) {
                        readWindowItems(packet, inventory);
                    } else if (packet.getType() == PacketType.SetSlot) {
                        readSetSlot(packet, inventory);
                    }
                }
            }
            File out;
            if (targetTimestamp >= 0) {
                out = new File(mcpr.getParentFile(), mcpr.getName() + "." + targetTimestamp + ".inventory.json");
            } else {
                out = new File(mcpr.getParentFile(), mcpr.getName() + ".inventory.json");
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("file", mcpr.getAbsolutePath());
            result.put("protocol", protocol.getVersion());
            result.put("lastTime", lastTime);
            Map<String, Object> items = new LinkedHashMap<>();
            for (Map.Entry<Integer, ItemStack> entry : inventory.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                items.put(String.valueOf(entry.getKey()), entry.getValue().toMap(itemIdMap));
            }
            result.put("inventory", items);
            if (itemIdMapResult.source != null) {
                result.put("itemRegistrySource", itemIdMapResult.source);
            }
            try (FileWriter writer = new FileWriter(out)) {
                gson.toJson(result, writer);
            }
            System.out.println("已生成: " + out.getAbsolutePath());
        }
    }

    private static void ensureRegistries(String requestedVersion) {
        File existing = findRegistriesJson(new File("."));
        if (existing != null) {
            System.out.println("Using existing registries: " + existing.getAbsolutePath());
            return;
        }

        try {
            System.out.println("Fetching available versions...");
            String versionListResponse = httpRequest("https://api.mslmc.cn/v3/query/available_versions/vanilla");
            JsonObject versionData = new JsonParser().parse(versionListResponse).getAsJsonObject();
            JsonArray versions = versionData.getAsJsonObject("data").getAsJsonArray("versionList");
            
            String versionToUse = requestedVersion;
            if (versionToUse == null || versionToUse.isEmpty()) {
                versionToUse = "1.20.1";
                System.out.println("No version specified. Defaulting to " + versionToUse);
            }
            
            boolean versionFound = false;
            for (JsonElement v : versions) {
                if (v.getAsString().equals(versionToUse)) {
                    versionFound = true;
                    break;
                }
            }
            
            if (!versionFound) {
                System.out.println("Version " + versionToUse + " not found in available versions. Trying anyway...");
            }

            System.out.println("Fetching download URL for version " + versionToUse + "...");
            String downloadResponse = httpRequest("https://api.mslmc.cn/v3/download/server/vanilla/" + versionToUse);
            JsonObject downloadData = new JsonParser().parse(downloadResponse).getAsJsonObject();
            String serverUrl = downloadData.getAsJsonObject("data").get("url").getAsString();

            File serverJar = new File("server.jar");
            if (!serverJar.exists()) {
                System.out.println("Downloading server.jar from " + serverUrl + "...");
                downloadFile(serverUrl, serverJar);
            } else {
                 System.out.println("server.jar already exists, skipping download.");
            }

            File eula = new File("eula.txt");
            if (!eula.exists()) {
                System.out.println("Creating eula.txt...");
                try (FileWriter w = new FileWriter(eula)) {
                    w.write("eula=true");
                }
            }

            System.out.println("Generating registries data...");
            ProcessBuilder pb = new ProcessBuilder("java", "-DbundlerMainClass=net.minecraft.data.Main", "-jar", serverJar.getAbsolutePath(), "--reports");
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Data generator failed with exit code " + exitCode);
            } else {
                System.out.println("Registries generated successfully.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to auto-download registries: " + e.getMessage());
        }
    }

    private static String httpRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    private static long parseTimestamp(String input) {
        if (input == null) {
            return -1;
        }
        String value = input.trim();
        if (value.isEmpty()) {
            return -1;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
        }
        String[] parts = value.split(":");
        if (parts.length == 2 || parts.length == 3) {
            try {
                int hours = 0;
                int minutes;
                int seconds;
                if (parts.length == 2) {
                    minutes = Integer.parseInt(parts[0]);
                    seconds = Integer.parseInt(parts[1]);
                } else {
                    hours = Integer.parseInt(parts[0]);
                    minutes = Integer.parseInt(parts[1]);
                    seconds = Integer.parseInt(parts[2]);
                }
                if (minutes < 0 || seconds < 0 || seconds >= 60 || minutes >= 60) {
                    return -1;
                }
                return (hours * 3600L + minutes * 60L + seconds) * 1000L;
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    private static void downloadFile(String urlString, File dest) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private static void readWindowItems(Packet packet, Map<Integer, ItemStack> inventory) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            int windowId = in.readUnsignedByte();
            if (packet.atLeast(ProtocolVersion.v1_17_1)) {
                in.readVarInt();
            }
            int count = in.readVarInt();
            if (windowId != 0) {
                for (int i = 0; i < count; i++) {
                    readItemStack(packet, in);
                }
                if (packet.atLeast(ProtocolVersion.v1_17_1)) {
                    readItemStack(packet, in);
                }
                return;
            }
            for (int i = 0; i < count; i++) {
                ItemStack stack = readItemStack(packet, in);
                if (stack == null) {
                    inventory.remove(i);
                } else {
                    inventory.put(i, stack);
                }
            }
            if (packet.atLeast(ProtocolVersion.v1_17_1)) {
                readItemStack(packet, in);
            }
        }
    }

    private static void readSetSlot(Packet packet, Map<Integer, ItemStack> inventory) throws IOException {
        try (Packet.Reader in = packet.reader()) {
            int windowId = in.readUnsignedByte();
            if (packet.atLeast(ProtocolVersion.v1_17_1)) {
                in.readVarInt();
            }
            int slot = in.readShort();
            ItemStack stack = readItemStack(packet, in);
            if (windowId != 0 || slot < 0) {
                return;
            }
            if (stack == null) {
                inventory.remove(slot);
            } else {
                inventory.put(slot, stack);
            }
        }
    }

    private static ItemStack readItemStack(Packet packet, Packet.Reader in) throws IOException {
        if (packet.atLeast(ProtocolVersion.v1_13)) {
            boolean present = in.readBoolean();
            if (!present) {
                return null;
            }
            int itemId = in.readVarInt();
            int count = in.readByte();
            CompoundTag tag = in.readNBT();
            return new ItemStack(itemId, count, tag, null);
        } else {
            short itemId = in.readShort();
            if (itemId < 0) {
                return null;
            }
            int count = in.readByte();
            short damage = in.readShort();
            CompoundTag tag = in.readNBT();
            return new ItemStack(itemId, count, tag, (int) damage);
        }
    }

    private static class ItemStack {
        private final int itemId;
        private final int count;
        private final CompoundTag tag;
        private final Integer damage;

        private ItemStack(int itemId, int count, CompoundTag tag, Integer damage) {
            this.itemId = itemId;
            this.count = count;
            this.tag = tag;
            this.damage = damage;
        }

        private Map<String, Object> toMap(Map<Integer, String> itemIdMap) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("itemId", itemId);
            String name = itemIdMap.get(itemId);
            if (name != null) {
                map.put("name", name);
            }
            map.put("count", count);
            if (damage != null) {
                map.put("damage", damage);
            }
            if (tag != null) {
                map.put("nbt", toSnbt(tag));
            }
            return map;
        }

        private String toSnbt(Tag tag) {
            if (tag instanceof CompoundTag) {
                StringBuilder sb = new StringBuilder("{");
                Map<String, Tag> value = ((CompoundTag) tag).getValue();
                boolean first = true;
                for (Map.Entry<String, Tag> entry : value.entrySet()) {
                    if (!first) sb.append(",");
                    sb.append(entry.getKey()).append(":").append(toSnbt(entry.getValue()));
                    first = false;
                }
                sb.append("}");
                return sb.toString();
            } else if (tag instanceof ListTag) {
                StringBuilder sb = new StringBuilder("[");
                List<Tag> value = ((ListTag) tag).getValue();
                boolean first = true;
                for (Tag t : value) {
                    if (!first) sb.append(",");
                    sb.append(toSnbt(t));
                    first = false;
                }
                sb.append("]");
                return sb.toString();
            } else if (tag instanceof StringTag) {
                String val = ((StringTag) tag).getValue();
                return "\"" + val.replace("\"", "\\\"") + "\"";
            } else if (tag instanceof ByteTag) {
                return ((ByteTag) tag).getValue() + "b";
            } else if (tag instanceof ShortTag) {
                return ((ShortTag) tag).getValue() + "s";
            } else if (tag instanceof IntTag) {
                return ((IntTag) tag).getValue().toString();
            } else if (tag instanceof LongTag) {
                return ((LongTag) tag).getValue() + "L";
            } else if (tag instanceof FloatTag) {
                return ((FloatTag) tag).getValue() + "f";
            } else if (tag instanceof DoubleTag) {
                return ((DoubleTag) tag).getValue() + "d";
            } else if (tag instanceof ByteArrayTag) {
                byte[] val = ((ByteArrayTag) tag).getValue();
                StringBuilder sb = new StringBuilder("[B;");
                for (int i = 0; i < val.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(val[i]).append("b");
                }
                sb.append("]");
                return sb.toString();
            } else if (tag instanceof IntArrayTag) {
                int[] val = ((IntArrayTag) tag).getValue();
                StringBuilder sb = new StringBuilder("[I;");
                for (int i = 0; i < val.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(val[i]);
                }
                sb.append("]");
                return sb.toString();
            } else if (tag instanceof LongArrayTag) {
                long[] val = ((LongArrayTag) tag).getValue();
                StringBuilder sb = new StringBuilder("[L;");
                for (int i = 0; i < val.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append(val[i]).append("L");
                }
                sb.append("]");
                return sb.toString();
            }
            return "";
        }
    }

    private static ItemIdMapResult loadItemIdMap(File mcpr) {
        Map<Integer, String> map = new LinkedHashMap<>();
        String source = null;
        File versionDir = mcpr.getParentFile().getParentFile();
        if (versionDir == null || !versionDir.exists()) {
            return new ItemIdMapResult(map, source);
        }
        File registriesJson = findRegistriesJson(versionDir);
        if (registriesJson != null) {
            try {
                map.putAll(readItemRegistryJson(registriesJson));
                if (!map.isEmpty()) {
                    source = registriesJson.getAbsolutePath();
                    return new ItemIdMapResult(map, source);
                }
            } catch (Exception ignored) {
            }
        }
        File[] jars = versionDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            return new ItemIdMapResult(map, source);
        }
        File jar = jars[0];
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry("data/minecraft/registries.json");
            if (entry == null) {
                return new ItemIdMapResult(map, source);
            }
            try (InputStreamReader reader = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                map.putAll(readItemRegistryJson(reader));
            }
        } catch (Exception ignored) {
            return new ItemIdMapResult(map, source);
        }
        if (!map.isEmpty()) {
            source = jar.getAbsolutePath();
        }
        return new ItemIdMapResult(map, source);
    }

    private static File findRegistriesJson(File versionDir) {
        File direct = new File(versionDir, "registries.json");
        if (direct.exists()) {
            return direct;
        }
        File reports = new File(versionDir, "reports/registries.json");
        if (reports.exists()) {
            return reports;
        }
        File generated = new File(versionDir, "generated/reports/registries.json");
        if (generated.exists()) {
            return generated;
        }
        File workingDir = new File(System.getProperty("user.dir", "."));
        for (File current = workingDir; current != null; current = current.getParentFile()) {
            File candidate = new File(current, "generated/reports/registries.json");
            if (candidate.exists()) {
                return candidate;
            }
        }
        return null;
    }

    private static Map<Integer, String> readItemRegistryJson(File jsonFile) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(new java.io.FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
            return readItemRegistryJson(reader);
        }
    }

    private static Map<Integer, String> readItemRegistryJson(InputStreamReader reader) {
        Map<Integer, String> map = new LinkedHashMap<>();
        JsonElement rootElement = new JsonParser().parse(reader);
        if (!rootElement.isJsonObject()) {
            return map;
        }
        JsonObject root = rootElement.getAsJsonObject();
        JsonObject itemRegistry = root.getAsJsonObject("minecraft:item");
        if (itemRegistry == null) {
            return map;
        }
        JsonObject entries = itemRegistry.getAsJsonObject("entries");
        if (entries == null) {
            return map;
        }
        for (Map.Entry<String, JsonElement> entryItem : entries.entrySet()) {
            JsonObject value = entryItem.getValue().getAsJsonObject();
            if (value == null || !value.has("protocol_id")) {
                continue;
            }
            int id = value.get("protocol_id").getAsInt();
            map.put(id, entryItem.getKey());
        }
        return map;
    }

    private static class ItemIdMapResult {
        private final Map<Integer, String> map;
        private final String source;

        private ItemIdMapResult(Map<Integer, String> map, String source) {
            this.map = map;
            this.source = source;
        }
    }
}
