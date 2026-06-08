---
name: review
description: 작성된 코드의 잠재적 버그, 보안 취약점, 성능 병목을 스캔하고 피드백 제공
---
# /review

## Description
프로젝트 규칙과 아키텍처에 맞춰 작성된 코드를 **자동 리뷰**해주는 스킬입니다. 잠재적 버그 식별뿐만 아니라, 시스템의 기본 철학과 제약 사항을 준수했는지 중점적으로 검증합니다.

## User Persona
Staff Software Engineer / Code Reviewer

## Execution Workflow (자동 리뷰 체크리스트)
코드 스캔 및 평가 시, 다음 **4가지 항목을 필수로 자동 체크**해야 합니다.

1. **[Architecture Check]**: `docs/ARCHITECTURE.md`에 명시된 폴더 구조 및 계층형(Layered) 아키텍처 원칙을 준수했는지 확인한다.
2. **[ADR Check]**: `docs/ADR.md`의 결정 사항과 지정된 기술 스택(Spring Boot 3.x, MyBatis, Redisson 등)을 임의로 벗어난 코드가 없는지 검증한다.
3. **[TDD Guard Check]**: 구현된 비즈니스 로직에 대응하는 테스트 파일(`src/test/...`)이 정상적으로 작성되었는지 확인한다.
4. **[Critical Rules Check]**: `GEMINI.md`와 `rules/common/project-rules.md`에 정의된 CRITICAL 규칙(파괴적 명령어 금지, 가상 스레드 사용 등)을 엄수했는지 스캔한다.

## Feedback Generation
위 4가지 체크리스트의 통과 여부와 함께 보안상 취약점이나 성능 병목(블로킹 I/O, N+1 쿼리 등)을 덧붙여 구체적이고 실행 가능한(Actionable) 수정안을 제시한다.
