# Phase 4 지시서: API 서빙 및 MCP 통합
> **Status: [완료됨]**
> - **히스토리**: REST API 및 초기 버전의 MCP 엔드포인트 구축, File-back 로직을 통한 에이전트 탐색 결과의 위키 재기록 파이프라인 완성.


> **Status: [완료됨]**
> - **히스토리**: 초기 구현 시 하드코딩(Mock)으로만 작성되었으나, Phase 4.5 보수 작업을 통해 `FileBackService` 파일 I/O 및 Neo4j 실제 쿼리로 완벽 대체됨.
> - **비고**: 현재 `McpController`는 REST API(Pseudo-MCP) 규격이므로, Phase 6에서 공식 SSE(Server-Sent Events) MCP로 마이그레이션 필요.
## 1. 개요 및 목표
- Ingest Webhook, Workspace BYOK 연동, Query 처리를 위한 REST API를 개발합니다.
- Anthropic 등 에이전트가 위키 및 그래프에 접근할 수 있도록 MCP(Model Context Protocol) 호환 엔드포인트(Tools, Resources)를 제공합니다.
- 탐색 중 도출된 통찰(Insight)을 다시 위키로 기록(File-back)하는 기능을 구현합니다.

## 2. 참조 문서
- `GEMINI.md` 및 `rules/common/project-rules.md` (TDD Guard 준수)
- `docs/API_SPEC.md` (REST API 및 MCP 엔드포인트 스펙 명세)
- `docs/ADR.md` (ADR-004: Query 분석 결과의 재귀적 영속화)

## 3. 구현 내용 목록
1. **REST API 컨트롤러 구현 (`com.aimsgraph.api` 패키지)**
   - `POST /api/v1/ingest`: Ingestion Webhook (Outbox Event 저장 후 Kafka 발송 연계) - [완료됨]
   - `POST /api/v1/workspaces/{workspace_id}/api-key`: BYOK 설정 - [완료됨]
   - **`POST /api/v1/query` (CRITICAL)**: Mock 코드를 제거하고, LangChain4j의 `ChatLanguageModel`을 주입받아 실제 LLM에 쿼리하여 응답을 반환하도록 재구현하세요.
2. **MCP 엔드포인트 구현 (`com.aimsgraph.mcp` 패키지)**
   - **`McpController` (CRITICAL)**: `executeTool`의 switch 문에 있는 하드코딩된 `result = "Mocked..."` 코드를 전부 걷어내십시오.
     - `search_graph`: `Neo4jClient`를 주입받아 유저의 질문에 맞는 실제 Cypher 쿼리를 날리고 그래프 결과를 반환해야 합니다 (반드시 `workspaceId`로 격리 쿼리).
     - `read_wiki_page`: `workspaces/{workspaceId}/wiki/...` 경로의 실제 파일을 읽어오도록 수정하세요.
3. **File-back 로직 (재귀적 위키 저장)**
   - **`FileBackService.java` (CRITICAL)**: 기본 저장 경로가 `data/wiki`로 되어있습니다. 이를 멀티테넌시 규칙에 맞게 `workspaces/{workspaceId}/wiki/insights` 하위로 저장하도록 수정하고, index.md 및 log.md의 락(Lock) 키에도 `{workspaceId}`가 포함되도록 갱신하십시오.

## 4. 제약 사항
- 반드시 테스트 코드(`src/test/...`) 작성 후 기능 구현 (TDD 방식). MockMvc 등을 적극 활용하세요.
- JWT 인증 인터셉터를 거쳐 컨트롤러에서 `workspace_id`가 올바르게 주입되고, 접근이 격리(RBAC)되는지 테스트로 입증하세요.
- 작업 완료 후 `c:\second brain\tasks.md` 파일에서 Phase 4를 `[x]`로 갱신하고 보고하세요.
