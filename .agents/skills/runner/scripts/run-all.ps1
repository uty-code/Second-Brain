# run-all.ps1
# 프론트엔드와 백엔드 개발 서버를 각각 새 창으로 기동합니다.

Write-Host "Launching Backend Server in a new window..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-File", "c:\second brain\.agents\skills\runner\scripts\run-backend.ps1"

Write-Host "Launching Frontend Server in a new window..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-File", "c:\second brain\.agents\skills\runner\scripts\run-frontend.ps1"

Write-Host "Both servers are being launched." -ForegroundColor Green
