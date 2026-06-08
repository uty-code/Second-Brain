---
name: github
description: 프로젝트 변경 사항을 검토하고 한국어로 된 일관된 커밋 메시지와 함께 GitHub 원격 저장소에 업로드하는 스킬
---
# /github

## Description
프로젝트의 현재 상태 및 변경된 소스 코드를 면밀히 분석하고, 지정된 Git 규칙에 맞게 **한국어로 정돈된 일관성 있는 커밋 메시지**를 작성하여 GitHub 원격 저장소(`origin`)에 안전하게 푸시(Push)하는 깃 업로드 자동화 스킬입니다.

## User Persona
Staff Software Engineer / DevOps Engineer

## Execution Workflow

1. **[Status Check & Filtering]**
   - `git status` 명령어를 실행하여 변경되거나 추가된 파일 목록을 파악합니다.
   - 데이터베이스 데이터, 로컬 테스트 산출물(`workspaces/`, `target/`), zip 파일 등 소스 코드 외의 임시 파일들이 `.gitignore`에 등록되어 누락 설정이 잘 작동하는지 사전 확인합니다.

2. **[Secrets Guard]**
   - 변경된 파일 내용에 API Key, 패스워드 등 민감한 개인 정보나 비밀 키가 하드코딩되어 포함되어 있는지 철저히 스캔합니다.
   - 만약 노출된 키가 발견되면, 즉시 소스 코드에서 환경 변수(`.env` 등) 주입 방식 혹은 플레이스홀더로 교체하여 커밋 이력에 남지 않도록 원천 조치합니다.

3. **[Consistent Korean Commit Message]**
   - 커밋 메시지는 Conventional Commits 방식을 준수하되, 반드시 **한국어로 작성**합니다.
   - **형식:** `<type>: <한국어 커밋 요약>`
   - **Type 목록:**
     - `feat`: 새로운 기능 추가 (예: `feat: 질문 에이전트 위키 컨텍스트 주입 로직 구현`)
     - `fix`: 버그 수정 (예: `fix: 파일 업로드 시 Neo4j DB 동기화 누락 수정`)
     - `refactor`: 코드 리팩토링 (예: `refactor: 중복된 파일 경로 관리 변수 통일`)
     - `docs`: 문서 수정 (예: `docs: ADR-010 신규 설계 문서 작성`)
     - `chore`: 빌드 설정, 의존성 패키지 관리 등 (예: `chore: gitignore 무시 규칙 추가`)
     - `test`: 테스트 코드 추가 및 수정 (예: `test: LlmService 단위 테스트 추가`)

4. **[Commit & Push]**
   - 안전이 확인된 소스 코드를 인덱스에 추가합니다 (`git add .` 혹은 특정 파일).
   - 확정된 한국어 커밋 메시지로 커밋을 생성합니다 (`git commit -m "<메시지>"`).
   - 설정된 원격 저장소(`origin`)의 현재 브랜치(예: `master` 또는 `main`)로 변경 내용을 안전하게 푸시합니다 (`git push origin <branch_name>`).
   - 푸시 보호(Push Protection) 등으로 에러 발생 시, 즉시 히스토리 정화 또는 원인 조치를 취합니다.
