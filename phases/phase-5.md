# Phase 5 지시서: 자가 치유 지식 린터 (Self-Healing Daemon)
> **Status: [완료됨]**
> - **히스토리**: Neo4j 서브그래프 기반 고아 노드 및 충돌 데이터 검출 린터 데몬 구현. STALE_DATA 등 유지보수 빚 자동 탐지 로직 적용.

> **Status: [완료됨]**
> - **히스토리**: 초기 구현 시 단일 테넌트(Global Dir & Lock)로 작성되었으나, Phase 5.5 보수 작업을 통해 `workspaceId` 기반의 완벽한 논리/물리 파일 시스템 및 분산 락 격리(Multi-Tenancy) 처리가 완료됨.

## 1. 개요 및 목표
- AIMS-Graph의 핵심 유지보수 로직인 자가 치유 린터 데몬을 개발합니다.
- 변경된 노드의 서브그래프를 스캔하여 고아 문서(Orphan Page), 깨진 링크, 구식 데이터 등을 감지하고, 가능한 부분은 자동 교정(Auto-fix)합니다.

## 2. 참조 문서
- `GEMINI.md` 및 `rules/common/project-rules.md` (TDD Guard 준수)
- `docs/ADR.md` (ADR-005: 비용 절감형 자가 치유 지식 린터 데몬)
- `docs/WIKI_SCHEMA.md` (7. Lint 데몬 판별 규칙)
- `docs/API_SPEC.md` (4. Lint API 스펙)

## 3. 구현 내용 목록
1. **Lint Core 로직 개발 (`com.aimsgraph.lint` 패키지)**
   - `WIKI_SCHEMA.md`의 판별 규칙(ORPHAN_PAGE, BROKEN_LINK, STALE_DATA, CONTRADICTION, MISSING_FRONTMATTER)을 확인하는 검사 로직 구현.
   - 자동 교정 가능 항목(ORPHAN_PAGE 링크 복구, MISSING_FRONTMATTER 채우기)에 대한 Auto-fix 기능 구현.
2. **Neo4j Subgraph 추출 및 린트 스케줄러**
   - 최근 변경된 노드의 의존성 하위 트리를 Neo4j 쿼리로 추출.
   - Spring `@Scheduled`를 활용해 데몬 스레드(가상 스레드 권장)로 백그라운드 주기적 실행.
3. **내부 통제용 Lint API 제공**
   - `POST /api/internal/lint` 엔드포인트 구현 (요청 시 즉시 린트 수행 및 결과 JSON 반환).

## 4. 제약 사항
- 반드시 테스트 코드(`src/test/...`) 작성 후 기능 구현 (TDD 방식).
- 블로킹 I/O 대기시간이 발생하므로 가상 스레드(Virtual Threads) 위에서 동작하는지 고려하세요.
- 작업 완료 후 `c:\second brain\tasks.md` 파일에서 Phase 5를 `[x]`로 갱신하고 최종 완료 보고를 작성하세요.
