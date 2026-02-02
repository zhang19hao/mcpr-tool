# MCPR Analyzer

一个用于分析ReplayMod生成的MCPR录像文件的工具，专门提取玩家背包数据。

## 功能特性

- 📹 解析MCPR录像文件格式
- 🎒 提取玩家背包数据
- 📊 分析物品变化历史
- 📈 生成详细的分析报告
- 🔍 支持多种数据包类型解析

## 使用方法

### 命令行使用

```bash
# 编译项目
mvn clean package

# 运行分析器
java -jar target/mcpr-analyzer-1.0.0.jar <mcpr文件路径>

# 示例
java -jar target/mcpr-analyzer-1.0.0.jar recording.mcpr
```

### 编程使用

```java
import com.replaymod.analyzer.MCPRAnalyzer;
import java.nio.file.Paths;

public class Example {
    public static void main(String[] args) {
        MCPRAnalyzer analyzer = new MCPRAnalyzer(Paths.get("recording.mcpr"));
        try {
            analyzer.analyze();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 输出文件

分析器会生成以下文件：

- `metadata.json` - 录像元数据信息
- `analysis_report.json` - 详细的分析报告
- `inventory_timeline.json` - 背包变化时间线

## 支持的Minecraft版本

- 1.7.10 - 1.21.x
- 支持Forge和Fabric版本

## 技术细节

### 数据包解析

分析器会解析以下类型的数据包：

- **WindowItemsS2CPacket** - 窗口物品数据
- **SetSlotS2CPacket** - 单个槽位更新
- **PlayerSpawnS2CPacket** - 玩家生成信息
- **EntityEquipmentS2CPacket** - 实体装备数据
- **WindowOpenS2CPacket** - 窗口打开事件

### 背包数据结构

```json
{
  "playerId": "uuid-string",
  "playerName": "PlayerName",
  "inventory": {
    "totalItems": 36,
    "itemCounts": {
      "diamond_sword": 1,
      "cobblestone": 64,
      "iron_pickaxe": 1
    }
  },
  "equipment": {
    "helmet": "diamond_helmet",
    "chestplate": "diamond_chestplate",
    "leggings": "diamond_leggings",
    "boots": "diamond_boots"
  }
}
```

## 开发说明

### 依赖项

- ReplayMod Core
- ReplayStudio
- PacketLib
- Gson (JSON处理)
- Log4j (日志)

### 构建要求

- Java 8 或更高版本
- Maven 3.6+

## 注意事项

1. **性能考虑**：大型录像文件可能需要较长时间分析
2. **内存使用**：建议在64位JVM上运行，分配足够内存
3. **兼容性**：不同Minecraft版本的协议可能有所不同

## 故障排除

### 常见问题

**Q: 分析器无法打开MCPR文件**
A: 确保文件路径正确，文件未损坏，且是有效的ReplayMod录像文件

**Q: 背包数据为空**
A: 检查录像是否包含完整的游戏会话，某些服务器可能禁用了背包同步

**Q: 分析速度很慢**
A: 大型录像文件包含大量数据包，这是正常现象。可以尝试增加JVM内存

### 日志级别

可以通过系统属性设置日志级别：

```bash
java -Dlog4j.level=DEBUG -jar mcpr-analyzer.jar recording.mcpr
```

## 许可证

本项目基于ReplayMod的开源组件开发，遵循相应的开源许可证。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。