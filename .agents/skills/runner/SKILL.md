---
name: runner
description: 프론트엔드 및 백엔드 개발 서버를 기동하고 포트 충돌을 방어하는 실행 스킬
---

# Runner Skill

이 스킬은 프로젝트 내의 프론트엔드(Vite + React)와 백엔드(Spring Boot + Java 21) 개발 서버를 안전하게 기동하고 포트 충돌을 방지하며, 프로세스를 관리하기 위한 스킬입니다.

## 스킬 스크립트 위치
- 백엔드 실행 스크립트: [run-backend.ps1](file:///c:/.agents/skills/runner/scripts/run-backend.ps1)
- 프론트엔드 실행 스크립트: [run-frontend.ps1](file:///c:/.agents/skills/runner/scripts/run-frontend.ps1)
- 통합 실행 스크립트: [run-all.ps1](file:///c:/.agents/skills/runner/scripts/run-all.ps1)

## 사용 방법

에이전트는 터미널 명령어를 실행할 때 포트 충돌이 나지 않도록 항상 이 스킬 아래에 있는 `scripts`의 파일들을 활용해야 합니다.

### 1. 백엔드 서버 기동
백엔드 포트(8080)를 사용하는 프로세스를 찾아 안전하게 종료한 후, 필요한 환경변수를 주입하고 Spring Boot 애플리케이션을 실행합니다.
```powershell
powershell -File "c:\second brain\.agents\skills\runner\scripts\run-backend.ps1"
```

### 2. 프론트엔드 서버 기동
프론트엔드 포트(3000)를 점유하고 있는 프로세스를 해제한 뒤, `npm run dev`를 호출합니다.
```powershell
powershell -File "c:\second brain\.agents\skills\runner\scripts\run-frontend.ps1"
```

### 3. 전체 서버 기동 (동시 기동)
프론트엔드와 백엔드를 모두 각각 새로운 백그라운드 작업으로 기동합니다.
```powershell
powershell -File "c:\second brain\.agents\skills\runner\scripts\run-all.ps1"
```
