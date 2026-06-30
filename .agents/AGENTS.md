# Project Custom Rules

- 사용자의 로컬 개발 환경에는 `OPENAI_API_KEY` 환경변수가 영구 등록되어 있습니다.
- 백엔드 실행(`bootRun`) 및 테스트 실행 가이드를 제공할 때, 환경변수 임시 주입 구문(예: `$env:OPENAI_API_KEY=...` 또는 `OPENAI_API_KEY=...`)은 항상 생략하십시오.
- 항상 환경변수가 이미 시스템에 설정되어 있다고 가정하고, 순수 실행 명령어(예: `./gradlew bootRun`)만 심플하게 제공하십시오.
