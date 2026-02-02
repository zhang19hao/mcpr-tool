package com.replaymod.analyzer;

import com.replaymod.replaystudio.protocol.Packet;
import com.replaymod.replaystudio.protocol.packets.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 高级背包数据解析器
 * 专门用于从Minecraft网络数据包中提取玩家背包信息
 */
public class AdvancedInventoryParser {
    private static final Logger logger = LogManager.getLogger(AdvancedInventoryParser.class);
    
    // 存储所有玩家的背包数据
    private Map<UUID, PlayerInventory> playerInventories = new HashMap<>();
    private Map<UUID, String> playerNames = new HashMap<>();
    
    // 窗口ID映射
    private Map<Integer, String> windowTypes = new HashMap<>();
    
    public AdvancedInventoryParser() {
        initializeWindowTypes();
    }
    
    /**
     * 初始化窗口类型映射
     */
    private void initializeWindowTypes() {
        windowTypes.put(0, "player_inventory");
        windowTypes.put(1, "chest");
        windowTypes.put(2, "crafting_table");
        windowTypes.put(3, "furnace");
        windowTypes.put(4, "dispenser");
        windowTypes.put(5, "enchantment_table");
        windowTypes.put(6, "brewing_stand");
        windowTypes.put(7, "villager");
        windowTypes.put(8, "beacon");
        windowTypes.put(9, "anvil");
        windowTypes.put(10, "hopper");
        windowTypes.put(11, "dropper");
        windowTypes.put(12, "shulker_box");
    }
    
