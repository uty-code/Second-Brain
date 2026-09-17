# Project Custom Rules

- 사용자의 로컬 개발 환경에는 `OPENAI_API_KEY` 환경변수가 영구 등록되어 있습니다.
- 백엔드 실행(`bootRun`) 및 테스트 실행 가이드를 제공할 때, 환경변수 임시 주입 구문(예: `$env:OPENAI_API_KEY=...` 또는 `OPENAI_API_KEY=...`)은 항상 생략하십시오.
- 항상 환경변수가 이미 시스템에 설정되어 있다고 가정하고, 순수 실행 명령어(예: `./gradlew bootRun`)만 심플하게 제공하십시오.
- 새로운 코드를 작성하거나 수정할 때, 또는 시스템 변경 사항을 자동으로 문서화할 때 항상 [project-rules.md](file:///c:/second%20brain/rules/common/project-rules.md)에 정의된 **AI-Native Markdown 규칙**을 철저히 준수하십시오.

