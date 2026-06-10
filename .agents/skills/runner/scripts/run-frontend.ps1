# run-frontend.ps1
# 3000 포트를 찾아 점유 중인 프로세스 종료 후 프론트엔드 서버 기동

$port3000 = (netstat -ano | findstr :3000 | findstr LISTENING)
if ($port3000) {
    $pid3000 = ($port3000 -split '\s+')[-1]
    Write-Host "Killing process $pid3000 occupying port 3000..." -ForegroundColor Yellow
    Stop-Process -Id $pid3000 -Force
    Start-Sleep -Seconds 1
} else {
    Write-Host "Port 3000 is clear." -ForegroundColor Green
}

# 프론트엔드 디렉터리로 이동하여 실행
Set-Location "c:\second brain\frontend"
Write-Host "Starting Frontend via npm run dev..." -ForegroundColor Cyan
npm run dev
