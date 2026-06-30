---
name: runner
description: 프론트엔드 및 백엔드 개발 서버를 기동하고 포트 충돌을 방어하는 실행 스킬
---

# Runner Skill

이 스킬은 프로젝트 내의 프론트엔드와 백엔드 개발 서버를 안전하게 기동하고 프로세스를 관리하기 위한 스킬입니다.

## 실행 스크립트 위치
- 통합 기동 스크립트: [start-all.bat](file:///c:/second%20brain/start-all.bat)
- 통합 정지 스크립트: [stop-all.bat](file:///c:/second%20brain/stop-all.bat)

## 사용 방법

### 1. 전체 서버 기동 (도커 + 백엔드 + 프론트엔드)
도커 컨테이너들의 포트가 활성화될 때까지 지능적으로 실시간 대기한 뒤, 백엔드와 프론트엔드 서버를 켭니다.
```cmd
"c:\second brain\start-all.bat"
```

### 2. 전체 서버 정지 (프로세스 킬 + 도커 다운)
실행 중인 자바 백엔드 및 노드 프론트엔드 프로세스를 강제 종료하고, 도커 컨테이너를 내립니다.
```cmd
"c:\second brain\stop-all.bat"
```
