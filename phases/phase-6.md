# Phase 6 지시서: 표준 MCP (SSE) 서버 마이그레이션

> **Status: [완료됨]**
> - **히스토리 (Phase 6)**: 외부 `mcp-spring-boot-starter` 의존성을 가져올 수 없는 환경 제약으로 인해, 자체 경량화 프레임워크(`@McpTool`, `Registry`)를 직접 구현하여 우회 연동함.
> - **히스토리 (Phase 6.5)**: 네트워크 제약 이슈가 해소됨을 확인하고, 자체 구현 코드를 폐기한 뒤 `org.springframework.ai:spring-ai-mcp-server-webmvc-spring-boot-starter` 기반의 공식 표준 아키텍처로 원상복구 및 마이그레이션 완료.

## 1. 개요 및 목표
- 기존 REST API 방식으로 흉내 내던 Pseudo-MCP(`McpController`)를 폐기하고, AIMS-Graph 백엔드를 **표준 JSON-RPC 2.0 규격의 MCP 서버(SSE 기반)**로 승격시킵니다.
- 이를 통해 Claude Desktop, Cursor, Antigravity 등의 외부 에이전트가 어떠한 미들웨어 브릿지 없이도 우리 백엔드에 다이렉트로 접속해 지식 그래프(Neo4j)와 통신할 수 있게 만듭니다.

## 2. 참조 문서
- `docs/API_SPEC.md` (REST 대신 공식 MCP 규격 참고)
- Spring Boot MCP 공식 레퍼런스 가이드

## 3. 구현 내용 목록
1. **MCP Starter 라이브러리 추가**
   - `build.gradle`에 `mcp-spring-boot-starter` (또는 해당하는 Java MCP SDK) 의존성 추가.
2. **SSE 엔드포인트 활성화 (`com.aimsgraph.mcp` 패키지 개편)**
   - 기존의 `POST /api/v1/mcp/tools/execute` 대신 SDK에서 제공하는 `/mcp/sse` 라우터를 설정합니다.
   - MCP Tool 어노테이션(`@McpTool` 등 라이브러리에 맞게)을 사용하여 `search_graph`, `read_wiki_page` 등의 메서드를 등록합니다.
3. **보안 및 인증 연동**
   - 클라이언트(Claude Desktop)가 HTTP 헤더를 통해 SSE 연결 시도 시, 기존처럼 `JwtInterceptor`를 거쳐 `workspaceId`를 가져와 세션에 유지(Stateful)하거나, 요청마다 식별하도록 아키텍처를 연동합니다.

## 4. 제약 사항
- 기존 `McpControllerTest`는 더 이상 유효하지 않으므로, SSE 스트리밍에 대한 MCP 컨트랙트 단위 테스트를 재작성해야 합니다.
- 작업 완료 후 `c:\second brain\tasks.md` 파일에 Phase 6 항목을 추가하고 `[x]`로 완료 표시하세요.
