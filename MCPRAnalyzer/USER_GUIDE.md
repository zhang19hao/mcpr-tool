# MCPR Analyzer 使用指南

## 项目概述

MCPR Analyzer 是一个专门用于分析 ReplayMod 生成的 MCPR 录像文件的工具，能够提取玩家背包数据、生成详细的分析报告，并提供多种分析模式。

## 快速开始

### 1. 环境准备

确保已安装以下软件：
- Java 8 或更高版本
- Maven 3.6+
- Git (可选)

### 2. 构建项目

```bash
# 克隆项目 (如果还没有)
# git clone <项目地址>

# 进入项目目录
cd MCPRAnalyzer

# 构建项目
mvn clean package

# 或者使用构建脚本
./build.sh      # Linux/Mac
build.bat       # Windows
```

### 3. 基本使用

#### 命令行模式

```bash
# 分析MCPR文件
java -jar target/mcpr-analyzer-1.0.0.jar analyze recording.mcpr

# 提取背包数据
java -jar target/mcpr-analyzer-1.0.0.jar extract recording.mcpr

# 显示文件信息
java -jar target/mcpr-analyzer-1.0.0.jar info recording.mcpr

# 列出检测到的玩家
java -jar target/mcpr-analyzer-1.0.0.jar list recording.mcpr
```

#### 交互式模式

```bash
# 启动交互式界面
java -jar target/mcpr-analyzer-1.0.0.jar

# 在交互式界面中
mcpr-analyzer> analyze recording.mcpr
mcpr-analyzer> extract recording.mcpr
mcpr-analyzer> help
mcpr-analyzer> exit
```

## 高级功能

### 批量分析

```bash
# 批量分析目录中的所有MCPR文件
for file in *.mcpr; do
    echo "分析文件: $file"
    java -jar target/mcpr-analyzer-1.0.0.jar analyze "$file"
done
```

### 自定义输出

```java
// 编程方式使用分析器
MCPRAnalyzer analyzer = new MCPRAnalyzer(Paths.get("recording.mcpr"));
analyzer.setOutputFormat("json");  // 或 "csv", "xml"
analyzer.setVerbose(true);
analyzer.analyze();
```

## 输出文件说明

### 1. metadata.json
包含录像的基本元数据信息：
```json
{
  "date": 1640995200000,
  "mcVersion": "1.20.1",
  "protocolVersion": 763,
  "duration": 300000,
  "fileFormat": "MCPR",
  "generator": "ReplayMod"
}
```

### 2. analysis_report.json
详细的分析报告：
```json
{
  "mcprFile": "recording.mcpr",
  "analysisTime": 1640995200000,
  "inventorySnapshots": 42,
  "playerData": 3,
  "snapshots": [
    {
      "time": 1000,
      "type": "window_items"
    }
  ],
  "players": [
    {
      "uuid": "player-uuid",
      "name": "Steve",
      "inventory": {
        "totalItems": 36,
        "itemCounts": {
          "diamond_sword": 1,
          "cobblestone": 64
        }
      }
    }
  ]
}
```

### 3. inventory_timeline.json
背包变化时间线：
```json
{
  "timeline": [
    {
      "timestamp": 1000,
      "player": "Steve",
      "changes": [
        {
          "slot": 0,
          "from": "empty",
          "to": "diamond_sword"
        }
      ]
    }
  ]
}
```

## 故障排除

### 常见问题

**问题1: 构建失败**
```
[ERROR] Failed to execute goal on project mcpr-analyzer...
```
**解决**: 确保网络连接正常，Maven能够下载依赖。可以尝试清理本地仓库缓存：
```bash
rm -rf ~/.m2/repository/com/replaymod
mvn clean install
```

**问题2: 文件无法打开**
```
错误: 文件不存在: recording.mcpr
```
**解决**: 检查文件路径是否正确，文件是否存在且可读。

**问题3: 分析结果为空**
```
背包快照数量: 0
```
**解决**: 
- 确保录像是完整的游戏会话
- 检查录像是否包含玩家交互
- 某些服务器可能禁用了背包同步

**问题4: 内存不足**
```
Exception in thread "main" java.lang.OutOfMemoryError
```
**解决**: 增加JVM内存：
```bash
java -Xmx2g -jar target/mcpr-analyzer-1.0.0.jar analyze recording.mcpr
```

### 性能优化

对于大型MCPR文件：

1. **增加内存**: `-Xmx4g` 或更高
2. **并行处理**: 使用多线程分析多个文件
3. **过滤数据**: 只分析特定时间段或特定玩家
4. **增量分析**: 只分析新添加的数据包

## 扩展开发

### 添加新的数据包解析器

```java
public class CustomPacketParser {
    public void parseCustomPacket(Packet packet, long timestamp) {
        // 实现自定义解析逻辑
    }
}
```

### 自定义输出格式

```java
public interface OutputFormatter {
    void format(Map<String, Object> data, OutputStream output);
}
```

### 集成到其他工具

```java
// 作为库使用
MCPRAnalyzer analyzer = new MCPRAnalyzer(mcprFile);
analyzer.addListener(new AnalysisListener() {
    @Override
    public void onPacketProcessed(Packet packet) {
        // 自定义处理逻辑
    }
});
```

## 版本历史

- **v1.0.0**: 初始版本，支持基本分析和背包提取
- **v1.1.0**: 计划中的版本，将支持更多数据包类型

## 技术支持

如果遇到问题：

1. 查看日志文件中的详细错误信息
2. 确保使用兼容的Minecraft版本
3. 检查ReplayMod版本兼容性
4. 验证MCPR文件完整性

## 许可证

本项目基于ReplayMod的开源组件开发，遵循相应的开源许可证。