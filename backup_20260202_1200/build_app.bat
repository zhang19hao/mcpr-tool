@echo off
setlocal
set ROOT=%~dp0
cd /d %ROOT%
call ReplayMod\gradlew.bat -p ReplayMod\libs\ReplayStudio inventoryToolJar
if errorlevel 1 exit /b 1
if not exist ReplayMod\libs\ReplayStudio\build\libs\inventory-tool.jar (
  echo 未找到 inventory-tool.jar
  exit /b 1
)
copy /y ReplayMod\libs\ReplayStudio\build\libs\inventory-tool.jar .
python -m PyInstaller --noconsole --name MCPR背包工具 --add-data "inventory-tool.jar;." inventory_tool_gui.py
if errorlevel 1 exit /b 1
echo 打包完成，输出在 dist 目录
endlocal
