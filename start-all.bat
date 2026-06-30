@echo off
title AIMS-Graph Development Starter
cd /d "%~dp0"

echo ===================================================
echo [1/3] Starting Docker Containers...
echo ===================================================
cd aims-backend
docker-compose up -d
if %errorlevel% neq 0 (
    echo [ERROR] Failed to start Docker containers. Please ensure Docker Desktop is running.
    pause
    exit /b %errorlevel%
)

echo.
echo Waiting for infrastructure ports (6300, 9092, 7687, 1433) to open...
set /a retry_count=0

:wait_loop
set /a retry_count+=1
if %retry_count% gtr 30 (
    echo [WARNING] Port check timed out. Proceeding to start backend anyway...
    goto start_backend
)

:: Check Redis (6300)
netstat -ano | findstr LISTENING | findstr :6300 > nul
if %errorlevel% neq 0 (timeout /t 1 /nobreak > nul & goto wait_loop)

:: Check Kafka (9092)
netstat -ano | findstr LISTENING | findstr :9092 > nul
if %errorlevel% neq 0 (timeout /t 1 /nobreak > nul & goto wait_loop)

:: Check Neo4j (7687)
netstat -ano | findstr LISTENING | findstr :7687 > nul
if %errorlevel% neq 0 (timeout /t 1 /nobreak > nul & goto wait_loop)

:: Check SQL Server (1433)
netstat -ano | findstr LISTENING | findstr :1433 > nul
if %errorlevel% neq 0 (timeout /t 1 /nobreak > nul & goto wait_loop)

:start_backend
echo Infrastructure is ready!

echo.
echo ===================================================
echo [2/3] Launching AIMS-Graph Backend Server...
echo ===================================================
start "AIMS Backend" /D "." cmd /k "gradlew bootRun"

echo.
echo ===================================================
echo [3/3] Launching Frontend Development Server...
echo ===================================================
start "AIMS Frontend" /D "..\frontend" cmd /k "npm run dev"

echo.
echo ===================================================
echo Development environments are starting up!
echo Backend API : http://localhost:8080
echo Frontend UI : http://localhost:3000
echo ===================================================
pause
