# run-backend.ps1
# 8080 포트를 찾아 점유 중인 프로세스 종료 후 백엔드 서버 기동

$port8080 = (netstat -ano | findstr :8080 | findstr LISTENING)
if ($port8080) {
    $pid8080 = ($port8080 -split '\s+')[-1]
    Write-Host "Killing process $pid8080 occupying port 8080..." -ForegroundColor Yellow
    Stop-Process -Id $pid8080 -Force
    Start-Sleep -Seconds 1
} else {
    Write-Host "Port 8080 is clear." -ForegroundColor Green
}

# 백엔드 디렉터리로 이동하여 실행
Set-Location "c:\second brain\aims-backend"
# Ensure DEEPSEEK_API_KEY is set in your actual environment, do not hardcode it here.
Write-Host "Starting Backend via ./gradlew bootRun..." -ForegroundColor Cyan
./gradlew bootRun
