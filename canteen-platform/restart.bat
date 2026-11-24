@echo off
title Canteen Platform - Restart
echo ============================================
echo   Canteen Platform - Quick Restart
echo   (Use when code NOT changed / config only)
echo   After code changes use rebuild-restart.bat
echo ============================================

set BASE=D:\canteen-platform\canteen-platform
set JAVA=D:\canteen-platform\canteen-platform\tools\jdk17\jdk-17.0.20.1+1\bin\java.exe

echo [1/2] Stopping old backend (port 8080)...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do taskkill /f /pid %%p >nul 2>&1
timeout /t 3 /nobreak >nul

echo [2/2] Starting backend (port 8080)...
cd /d %BASE%\backend
start "canteen-backend" /min "%JAVA%" -jar target\canteen-platform.jar
timeout /t 10 /nobreak >nul

echo Done. Opening browser...
start http://localhost:8080
echo If not opened, visit http://localhost:8080
pause
