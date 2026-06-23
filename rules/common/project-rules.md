> GEMINI.md에서 CRITICAL 규칙의 요약만 선언하고, 이 파일에서는 상세한 실행 규칙과 Hook 가드레일을 정의합니다.

## CRITICAL: 안전 가드레일 (Hooks)

### LLM Wiki Guard
- **절대 원칙 (NO RAG)**: 이 프로젝트에서 RAG(문서 청킹, 벡터 임베딩, 코사인 유사도 검색, Vector DB)를 부활시키거나 다시 도입하려는 어떠한 시도도 즉각 중단하고 기각할 것.
- 우리는 수학적 유사도 기반의 파편화된 검색을 거부하고, 오직 원본을 마크다운 위키로 컴파일하며 온톨로지(Neo4j)의 명시적 엣지를 따라 '에이전틱 순회(Agentic Graph Traversal)'를 하는 **안드레이 카파시의 "LLM Wiki (Second Brain)" 패턴**만을 유일한 지식 탐색 아키텍처로 강제한다.

### TDD Guard
- 구현 파일(`src/main/`)을 수정하거나 새로 생성할 때, 해당 기능에 대응하는 테스트 파일(`src/test/`)이 반드시 함께 존재해야 한다.
- 테스트 없이 구현 코드만 작성하는 것은 절대 금지.
- 테스트가 먼저 실패(Red)하는 것을 확인한 후, 구현을 작성하여 통과(Green)시킬 것.

### Dangerous Command Guard
- 아래 파괴적 명령어는 어떤 상황에서도 실행 금지:
  - `rm -rf /`, `rm -rf *`, `rmdir /s /q`
  - `git reset --hard`
  - `git push --force`, `git push -f`
  - `DROP DATABASE`, `TRUNCATE TABLE` (프로덕션 환경)
- 만약 위 명령어가 반드시 필요한 상황이라면, 실행 전 사용자에게 명시적으로 확인을 받을 것.

### Circuit Breaker
- 동일한 테스트 또는 빌드에서 **3회 연속 실패** 시, 같은 접근 방식을 반복하지 말 것.
- 즉시 작업을 중단하고, 실패 원인을 분석한 뒤 다른 전략을 제시하거나 사용자에게 개입을 요청할 것.
- 절대로 동일한 코드를 미세하게만 바꿔가며 무한 루프를 돌지 말 것.

### Phase Completion Guard
- Phase 완료 시 `tasks.md` 갱신과 더불어, 반드시 작업한 `phases/phase-*.md` 최상단에 `> **Status: [완료됨]**` 및 핵심 결정사항을 `- **히스토리**:`로 간결히 기록할 것.

### Auto-Documentation Guard (자동 문서화 규칙)
- 아키텍처, 데이터베이스 스키마, API 엔드포인트 등 시스템의 주요 변경사항이 발생한 경우, **사용자가 별도로 지시하지 않더라도 에이전트가 스스로 판단하여 관련된 `docs/` 폴더 내의 문서(`ARCHITECTURE.md`, `API_SPEC.md`, `ADR.md` 등)를 즉각 업데이트할 것.**
- 새 세션이 열렸을 때 문서만 읽고도 현재 시스템 상태 to 100% 파악할 수 있도록 최신 상태 유지를 최우선 과제로 삼는다.

### Harness Engine Guard
- **하네스 용어 혼동 방지**: '하네스(Harness)' 혹은 '하네스 프레임워크' 관련 질문이나 태스크를 수행할 때, 절대 일반적인 QA 테스트 자동화/테스트 케이스 작성용 테스트 하네스로 혼동하지 말 것.
- 본 워크스페이스에서 하네스는 **'기획 분석 후 자율 코딩을 지시하는 Headless 서브 에이전트 오케스트레이션 엔진'**을 의미한다.
- 하네스 관련 문서화 또는 워크플로우 지시서는 반드시 `GEMINI.md` 및 `.agents/skills/harness/SKILL.md`에 명시된 규칙(Context Ingestion -> Goal Alignment -> Phase Splitting/updates.md Append -> Subagent Invoke)을 준수하여 작성되어야 한다.
