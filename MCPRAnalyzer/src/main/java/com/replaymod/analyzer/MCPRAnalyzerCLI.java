package com.replaymod.analyzer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * MCPR分析器的交互式命令行界面
 */
public class MCPRAnalyzerCLI {
    
    private static final String WELCOME_MESSAGE = """
        ╔══════════════════════════════════════════════════════════════╗
        ║                    MCPR Analyzer v1.0.0                     ║
        ║                                                              ║
        ║  ReplayMod录像文件分析工具 - 玩家背包数据提取器              ║
        ╚══════════════════════════════════════════════════════════════╝
        """;
    
    private static final String HELP_MESSAGE = """
        可用命令:
        
        analyze <文件路径>  - 分析指定的MCPR文件
        info <文件路径>     - 显示文件基本信息
        extract <文件路径>  - 提取所有玩家背包数据
        list <文件路径>     - 列出检测到的玩家
        help               - 显示此帮助信息
        exit               - 退出程序
        
        示例:
        analyze recording.mcpr
        extract C:\\replays\\my_recording.mcpr
        """;
    
    private Scanner scanner;
    private boolean running;
    
    public MCPRAnalyzerCLI() {
        this.scanner = new Scanner(System.in);
        this.running = true;
    }
    
    public void start() {
        System.out.println(WELCOME_MESSAGE);
        System.out.println("输入 'help' 查看可用命令，输入 'exit' 退出程序。\n");
        
        while (running) {
            System.out.print("mcpr-analyzer> ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            String[] parts = input.split("\\s+", 2);
            String command = parts[0].toLowerCase();
            String argument = parts.length > 1 ? parts[1] : "";
            
            try {
                handleCommand(command, argument);
            } catch (Exception e) {
                System.err.println("错误: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("感谢使用MCPR Analyzer！");
        scanner.close();
    }
    
    private void handleCommand(String command, String argument) {
        switch (command) {
            case "analyze":
                handleAnalyze(argument);
                break;
                
            case "info":
                handleInfo(argument);
                break;
                
            case "extract":
                handleExtract(argument);
                break;
                
            case "list":
                handleList(argument);
                break;
                
            case "help":
                System.out.println(HELP_MESSAGE);
                break;
                
            case "exit":
            case "quit":
                running = false;
                break;
                
            default:
                System.out.println("未知命令: " + command);
                System.out.println("输入 'help' 查看可用命令。");
        }
    }
    
    private void handleAnalyze(String filePath) {
        if (filePath.isEmpty()) {
            System.out.println("请提供MCPR文件路径");
            return;
        }
        
        Path path = Paths.get(filePath);
        if (!path.toFile().exists()) {
            System.err.println("文件不存在: " + filePath);
            return;
        }
        
        System.out.println("正在分析文件: " + filePath);
        
        try {
            MCPRAnalyzer analyzer = new MCPRAnalyzer(path);
            analyzer.analyze();
            
            System.out.println("✅ 分析完成！");
            System.out.println("报告文件已生成在相同目录下。");
            
        } catch (Exception e) {
            System.err.println("分析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleInfo(String filePath) {
        if (filePath.isEmpty()) {
            System.out.println("请提供MCPR文件路径");
            return;
        }
        
        Path path = Paths.get(filePath);
        if (!path.toFile().exists()) {
            System.err.println("文件不存在: " + filePath);
            return;
        }
        
        System.out.println("正在获取文件信息: " + filePath);
        
        try {
            // 创建简化的分析器只获取基本信息
            MCPRAnalyzer analyzer = new MCPRAnalyzer(path);
            
            // 这里可以添加只读取元数据的方法
            System.out.println("文件大小: " + formatFileSize(path.toFile().length()));
            System.out.println("最后修改: " + new java.util.Date(path.toFile().lastModified()));
            
        } catch (Exception e) {
            System.err.println("获取信息失败: " + e.getMessage());
        }
    }
    
    private void handleExtract(String filePath) {
        if (filePath.isEmpty()) {
            System.out.println("请提供MCPR文件路径");
            return;
        }
        
        Path path = Paths.get(filePath);
        if (!path.toFile().exists()) {
            System.err.println("文件不存在: " + filePath);
            return;
        }
        
        System.out.println("正在提取背包数据: " + filePath);
        
        try {
            // 创建高级分析器进行详细提取
            AdvancedInventoryParser parser = new AdvancedInventoryParser();
            
            // 这里需要集成完整的分析流程
            MCPRAnalyzer analyzer = new MCPRAnalyzer(path);
            analyzer.analyze();
            
            System.out.println("✅ 背包数据提取完成！");
            
        } catch (Exception e) {
            System.err.println("提取失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleList(String filePath) {
        if (filePath.isEmpty()) {
            System.out.println("请提供MCPR文件路径");
            return;
        }
        
        Path path = Paths.get(filePath);
        if (!path.toFile().exists()) {
            System.err.println("文件不存在: " + filePath);
            return;
        }
        
        System.out.println("正在列出检测到的玩家: " + filePath);
        
        try {
            // 这里可以添加快速扫描功能，只检测玩家而不进行完整分析
            System.out.println("检测到以下玩家:");
            // 实际实现中会显示真实的玩家列表
            
        } catch (Exception e) {
            System.err.println("列出玩家失败: " + e.getMessage());
        }
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
    
    public static void main(String[] args) {
        if (args.length > 0) {
            // 命令行模式
            handleCommandLine(args);
        } else {
            // 交互式模式
            new MCPRAnalyzerCLI().start();
        }
    }
    
    private static void handleCommandLine(String[] args) {
        if (args.length < 2) {
            System.err.println("用法: java -jar mcpr-analyzer.jar <命令> <文件路径>");
            System.err.println("可用命令: analyze, info, extract, list");
            System.exit(1);
        }
        
        String command = args[0].toLowerCase();
        String filePath = args[1];
        
        MCPRAnalyzerCLI cli = new MCPRAnalyzerCLI();
        cli.handleCommand(command, filePath);
    }
}