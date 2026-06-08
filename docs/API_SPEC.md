# API 및 MCP 인터페이스 명세 (API_SPEC.md)

## 1. Ingestion Webhook (REST API)
외부 시스템에서 새로운 문서를 백엔드로 밀어 넣을 때 사용하는 엔드포인트입니다.

**`POST /api/v1/ingest`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}` (토큰에 매핑된 접근 허용 workspace_id 목록 추출)
- **Content-Type**: `application/json`
- **Request Body**:
  ```json
  {
    "source_id": "ext-12345",
    "title": "Andrej Karpathy's LLM Wiki Concept",
    "content_markdown": "Most people's experience with LLMs...",
    "source_type": "MARKDOWN",
    "tags": ["AI", "Architecture"]
  }
  ```
- **Response `202 Accepted`**:
  ```json
  {
    "event_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PENDING",
    "message": "Ingestion event queued successfully."
  }
  ```

**`GET /api/v1/ingest/{event_id}/status`**
- 아웃박스 이벤트의 현재 처리 상태를 조회합니다.
- **Response `200 OK`**:
  ```json
  {
    "event_id": "550e8400-...",
    "status": "PROCESSED",
    "processed_at": "2026-06-02T06:00:00Z"
  }
  ```

### 1.2 에러 응답 규약
모든 API 에러는 다음 통일된 JSON 포맷을 따릅니다.
```json
{
  "error": "DUPLICATE_SOURCE",
  "message": "Source with the same content_hash already exists.",
  "timestamp": "2026-06-02T06:00:00Z"
}
```
| HTTP Status | error code | 설명 |
|---|---|---|
| `400` | `INVALID_PAYLOAD` | 필수 필드 누락 또는 잘못된 형식 |
| `409` | `DUPLICATE_SOURCE` | 동일한 `content_hash`의 소스가 이미 존재 |
| `500` | `INTERNAL_ERROR` | 서버 내부 오류 |

## 2. Workspace (Tenant) Management API
사용자의 세컨드 브레인(금고) 설정을 조회하고 관리하기 위한 엔드포인트입니다. (BYOK 기능 및 데이터베이스 개별 API 키 등록 엔드포인트는 폐기되었습니다.)

**`GET /api/v1/workspaces/list`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **Response `200 OK`**:
  ```json
  [
    "default-workspace",
    "my-second-brain"
  ]
  ```

**`GET /api/v1/workspaces/{workspace_id}/graph`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **Response `200 OK`**:
  ```json
  {
    "nodes": [
      {
        "id": "harness-framework",
        "name": "하네스 프레임워크",
        "type": "concept",
        "val": 1
      }
    ],
    "links": [
      {
        "source": "harness-framework",
        "target": "harness-skill",
        "label": "RELATED_TO"
      }
    ]
  }
  ```

**`GET /api/v1/workspaces/{workspace_id}/export`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **Response `200 OK`**: `application/zip` 파일 스트림 (Neo4j 데이터 및 마크다운 위키 파일들을 포함하는 압축파일)

---

## 3. Wiki Page API
위키 페이지의 마크다운 상세 본문 콘텐츠를 조회합니다.

**`GET /api/v1/wiki/{conceptName}`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}` (또는 `X-Workspace-ID`)
- **Path Parameter**:
  - `conceptName`: 영문 파일 슬러그(예: `harness-framework`) 또는 한글/공백이 포함된 명칭(예: `하네스 프레임워크`).
  - **동적 식별자 해소 레이어(Dynamic Slug Resolution)**: `conceptName`이 한글/공백 형식이면, 백엔드가 실시간으로 Neo4j 그래프 DB에서 해당 이름을 `title`로 갖는 개념 노드를 찾아 매핑된 영문 슬러그를 찾아내어 파일을 조회합니다.
- **Response `200 OK`**:
  ```json
  {
    "content": "---\ntitle: \"하네스 프레임워크\"\n...\n# 하네스 프레임워크\n..."
  }
  ```
- **Response `404 Not Found`**:
  ```json
  {
    "error": "FILE_NOT_FOUND",
    "message": "The requested wiki page could not be found."
  }
  ```

---

## 4. Query API (REST)
사용자가 위키에 질문을 던지는 엔드포인트입니다.

