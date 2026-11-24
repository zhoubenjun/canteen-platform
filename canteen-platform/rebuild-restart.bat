@echo off
title Canteen Platform - Rebuild & Restart
echo ============================================
echo   Canteen Platform - Full Rebuild & Restart
echo   (Frontend + Backend + default images)
echo   Use this after you modify source code
echo ============================================

set BASE=D:\canteen-platform\canteen-platform
set JAVA=D:\canteen-platform\canteen-platform\tools\jdk17\jdk-17.0.20.1+1\bin\java.exe

echo [1/5] Stopping old backend (8080)...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do taskkill /f /pid %%p >nul 2>&1
timeout /t 3 /nobreak >nul

echo [2/5] Building frontend (npm run build)...
cd /d %BASE%\frontend
call npm run build
if errorlevel 1 ( echo FRONTEND BUILD FAILED & pause & exit /b 1 )

echo [3/5] Regenerating default category images...
cd /d %BASE%\tools
"%JAVA%" -cp . GenDefaultImages

echo [4/5] Packaging backend (mvn package)...
cd /d %BASE%\backend
call mvn -q -DskipTests package
if errorlevel 1 ( echo BACKEND BUILD FAILED & pause & exit /b 1 )

echo [5/5] Starting backend (8080)...
start "canteen-backend" /min "%JAVA%" -jar target\canteen-platform.jar
timeout /t 12 /nobreak >nul

echo Done. Opening browser...
start http://localhost:8080
echo If not opened, visit http://localhost:8080
pause
