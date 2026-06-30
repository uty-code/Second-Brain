@echo off
title AIMS-Graph Development Stopper
cd /d "%~dp0"

echo ===================================================
echo [1/3] Stopping Java Backend processes (Port 8080)...
echo ===================================================
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /f /pid %%a
)

echo.
echo ===================================================
echo [2/3] Stopping Node Frontend processes (Port 3000)...
echo ===================================================
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :3000 ^| findstr LISTENING') do (
    taskkill /f /pid %%a
)

echo.
echo ===================================================
echo [3/3] Shutting down Docker Containers...
echo ===================================================
cd aims-backend
docker-compose down

echo.
echo ===================================================
echo All environments have been successfully shut down.
echo ===================================================
pause
