# MCPR 背包恢复工具

这是一个面向普通用户的可视化工具，用于从 ReplayMod 的 `.mcpr` 回放中提取指定时间的背包数据，并自动生成可直接导入游戏的 function 数据包。

## 适合谁用

- 只想恢复背包，不想折腾命令或脚本
- 希望用图形界面一步生成数据包

## 使用方法

1. 双击运行 `inventory-tool-gui.jar`
2. 选择回放文件（`.mcpr`）
3. 选择时间（可自行调整）
4. 点击“开始生成”
5. 输出目录里会生成一个数据包文件夹，可直接放到世界的 datapacks 里

## 运行前准备

- 电脑已安装 Java（建议 8 或以上）
- 回放文件来自 ReplayMod

## 版权说明

- ReplayMod 使用 GPL-3.0 许可

## 编译命令

在项目根目录执行：
```
.\ReplayMod\gradlew.bat -p ReplayMod\libs\ReplayStudio inventoryToolGuiJar
```

## 输出内容说明

- 数据包目录：`输出目录/InventoryRestore`
- 函数文件：`data/inventory_restore/functions/restore.mcfunction`
