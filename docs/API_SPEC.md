# API Specification: AIMS-Graph

## 1. Authentication & Account API (B2B 인증 및 계정 관리)
Spring Security 및 JWT 기반의 멀티유저 인증 API입니다.

### 1.1 사용자 등록 (Register)
- **`POST /api/v1/auth/register`**
- **Request Body**:
  ```json
  {
    "username": "user123",
    "password": "securepassword"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "message": "User registered successfully",
    "username": "user123"
  }
  ```

### 1.2 사용자 로그인 (Login)
- **`POST /api/v1/auth/login`**
- **Request Body**:
  ```json
  {
    "username": "user123",
    "password": "securepassword"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIiwi...",
    "username": "user123",
    "workspaceId": "ws-user123"
  }
  ```
  *(참고: 로그인 성공 시 해당 사용자에게 기본 할당되는 워크스페이스 ID `ws-[username]`도 함께 반환됩니다.)*

### 1.3 사용자 로그아웃 (Logout)
- **`POST /api/v1/auth/logout`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **동작**: 백엔드에서 전달된 JWT 토큰을 추출하고 유효성을 검증한 뒤, 남은 만료 시간(TTL) 동안 Redis 블랙리스트(`blacklist:{token}`)에 추가하여 즉각 토큰을 무효화합니다.
- **Response `200 OK`**:
  ```json
  {
    "message": "Logged out successfully"
  }
  ```

### 1.4 계정 영구 탈퇴 (Delete Account)
- **`DELETE /api/v1/auth/account`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **동작**: 사용자가 소유한 모든 데이터를 파괴적으로 삭제합니다.
  1. Neo4j에서 사용자의 워크스페이스 관련 노드 및 관계 전체 삭제 (`MATCH (n) WHERE n.workspaceId = 'ws-[username]' OR n.workspaceId STARTS WITH '[username]_' DETACH DELETE n`).
  2. 로컬 디렉토리에서 사용자 소유의 워크스페이스 폴더 전체 물리적 삭제.
  3. RDBMS `Users` 테이블에서 해당 유저 레코드 영구 삭제.
- **Response `200 OK`**:
  ```json
  {
    "message": "Account deleted successfully"
  }
  ```
- **Response `401 Unauthorized`**: 로그인 인증 실패 시 반환.

---

## 2. Ingest API (지식 수집 및 적재)
원본 데이터를 백엔드로 전달하여 분석 및 지식 컴파일 파이프라인을 구동시킵니다.

### 2.1 문서 수집 요청 (Ingest Source)
- **`POST /api/v1/ingest/source`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
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

### 2.2 수집 이벤트 상태 조회 (Get Ingest Status)
- **`GET /api/v1/ingest/{event_id}/status`**
- **Response `200 OK`**:
  ```json
  {
    "event_id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PROCESSED",
    "processed_at": "2026-06-02T06:00:00Z"
  }
  ```

---

## 3. Workspace API (워크스페이스 격리 및 자격증명 관리)
사용자의 격리된 지식창고(Vault) 목록 조회 및 생성을 처리합니다.

### 3.1 워크스페이스 목록 조회 (List Workspaces)
- **`GET /api/v1/workspaces/list`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **동작**: 현재 로그인된 사용자의 소유 영역에 해당하는 워크스페이스 폴더(`ws-[username]` 또는 `[username]_`로 시작)만 스캔하여 리스트로 반환합니다. 로그인되지 않은 사용자(또는 익명/더미 유저)는 빈 리스트를 반환받습니다.
- **Response `200 OK`**:
  ```json
  [
    "ws-user123",
    "user123_my-development-notes"
  ]
  ```

### 3.2 워크스페이스 생성 (Create Workspace)
- **`POST /api/v1/workspaces`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **Request Body**:
  ```json
  {
    "name": "my-development-notes"
  }
  ```
- **동작**: 비로그인 사용자는 차단(401)되며, 입력받은 이름의 공백 및 영문 이외 문자를 하이픈(-)으로 정제한 뒤, 사용자의 `[username]_`를 접두사로 붙여 격리된 물리적 폴더를 생성합니다.
- **Response `200 OK`**:
  ```json
  {
    "workspace_id": "user123_my-development-notes",
    "message": "Workspace created successfully."
  }
  ```
- **Response `409 Conflict`**: 이미 동일한 명칭의 워크스페이스가 존재할 때 발생.

