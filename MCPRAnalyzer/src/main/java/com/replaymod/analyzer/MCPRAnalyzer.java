package com.replaymod.analyzer;

import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.PacketTypeRegistry;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * MCPR文件分析器 - 用于分析ReplayMod生成的录像文件
 * 主要功能：提取玩家背包数据、分析游戏状态、导出有用信息
 */
public class MCPRAnalyzer {
    private static final Logger logger = LogManager.getLogger(MCPRAnalyzer.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final Path mcprFile;
    private ReplayFile replayFile;
    private PacketTypeRegistry registry;
    
    // 存储提取的数据
    private Map<String, PlayerData> playerDataMap = new HashMap<>();
    private List<InventorySnapshot> inventorySnapshots = new ArrayList<>();
    
    public MCPRAnalyzer(Path mcprFile) {
        this.mcprFile = mcprFile;
    }
    
    /**
     * 主要的分析方法
     */
    public void analyze() throws Exception {
        logger.info("开始分析MCPR文件: {}", mcprFile);
        
        // 打开MCPR文件
        openReplayFile();
        
        // 分析元数据
        analyzeMetadata();
        
        // 分析数据包
        analyzePackets();
        
        // 生成报告
        generateReport();
        
        logger.info("分析完成");
    }
    
    /**
     * 打开MCPR文件
     */
    private void openReplayFile() throws Exception {
        logger.info("正在打开MCPR文件...");
        
        // 创建ReplayFile实例
        File file = mcprFile.toFile();
        if (!file.exists()) {
            throw new FileNotFoundException("MCPR文件不存在: " + mcprFile);
        }
        
        // 这里需要使用ReplayMod的API来打开文件
        // 由于ReplayStudio库是核心依赖，我们需要通过它来读取
        replayFile = new ReplayFile(file);
        registry = PacketTypeRegistry.getDefaultRegistry(State.PLAY);
        
        logger.info("MCPR文件打开成功");
    }
    
    /**
     * 分析元数据
     */
    private void analyzeMetadata() throws Exception {
        logger.info("正在分析元数据...");
        
        if (replayFile == null) {
            throw new IllegalStateException("Replay文件未打开");
        }
        
        // 获取元数据
        var metaData = replayFile.getMetaData();
        
        logger.info("录制时间: {}", new Date(metaData.getDate()));
        logger.info("Minecraft版本: {}", metaData.getMcVersion());
        logger.info("协议版本: {}", metaData.getProtocolVersion());
        logger.info("录制时长: {}秒", metaData.getDuration() / 1000);
        
        // 保存元数据到JSON文件
        Path metadataPath = mcprFile.getParent().resolve("metadata.json");
        try (FileWriter writer = new FileWriter(metadataPath.toFile())) {
            gson.toJson(Map.of(
                "date", metaData.getDate(),
                "mcVersion", metaData.getMcVersion(),
                "protocolVersion", metaData.getProtocolVersion(),
                "duration", metaData.getDuration(),
                "fileFormat", metaData.getFileFormat(),
                "generator", metaData.getGenerator()
            ), writer);
        }
        
        logger.info("元数据已保存到: {}", metadataPath);
    }
    
    /**
     * 分析数据包，提取玩家背包信息
     */
    private void analyzePackets() throws Exception {
        logger.info("正在分析数据包...");
        
        if (replayFile == null) {
            throw new IllegalStateException("Replay文件未打开");
        }
        
        try (ReplayInputStream in = replayFile.getPacketData(registry)) {
            int packetCount = 0;
            long lastTime = 0;
            
            while (in.available() > 0) {
                // 读取时间戳
                long time = in.readTimestamp();
                
                // 读取数据包
                Packet packet = in.readPacket();
                packetCount++;
                
                // 处理特定类型的数据包
                processPacket(packet, time);
                
                if (packetCount % 1000 == 0) {
                    logger.info("已处理 {} 个数据包", packetCount);
                }
                
                lastTime = time;
            }
            
            logger.info("数据包分析完成，共处理 {} 个数据包", packetCount);
        }
    }
    
    /**
     * 处理单个数据包
     */
    private void processPacket(Packet packet, long time) {
        try {
            String packetType = packet.getClass().getSimpleName();
            
            // 处理窗口物品数据包（包含背包信息）
            if (packetType.contains("WindowItems") || packetType.contains("WindowItemsS2CPacket")) {
                processWindowItemsPacket(packet, time);
            }
            
            // 处理设置槽位数据包（单个物品更新）
            if (packetType.contains("SetSlot") || packetType.contains("SetSlotS2CPacket")) {
                processSetSlotPacket(packet, time);
            }
            
            // 处理玩家生成数据包
            if (packetType.contains("PlayerSpawn") || packetType.contains("PlayerSpawnS2CPacket")) {
                processPlayerSpawnPacket(packet, time);
            }
            
        } catch (Exception e) {
            logger.warn("处理数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 处理窗口物品数据包
     */
    private void processWindowItemsPacket(Packet packet, long time) {
        try {
            // 这里需要解析具体的背包数据
            // 由于ReplayStudio的API限制，我们需要通过反射或其他方式提取数据
            logger.debug("发现窗口物品数据包，时间戳: {}", time);
            
            // 创建背包快照
            InventorySnapshot snapshot = new InventorySnapshot(time, "window_items");
            inventorySnapshots.add(snapshot);
            
        } catch (Exception e) {
            logger.warn("处理窗口物品数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 处理设置槽位数据包
     */
    private void processSetSlotPacket(Packet packet, long time) {
        try {
            logger.debug("发现设置槽位数据包，时间戳: {}", time);
            
            // 创建背包快照
            InventorySnapshot snapshot = new InventorySnapshot(time, "set_slot");
            inventorySnapshots.add(snapshot);
            
        } catch (Exception e) {
            logger.warn("处理设置槽位数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 处理玩家生成数据包
     */
    private void processPlayerSpawnPacket(Packet packet, long time) {
        try {
            logger.debug("发现玩家生成数据包，时间戳: {}", time);
            
            // 提取玩家信息
            // 这里需要根据具体的协议版本来解析数据包
            
        } catch (Exception e) {
            logger.warn("处理玩家生成数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 生成分析报告
     */
    private void generateReport() throws IOException {
        logger.info("正在生成分析报告...");
        
        Path reportPath = mcprFile.getParent().resolve("analysis_report.json");
        
        Map<String, Object> report = new HashMap<>();
        report.put("mcprFile", mcprFile.toString());
        report.put("analysisTime", System.currentTimeMillis());
        report.put("inventorySnapshots", inventorySnapshots.size());
        report.put("playerData", playerDataMap.size());
        
        // 详细的背包快照信息
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (InventorySnapshot snapshot : inventorySnapshots) {
            Map<String, Object> snap = new HashMap<>();
            snap.put("time", snapshot.time);
            snap.put("type", snapshot.type);
            snapshots.add(snap);
        }
        report.put("snapshots", snapshots);
        
        // 保存报告
        try (FileWriter writer = new FileWriter(reportPath.toFile())) {
            gson.toJson(report, writer);
        }
        
        logger.info("分析报告已保存到: {}", reportPath);
        
        // 打印摘要
        System.out.println("\n=== MCPR文件分析摘要 ===");
        System.out.println("文件: " + mcprFile.getFileName());
        System.out.println("背包快照数量: " + inventorySnapshots.size());
        System.out.println("玩家数据数量: " + playerDataMap.size());
        System.out.println("报告文件: " + reportPath.getFileName());
        System.out.println("========================\n");
    }
    
    /**
     * 主函数 - 命令行入口
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("用法: java -jar mcpr-analyzer.jar <mcpr文件路径>");
            System.exit(1);
        }
        
        Path mcprFile = Paths.get(args[0]);
        MCPRAnalyzer analyzer = new MCPRAnalyzer(mcprFile);
        
        try {
            analyzer.analyze();
        } catch (Exception e) {
            logger.error("分析失败: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    /**
     * 玩家数据类
     */
    private static class PlayerData {
        String name;
        UUID uuid;
        Map<Integer, String> inventory = new HashMap<>(); // 槽位 -> 物品
        
        public PlayerData(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }
    
    /**
     * 背包快照类
     */
    private static class InventorySnapshot {
        long time;
        String type;
        Map<String, Object> data = new HashMap<>();
        
        public InventorySnapshot(long time, String type) {
            this.time = time;
            this.type = type;
        }
    }
}