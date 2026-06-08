---
name: harness
description: 프로젝트 기획서를 분석하고 서브 에이전트를 띄워 자동 코딩을 수행하는 하네스 엔진
---
# /harness

## Description
프로젝트의 헌법(`GEMINI.md`), 기획서(`docs/`), 규칙(`rules/`)을 분석하여 구현 계획을 구체화하고, 사용자와 논의를 거친 뒤, 각 단계마다 독립된 서브 에이전트(Subagent)를 백그라운드로 띄워 코딩을 자율 수행하는 원스톱 헤드리스(Headless) 멀티 에이전트 파이프라인 엔진입니다.

## User Persona
Staff Backend Engineer / DevOps Manager

## Execution Workflow
1. **[Context Ingestion]** 워크스페이스 루트의 `GEMINI.md`(프로젝트 헌법)를 최우선으로 읽고, 이어서 `rules/common/project-rules.md`와 `docs/` 폴더 내의 모든 기획 문서(PRD, ARCHITECTURE, ADR, SCHEMA, API_SPEC, WIKI_SCHEMA, SETUP)를 정독하여 가드레일을 완전히 파악한다.
2. **[Goal Alignment & Validation]** `tasks.md`를 읽고, 기획 문서와 Phase 간의 기술적 갭이나 모순점이 없는지 검증한다. 심각한 설계 누락이나 에지 케이스가 발견되면 즉시 사용자에게 질문한다.
3. **[User Discussion — 구체화]** 검증 결과와 구현 계획 초안을 사용자에게 제시하고 **반드시 사용자의 승인을 받는다.** 사용자가 수정을 요청하면 반영하여 재제시한다. 승인 없이 코딩을 시작하지 않는다.
4. **[Phase Splitting]** 사용자가 승인하면, 구현 계획을 명확한 작업 단위로 쪼개어 `phases/` 폴더 하위에 개별 마크다운 파일(Phase 지시서)들을 생성한다. 이때 파일명은 반드시 `phase-{번호}.md` 포맷(예: phase-1.md, phase-2.md)을 엄격히 준수한다. 각 Phase 지시서에는 작업 범위, 생성할 파일 목록, 성공 기준을 명시한다.
5. **[Headless Subagent Execution]** `invoke_subagent` 도구를 사용하여 첫 번째 대기 중인 Phase 지시서를 담당할 완전히 새로운 서브 에이전트(Subagent)를 백그라운드 호출한다. 서브 에이전트에게는 해당 Phase 지시서와 `GEMINI.md`, `rules/`를 함께 전달하여 가드레일을 준수하도록 한다. (이로써 페이즈 간 컨텍스트 오염을 완벽히 차단한다.)
6. **[State & Chain Management]** 서브 에이전트가 Phase를 마치면, 루트 `tasks.md` 상태를 `[x]`로 갱신하고, 해당 `phases/phase-*.md` 파일 최상단에 `> **Status: [완료됨]**` 및 간결한 작업 히스토리를 의무 기록하도록 지시/확인한다. 이후 다음 Phase로 넘어간다.
7. **[Error Handling]** 서브 에이전트가 에러를 보고하거나, Circuit Breaker 규칙(3회 연속 실패)에 해당하면 파이프라인을 즉시 멈추고 사용자에게 개입을 요청한다. 에러 내용과 시도한 접근 방식을 함께 보고한다.