    /**
     * 解析数据包中的背包信息
     */
    public void parsePacket(Packet packet, long timestamp) {
        try {
            String packetType = packet.getClass().getSimpleName();
            
            switch (packetType) {
                case "WindowItemsS2CPacket":
                case "PacketWindowItems":
                    parseWindowItemsPacket(packet, timestamp);
                    break;
                    
                case "SetSlotS2CPacket":
                case "PacketSetSlot":
                    parseSetSlotPacket(packet, timestamp);
                    break;
                    
                case "PlayerSpawnS2CPacket":
                case "PacketSpawnPlayer":
                    parsePlayerSpawnPacket(packet, timestamp);
                    break;
                    
                case "WindowOpenS2CPacket":
                case "PacketOpenWindow":
                    parseWindowOpenPacket(packet, timestamp);
                    break;
                    
                case "WindowCloseS2CPacket":
                case "PacketCloseWindow":
                    parseWindowClosePacket(packet, timestamp);
                    break;
                    
                case "EntityEquipmentS2CPacket":
                case "PacketEntityEquipment":
                    parseEntityEquipmentPacket(packet, timestamp);
                    break;
                    
                default:
                    // 其他数据包类型，忽略
                    break;
            }
            
        } catch (Exception e) {
            logger.warn("解析数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 解析窗口物品数据包
     */
    private void parseWindowItemsPacket(Packet packet, long timestamp) {
        try {
            logger.debug("解析窗口物品数据包，时间戳: {}", timestamp);
            
            // 使用反射提取数据包字段
            int windowId = getFieldValue(packet, "windowId", -1);
            List<?> items = getFieldValue(packet, "items", Collections.emptyList());
            
            if (windowId == 0) { // 玩家背包
                UUID playerId = getCurrentPlayerId();
                if (playerId != null) {
                    PlayerInventory inventory = getOrCreateInventory(playerId);
                    inventory.updateFromItemList(items, timestamp);
                    logger.info("更新玩家 {} 的背包，物品数量: {}", 
                              getPlayerName(playerId), items.size());
                }
            }
            
        } catch (Exception e) {
            logger.warn("解析窗口物品数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 解析设置槽位数据包
     */
    private void parseSetSlotPacket(Packet packet, long timestamp) {
        try {
            logger.debug("解析设置槽位数据包，时间戳: {}", timestamp);
            
            int windowId = getFieldValue(packet, "windowId", -1);
            int slot = getFieldValue(packet, "slot", -1);
            Object item = getFieldValue(packet, "item", null);
            
            if (windowId == 0 && slot >= 0) { // 玩家背包
                UUID playerId = getCurrentPlayerId();
                if (playerId != null) {
                    PlayerInventory inventory = getOrCreateInventory(playerId);
                    inventory.updateSlot(slot, item, timestamp);
                    logger.debug("更新玩家 {} 的槽位 {}，物品: {}", 
                               getPlayerName(playerId), slot, getItemName(item));
                }
            }
            
        } catch (Exception e) {
            logger.warn("解析设置槽位数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 解析玩家生成数据包
     */
    private void parsePlayerSpawnPacket(Packet packet, long timestamp) {
        try {
            logger.debug("解析玩家生成数据包，时间戳: {}", timestamp);
            
            UUID playerId = getFieldValue(packet, "playerId", null);
            String playerName = getFieldValue(packet, "name", "Unknown");
            
            if (playerId != null) {
                playerNames.put(playerId, playerName);
                getOrCreateInventory(playerId);
                logger.info("发现玩家: {} ({})", playerName, playerId);
            }
            
        } catch (Exception e) {
            logger.warn("解析玩家生成数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 解析窗口打开数据包
     */
    private void parseWindowOpenPacket(Packet packet, long timestamp) {
        try {
            int windowId = getFieldValue(packet, "windowId", -1);
            String windowType = getFieldValue(packet, "windowType", "unknown");
            String title = getFieldValue(packet, "title", "Unknown");
            
            windowTypes.put(windowId, windowType);
            logger.debug("打开窗口 {}: {} ({})", windowId, windowType, title);
            
        } catch (Exception e) {
            logger.warn("解析窗口打开数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 解析窗口关闭数据包
     */
    private void parseWindowClosePacket(Packet packet, long timestamp) {
        try {
            int windowId = getFieldValue(packet, "windowId", -1);
            logger.debug("关闭窗口 {}", windowId);
            
        } catch (Exception e) {
            logger.warn("解析窗口关闭数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 解析实体装备数据包（盔甲和手持物品）
     */
    private void parseEntityEquipmentPacket(Packet packet, long timestamp) {
        try {
            int entityId = getFieldValue(packet, "entityId", -1);
            int slot = getFieldValue(packet, "slot", -1);
            Object item = getFieldValue(packet, "item", null);
            
            // 这里需要根据entityId找到对应的玩家
            UUID playerId = getPlayerByEntityId(entityId);
            if (playerId != null) {
                PlayerInventory inventory = getOrCreateInventory(playerId);
                inventory.updateEquipment(slot, item, timestamp);
                logger.debug("更新玩家 {} 的装备槽位 {}，物品: {}", 
                           getPlayerName(playerId), slot, getItemName(item));
            }
            
        } catch (Exception e) {
            logger.warn("解析实体装备数据包时出错: {}", e.getMessage());
        }
    }
    
    /**
     * 获取或创建玩家背包
     */
    private PlayerInventory getOrCreateInventory(UUID playerId) {
        return playerInventories.computeIfAbsent(playerId, k -> new PlayerInventory(playerId));
    }
    
    /**
     * 获取玩家名称
     */
    private String getPlayerName(UUID playerId) {
        return playerNames.getOrDefault(playerId, "Unknown");
    }
    
    /**
     * 获取当前玩家ID（简化版本）
     */
    private UUID getCurrentPlayerId() {
        // 在实际实现中，这里需要跟踪当前视角的玩家
        // 简化版本返回第一个玩家
        return playerInventories.keySet().stream().findFirst().orElse(null);
    }
    
    /**
     * 根据实体ID获取玩家ID
     */
    private UUID getPlayerByEntityId(int entityId) {
        // 在实际实现中，需要维护实体ID到玩家UUID的映射
        // 简化版本返回第一个玩家
        return playerInventories.keySet().stream().findFirst().orElse(null);
    }
    
    /**
     * 获取物品名称
     */
    private String getItemName(Object item) {
        if (item == null) return "empty";
        try {
            // 尝试获取物品名称
            String name = getFieldValue(item, "name", "");
            if (!name.isEmpty()) return name;
            
            // 获取类名作为备选
            return item.getClass().getSimpleName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 使用反射获取字段值
     */
    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object obj, String fieldName, T defaultValue) {
        if (obj == null) return defaultValue;
        
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return (T) field.get(obj);
            }
        } catch (Exception e) {
            // 忽略错误，返回默认值
        }
        
        return defaultValue;
    }
    
    /**
     * 查找字段（包括父类）
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * 获取所有玩家的背包数据
     */
    public Map<UUID, PlayerInventory> getPlayerInventories() {
        return new HashMap<>(playerInventories);
    }
    
    /**
     * 生成背包报告
     */
    public Map<String, Object> generateReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("totalPlayers", playerInventories.size());
        
        List<Map<String, Object>> players = new ArrayList<>();
        for (Map.Entry<UUID, PlayerInventory> entry : playerInventories.entrySet()) {
            Map<String, Object> player = new HashMap<>();
            player.put("uuid", entry.getKey().toString());
            player.put("name", getPlayerName(entry.getKey()));
            player.put("inventory", entry.getValue().getInventorySummary());
            players.add(player);
        }
        report.put("players", players);
        
        return report;
    }
    
    /**
     * 玩家背包类
     */
    public static class PlayerInventory {
        private final UUID playerId;
        private Map<Integer, ItemStack> items = new HashMap<>();
        private Map<Integer, ItemStack> equipment = new HashMap<>(); // 盔甲槽
        private long lastUpdate;
        
        public PlayerInventory(UUID playerId) {
            this.playerId = playerId;
        }
        
        public void updateFromItemList(List<?> itemList, long timestamp) {
            items.clear();
            for (int i = 0; i < itemList.size(); i++) {
                Object item = itemList.get(i);
                if (item != null) {
                    items.put(i, new ItemStack(item));
                }
            }
            lastUpdate = timestamp;
        }
        
        public void updateSlot(int slot, Object item, long timestamp) {
            if (item != null) {
                items.put(slot, new ItemStack(item));
            } else {
                items.remove(slot);
            }
            lastUpdate = timestamp;
        }
        
        public void updateEquipment(int slot, Object item, long timestamp) {
            if (item != null) {
                equipment.put(slot, new ItemStack(item));
            } else {
                equipment.remove(slot);
            }
            lastUpdate = timestamp;
        }
        
        public Map<String, Object> getInventorySummary() {
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalItems", items.size());
            summary.put("lastUpdate", lastUpdate);
            
            // 统计物品类型
            Map<String, Integer> itemCounts = new HashMap<>();
            for (ItemStack item : items.values()) {
                String name = item.getName();
                itemCounts.put(name, itemCounts.getOrDefault(name, 0) + 1);
            }
            summary.put("itemCounts", itemCounts);
            
            return summary;
        }
        
        public UUID getPlayerId() {
            return playerId;
        }
    }
    
    /**
     * 物品堆叠类
     */
    public static class ItemStack {
        private final Object item;
        private String name;
        private int count;
        
        public ItemStack(Object item) {
            this.item = item;
            this.name = extractItemName(item);
            this.count = extractItemCount(item);
        }
        
        private String extractItemName(Object item) {
            if (item == null) return "empty";
            try {
                // 尝试各种方式获取物品名称
                String name = getFieldValue(item, "name", "");
                if (!name.isEmpty()) return name;
                
                name = getFieldValue(item, "displayName", "");
                if (!name.isEmpty()) return name;
                
                // 获取类名作为备选
                return item.getClass().getSimpleName().replace("Item", "");
            } catch (Exception e) {
                return "unknown";
            }
        }
        
        private int extractItemCount(Object item) {
            try {
                return getFieldValue(item, "count", 1);
            } catch (Exception e) {
                return 1;
            }
        }
        
        public String getName() {
            return name;
        }
        
        public int getCount() {
            return count;
        }
        
        @Override
        public String toString() {
            return name + " x" + count;
        }
    }
}