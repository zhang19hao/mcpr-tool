#!/bin/bash

# MCPR Analyzer 构建脚本

echo "正在构建MCPR Analyzer..."

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误: Maven未安装，请先安装Maven"
    exit 1
fi

# 清理并构建
echo "执行Maven构建..."
mvn clean package

# 检查构建结果
if [ $? -eq 0 ]; then
    echo "✅ 构建成功！"
    echo "可执行JAR文件位置: target/mcpr-analyzer-1.0.0.jar"
    echo ""
    echo "使用方法:"
    echo "  java -jar target/mcpr-analyzer-1.0.0.jar <命令> <mcpr文件>"
    echo ""
    echo "示例:"
    echo "  java -jar target/mcpr-analyzer-1.0.0.jar analyze recording.mcpr"
    echo "  java -jar target/mcpr-analyzer-1.0.0.jar extract recording.mcpr"
else
    echo "❌ 构建失败，请检查错误信息"
    exit 1
fi