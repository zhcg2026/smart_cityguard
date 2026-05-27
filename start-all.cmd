@echo off
chcp 65001 >nul
title Smart Cityguard - All Services

echo ========================================
echo   Smart Cityguard 一键启动
echo ========================================
echo.

:: 检查并关闭已有进程，避免端口冲突
echo [0/4] 清理旧进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9000 ^| findstr LISTENING') do taskkill /PID %%a /F >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do taskkill /PID %%a /F >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000 ^| findstr LISTENING') do taskkill /PID %%a /F >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3003 ^| findstr LISTENING') do taskkill /PID %%a /F >nul 2>&1
timeout /t 1 /nobreak >nul
echo       旧进程已清理
echo.

echo [1/4] 启动 MinIO 文件服务 (port 9000)...
start "MinIO" powershell -ExecutionPolicy Bypass -File "%~dp0start-minio.ps1"
timeout /t 2 /nobreak >nul

echo [2/4] 启动后端 Spring Boot (port 8080)...
echo       首次启动会自动编译，请耐心等待...
cd /d d:\smart_cityguard\backend
start "Backend-8080" cmd /c "mvn spring-boot:run -pl smart-cityguard-server -DskipTests && pause"
echo       后端正在编译启动中（约30-60秒）...

echo [3/4] 启动管理端 (port 3000)...
cd /d d:\smart_cityguard\frontend\admin-web
start "AdminWeb-3000" cmd /c "npm run dev"

echo [4/4] 启动采集端 (port 3003)...
cd /d d:\smart_cityguard\frontend\collector-app
start "CollectorApp-3003" cmd /c "npm run dev"

echo.
echo ========================================
echo   四个服务正在启动中...
echo.
echo   MinIO API:     http://localhost:9000
echo   MinIO Console:  http://localhost:9001
echo   后端:           http://localhost:8080
echo   管理端:         http://localhost:3000
echo   采集端:         http://localhost:3003
echo   API文档:        http://localhost:8080/doc.html
echo.
echo   提示：
echo   - 前端修改：浏览器会自动刷新，无需重启
echo   - 后端修改：需要重启后端（重新双击此脚本即可）
echo   - 关闭服务：直接关闭对应窗口
echo ========================================
echo.
echo 按任意键退出此窗口（所有服务在独立窗口运行）
pause >nul