**`POST /api/v1/query`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **Request Body**:
  ```json
  {
    "question": "LLM Wiki와 RAG의 근본적인 차이점은?",
    "file_back": true
  }
  ```
- `file_back`: `true`이면 응답 결과를 새로운 위키 페이지로 자동 저장(Filed back)합니다.
- **Response `200 OK`**:
  ```json
  {
    "answer": "...",
    "sources_cited": ["wiki/concepts/llm-wiki.md", "wiki/concepts/rag.md"],
    "filed_back_path": "wiki/insights/llm-wiki-vs-rag.md"
  }
  ```

## 4. Lint API (내부 / Daemon 전용)
자가 치유 데몬이 호출하는 내부 전용 API입니다.

**`POST /api/internal/lint`**
- **Request Body**:
  ```json
  {
    "scope": "CHANGED_SUBGRAPH",
    "since": "2026-06-01T00:00:00Z"
  }
  ```
- `scope`: `FULL` (전체 스캔) 또는 `CHANGED_SUBGRAPH` (변경된 노드의 의존성 트리만).
- **Response `200 OK`**:
  ```json
  {
    "issues_found": 3,
    "auto_fixed": 2,
    "requires_review": [{"page": "wiki/concepts/old-topic.md", "issue": "STALE_DATA"}]
  }
  ```

## 5. MCP (Model Context Protocol) 엔드포인트

> **✅ [상태: 공식 Spring AI MCP 서버 연동 완료]**
> Phase 6 및 6.5 마이그레이션을 통해 `org.springframework.ai:spring-ai-mcp-server-webmvc-spring-boot-starter`를 도입했습니다.
> 클라이언트는 `/mcp/sse` 엔드포인트를 통해 JSON-RPC 2.0 프로토콜로 직접 SSE 연결을 맺으며 통신합니다. (인증은 기존과 동일하게 JWT 헤더를 사용하며 `JwtInterceptor`가 가로채 처리합니다.)

AI 에이전트(Claude Desktop, Cursor 등)가 AIMS-Graph 시스템과 다이렉트로 통신하기 위해 노출되는 도구들입니다. Spring AI의 `@Tool` 어노테이션으로 구현되어 있습니다.

### 5.1 Endpoints
- **GET `/mcp/sse`**: 클라이언트가 SSE 스트림을 열기 위해 접속하는 진입점. Next.js 프론트엔드는 일반적인 REST 대신 `@modelcontextprotocol/sdk`를 사용하여 브라우저에서 직접 이 엔드포인트에 접속, 브라우저 기반 MCP 클라이언트로 동작하여 그래프 데이터를 가져옵니다.
- **POST `/mcp/message`**: 클라이언트가 툴 실행 요청 등을 JSON-RPC 형식으로 보낼 때 사용하는 엔드포인트. (SSE 연결 시 받은 sessionId 사용)

### 5.1 Tools
| Tool 이름 | 파라미터 | 반환 타입 | 설명 |
|---|---|---|---|
| `searchGraph` | `concept_name` (String) | JSON (노드 배열 + 관계) | Neo4j에서 개념과 인접 노드를 탐색 |
| `getNodeContext` | `concept_name` (String) | String (마크다운+엣지) | 그래프 순회를 위한 인접 컨텍스트 조회 |
| `readWikiPage` | `page_path` (String) | String (마크다운 원문) | 특정 위키 페이지 전체 텍스트 반환 |
| `fileBackInsight` | `title` (String), `content` (String) | JSON | 새로운 통찰을 위키 페이지로 기록 |
| `listRecentChanges` | 없음 | JSON (변경 로그 배열) | 최근 위키 변경 이력 조회 |

*(참고: `workspaceId` 파라미터는 더 이상 명시적으로 넘길 필요가 없습니다. HTTP Session의 JWT 토큰에서 자동 추출되어 주입됩니다.)*

### 5.2 Resources
| URI | 반환 타입 | 설명 |
|---|---|---|
| `wiki://index` | String (마크다운) | 전체 위키 목차 (`index.md`) |
| `wiki://log` | String (마크다운) | 연대기적 변경 기록 (`log.md`) |
| `wiki://stats` | JSON | 노드/엣지 수, 최근 Lint 결과 등 시스템 통계 |
