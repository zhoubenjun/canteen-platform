@echo off
title Canteen Platform - Start All
echo ============================================
echo   Canteen Platform - One-click Start
echo   (MySQL + Database init + Backend)
echo ============================================

set BASE=D:\canteen-platform\canteen-platform
set MYSQL=%BASE%\tools\mysql-8.0.37-winx64
set DATA=%BASE%\tools\mysql-data

echo [1/3] Starting MySQL...
if not exist "%DATA%\mysql" (
  echo   First-time init of MySQL data dir...
  "%MYSQL%\bin\mysqld.exe" --no-defaults --initialize-insecure --basedir=%MYSQL% --datadir=%DATA%
)
start "" /min "%MYSQL%\bin\mysqld.exe" --no-defaults --basedir=%MYSQL% --datadir=%DATA% --port=3306 --console
timeout /t 8 /nobreak >nul

echo [2/3] Checking database...
"%MYSQL%\bin\mysql.exe" -uroot -proot -e "USE canteen; SELECT 1;" >nul 2>&1
if errorlevel 1 (
  echo   Initializing database...
  "%MYSQL%\bin\mysql.exe" -uroot -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';" >nul 2>&1
  "%MYSQL%\bin\mysql.exe" -uroot -proot --default-character-set=utf8mb4 -e "source %BASE%/sql/init.sql" >nul 2>&1
)

echo [3/3] Starting backend (8080)...
set JAVA=D:\canteen-platform\canteen-platform\tools\jdk17\jdk-17.0.20.1+1\bin\java.exe
cd /d %BASE%\backend
rem Clean leftover backend instances on port 8080
for /f "tokens=5" %%p in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do taskkill /f /pid %%p >nul 2>&1
timeout /t 2 /nobreak >nul

if exist "target\canteen-platform.jar" (
  start "canteen-backend" /min "%JAVA%" -jar target\canteen-platform.jar
) else (
  echo   jar not found, packaging first...
  call mvn -q -DskipTests package
  start "canteen-backend" /min "%JAVA%" -jar target\canteen-platform.jar
)

timeout /t 10 /nobreak >nul
echo Done. Opening browser...
start http://localhost:8080
echo If not opened, visit http://localhost:8080
pause
