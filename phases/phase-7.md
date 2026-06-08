# Phase 7 지시서: 백엔드 기능 고도화 및 LLM Wiki 패턴 완벽 적용
> **Status: [완료됨]**
> - **히스토리**: RAG 전면 폐기 및 순수 에이전틱 그래프 탐색(`get_node_context`) 구현, Rate Limiting 방어벽 적용, 실시간 푸시 알림(SSE) 구축, ZIP Export API 개발, 다변화된 동적 온톨로지 엣지 주입 기능 완벽 적용.


## 1. 개요 및 목표
- 이전 단계까지 만들어진 백엔드 파이프라인을 바탕으로, 실제 상용 서비스에 준하는 부가 기능들을 추가합니다.
- 특히 수학적 유사도 기반의 RAG를 전면 폐기하고, 안드레이 카파시의 'LLM Wiki (Second Brain)' 패턴에 완벽하게 부합하도록 온톨로지(Ontology)와 에이전틱 탐색 기능을 고도화합니다.

## 2. 참조 문서
- `docs/PRD.md` (Agentic Graph Traversal 철학)
- `docs/WIKI_SCHEMA.md` (다변화된 엣지 타입)
- `docs/ADR.md` (ADR-001, ADR-007)

## 3. 구현 내용 목록

### 3.1. 에이전틱 그래프 탐색 (Agentic Graph Traversal) 도입
- RAG(VectorStore, 코사인 유사도)를 백엔드 코드와 빌드 환경에서 완전히 제거합니다.
- `McpController`에 `get_node_context` 도구를 구현하여, LLM이 현재 노드의 마크다운 내용과 연결된 입출력 엣지(표지판)들을 보고 스스로 다음 탐색 경로를 추론할 수 있게 만듭니다.

### 3.2. Rate Limiting (API 호출 제한) 적용
- 클라이언트(에이전트)의 무분별한 요청을 방지하기 위해 `Redisson`의 `RRateLimiter`를 도입합니다.
- `JwtInterceptor`에서 `workspaceId`별로 API 요청을 분당 50회 등으로 제한하여 서버 부하를 방지합니다.

### 3.3. 실시간 푸시 알림 (Notification SSE)
- Kafka Consumer(`IngestionWorker`)나 Linter가 무거운 백그라운드 작업을 완료했을 때, 프론트엔드로 작업 완료 상태를 즉시 알려주기 위해 `NotificationController` (SSE 기반)를 구현합니다.

### 3.4. 데이터 내보내기 (Export API)
- 사용자의 지식 데이터가 플랫폼에 종속되지 않도록 보장하기 위해, `workspaceId`에 해당하는 전체 마크다운 파일과 설정들을 ZIP 형태로 다운로드할 수 있는 API(`GET /api/v1/workspaces/{workspaceId}/export`)를 제공합니다.

### 3.5. 다변화된 온톨로지(Edge Types) 동적 주입
- 개념을 무조건 `[:RELATES_TO]` 엣지로 연결하던 기존 로직을 파기합니다.
- `LlmService` 프롬프트를 수정하여 LLM이 스스로 `EXTENDS`, `CONTRADICTS`, `DEPENDS_ON`, `EXPLAINS`, `RELATED_TO` 등의 명확한 논리적 관계를 지정하게 만들고, 이를 Neo4j 그래프에 동적으로 반영합니다.

## 4. 제약 사항
- 모든 기능 추가는 TDD를 기반으로 단위 테스트를 통과해야 합니다.
- 작업 완료 후 `c:\second brain\tasks.md` 파일에 Phase 7 항목을 추가하고 `[x]`로 완료 표시하세요.