### 3.3 워크스페이스 지식 그래프 조회 (Get Workspace Graph)
- **`GET /api/v1/workspaces/{workspace_id}/graph`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **Response `200 OK`**:
  ```json
  {
    "nodes": [
      {
        "id": "harness-framework",
        "name": "하네스 프레임워크",
        "type": "concept",
        "val": 5
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

### 3.4 워크스페이스 데이터 백업 내보내기 (Export Workspace)
- **`GET /api/v1/workspaces/{workspace_id}/export`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **Response `200 OK`**: `application/zip` 파일 스트림 (해당 워크스페이스의 Neo4j 노드 데이터 백업 json과 물리 마크다운 위키 파일들을 포함하는 압축 파일).

---

## 4. Wiki Page API (마크다운 페이지 조회)
지식 위키 본문의 실제 마크다운 텍스트를 조회합니다.

### 4.1 위키 페이지 본문 조회 (Get Wiki Page)
- **`GET /api/v1/wiki/{conceptName}`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}` (또는 `X-Workspace-ID` / 쿼리 파라미터 `workspaceId`)
- **Path Parameter**:
  - `conceptName`: 한글/영어/공백이 포함된 개념명(예: `하네스 프레임워크`) 또는 영문 slug(예: `harness-framework`).
- **동작**: 경로 조작(Path Traversal) 방지 필터를 거친 뒤, 동적 슬러그 해석(Dynamic Slug Resolution)을 통해 Neo4j 지식 그래프를 조회하여 실제 마크다운 파일 경로를 유추하고 해당 파일 본문을 반환합니다.
- **Response `200 OK`**:
  ```json
  {
    "content": "---\ntitle: \"하네스 프레임워크\"\ntype: concept\n---\n# 하네스 프레임워크\n\n하네스 프레임워크는..."
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

## 5. Query API (REST 기반 자연어 질의)
지식 위키 및 그래프를 탐색하여 질문에 자율적으로 응답하는 엔드포인트입니다.

- **`POST /api/v1/query`**
- **Headers**: `Authorization: Bearer {JWT_TOKEN}`
- **Request Body**:
  ```json
  {
    "question": "LLM 위키와 일반 RAG의 아키텍처적 차이는 무엇인가요?",
    "file_back": true,
    "useNotion": true
  }
  ```
- **동작**: `LangChain4j` 기반의 Agentic Graph Traversal을 수행하여 논리적 지식 지도를 구축 및 응답합니다. `file_back`이 true일 경우 응답 결과로 신규 위키 인사이트 페이지를 자동 생성합니다.
- **Response `200 OK`**:
  ```json
  {
    "answer": "LLM 위키는 사후 RAG와 달리...",
    "sources_cited": ["wiki/concepts/llm-wiki.md", "wiki/concepts/rag.md"],
    "filed_back_path": "wiki/insights/llm-wiki-vs-rag.md"
  }
  ```

---

## 6. Notification SSE API (실시간 알림 스트림)
지식 탐색 중 실시간 이벤트를 전송받기 위한 Server-Sent Events 엔드포인트입니다.

- **`GET /api/v1/notifications/sse`**
- **Query Parameters**:
  - `token`: JWT 토큰 (또는 WebSocket/SSE 연결 제약 상의 쿼리 주입 허용)
  - `workspaceId`: 대상 워크스페이스 ID
- **동작**: 클라이언트 브라우저와 비동기 커넥션을 수립하고, AI 에이전트 활동 이벤트를 스트리밍합니다.
- **발행 이벤트 명세 (`ai_reading`):**
  - AI 에이전트가 특정 개념 노드를 조회하거나 읽어 들일 때 발생합니다.
  - **Payload**:
    ```json
    {
      "nodeId": "harness-framework"
    }
    ```

---

## 7. Model Context Protocol (MCP) API
외부 AI 도구(Cursor, Claude Desktop 등)와 연동하기 위한 공식 Spring AI MCP (SSE 방식) 규약 엔드포인트입니다.

- **GET `/mcp/sse`**: JSON-RPC 2.0 프로토콜을 수행하기 위한 최초 SSE 연결 엔드포인트.
- **POST `/mcp/message`**: 클라이언트가 툴 실행 및 리소스 조회 메시지를 전송하는 엔드포인트.

### 7.1 제공 Tools 리스트
- `searchGraph(concept_name: String)`: Neo4j 그래프에서 인접 노드 및 관계 정보 검색.
- `getNodeContext(concept_name: String)`: 특정 노드의 관계망 요약을 텍스트 형식으로 조회 (호출 시 `ai_reading` 이벤트 발행).
- `readWikiPage(page_path: String)`: 지정한 마크다운 위키 페이지 본문 내용을 조회 (호출 시 `ai_reading` 이벤트 발행).
- `fileBackInsight(title: String, content: String)`: 에이전트가 도출한 통찰을 새로운 마크다운 파일로 영구 적재.
- `listRecentChanges()`: 최근 위키 변동 기록 목록 조회.
