## [2026-06-09] Chat-Notion MCP 연동 기능 추가 (Micro-Task)
- **Goal:** 채팅(Query) API 호출 시 명시적인 파라미터(useNotion=true)가 주어질 때만 Notion 문서를 검색하여 LLM에 프롬프트로 주입.
- **Affected Files:**
  - QueryController.java: 선택적 파라미터 useNotion, 
otionPageId 추가.
  - LlmService.java: query 메서드에서 useNotion이 true일 경우 NotionIngestService 호출 및 프롬프트 병합.
  - NotionIngestService.java: 검색 또는 페이지 조회 보조 메서드 추가.
  - API_SPEC.md: API 명세 갱신.
- **Constraints:**
  - 기존 로컬 마크다운 검색 로직 훼손 금지.
  - 매번 Notion을 호출하여 레이턴시를 낭비하지 않도록 파라미터로 엄격히 분리할 것.
## [2026-06-09] GraphCanvas MCP 모달 토글 버그 수정 (Micro-Task)
- **Goal:** 프론트엔드의 MCP 연결 플로팅 버튼이 누를 때마다 켜졌다 꺼졌다(토글) 되도록 수정.
- **Affected Files:**
  - c:\second brain\frontend\src\components\graph\GraphCanvas.tsx
- **Details:** 
  - onClick={() => setShowMcpModal(true)} 로 하드코딩된 부분을 onClick={() => setShowMcpModal(!showMcpModal)} 또는 (prev => !prev)로 변경.
## [2026-06-09] ChatPanel 노션 토글 버튼 UI 개선 (Micro-Task) [완료됨]
- **Goal:** 프론트엔드 채팅 패널의 노션 검색 토글 버튼에서 번잡한 텍스트를 제거하고 아이콘만 미니멀하게 렌더링.
- **Affected Files:**
  - c:\second brain\frontend\src\components\chat\ChatPanel.tsx
- **Details:** 
  - {useNotion ? "Notion Search: ON" : "Notion Search: OFF"} 텍스트 출력부 제거.
  - 아이콘만 남기고 버튼 패딩 등(px-2.5 -> p-2 등)을 아이콘 전용 버튼에 맞게 최적화.
## [2026-06-09] LlmService 챗봇 인지능력(Awareness) 프롬프트 개선 (Micro-Task) [완료됨]
- **Goal:** 채팅봇이 "노션 접근 권한이 없다"고 헛소리를 하는 문제(System Prompt 부재) 해결.
- **Affected Files:**
  - c:\second brain\aims-backend\src\main\java\com\aimsgraph\ingest\LlmService.java
- **Details:** 
  - query 메서드 내부의 enrichedQuery 프롬프트와 queryDirect 호출 시 사용되는 베이스 프롬프트를 강화.
  - "You are a helpful assistant integrated with Notion MCP and Second Brain Wiki. If the user asks if you have access to Notion, confidently reply YES, because the backend system dynamically injects Notion context when the user toggles the Notion button in the UI." 라는 식의 권한 인지 문구를 하드코딩 주입.
## [2026-06-09] 노션 전역 검색 기반 Agentic RAG 구현 (Feature)
- **Goal:** 사용자가 노션 페이지 ID를 명시하지 않더라도, AI가 질문 키워드를 바탕으로 노션 Search API를 호출하여 동적으로 가장 관련 있는 문서를 찾아오도록 구현.
- **Affected Files:**
  - c:\second brain\aims-backend\src\main\java\com\aimsgraph\ingest\NotionIngestService.java (searchNotionPageId 추가)
  - c:\second brain\aims-backend\src\main\java\com\aimsgraph\ingest\LlmService.java (query 메서드에서 fallback 검색 호출)

## 2026-06-10: Graph-based Agentic Traversal Implementation [완료됨]
- Issue: The current context retrieval passes all filenames to the LLM instead of traversing the Neo4j graph.
- Goal: Implement LangChain4j AiServices and Tools for true Agentic Graph Traversal.
- Plan: Create GraphTools with searchGraph, getNodeContext, readWikiPage, and readNotionPage. Modify LlmService.java to use AiServices.builder() for generating agentic responses.

## 2026-06-10: Remove Notion Page ID Manual Input
- Issue: The user wants to remove the manual Notion Page ID input field since the AI can now autonomously search Notion via Agentic Workflow.
- Goal: Remove Notion Page ID UI from frontend and simplify backend API to omit this field.

## 2026-06-10: Chat-to-Build Delegation (Save Insight Tool)
- Issue: The user wants to build the second brain via the chat interface without losing Zettelkasten quality.
- Goal: Add saveToSecondBrain tool to GraphTools so the Chat Agent can delegate knowledge ingestion to the dedicated wiki pipeline.
- Plan: Update GraphTools to support saving, pass model credentials, and enforce detailed summary extraction in the Chat Agent's prompt.

## 2026-06-10: RightPanel Resize Feature
- Issue: The user wants to resize the right panel (chat/viewer) by dragging its left border.
- Goal: Implement drag-to-resize functionality in RightPanel.tsx using React state and mouse events.

## 2026-06-10: GitHub MCP Connection Implementation [완료됨]
- Goal: Implement GitHub MCP connection UI and token verification in GraphCanvas, api.ts, and useAppStore.ts.
- Status: Done

## 2026-06-10: BYOK DB Encryption Migration [완료됨]
- Goal: Migrate API tokens from frontend localStorage to backend AES-encrypted DB storage.
- Status: Done

## 2026-06-10: B2B Login 초 & JWT Auth [완료됨]
- Goal: Implement JWT-based login page and Spring Security on backend for multi-user support.
- Status: Done

## 2026-06-11: 프론트엔드/백엔드 완전 로그아웃 (Redis Blacklisting) 구현 [완료됨]
- Goal: 프론트엔드 글로벌 로그아웃 버튼(Zustand 상태 초기화, /login 리다이렉트) 구현 및 백엔드 POST /v1/auth/logout API를 통한 Redis JWT Blacklisting 연동 (안 2 채택).

## 2026-06-11: 로그아웃 버튼 위치 변경 (UI)
- Goal: 사용성 개선을 위해 Sidebar.tsx 우측 상단의 로그아웃 버튼을 좌측 사이드바 하단(Footer) 영역으로 크게 이동.

## 2026-06-11: 사이드바 하단 사용자 계정 표시 (UI)
- Goal: 좌측 사이드바 하단의 로그아웃 버튼 옆에 현재 로그인한 사용자 아이디(`currentUser`)가 표시되도록 UI 레이아웃 개선.

## 2026-06-11: 로그인 직후 빈 화면(Empty State) 버그 수정
- Goal: 로그인 완료 직후 페이지 진입 시 새로고침 전까지 그래프가 보이지 않던 문제를 해결하기 위해, `page.tsx`의 렌더링 의존성(dependency array)에 `isLoggedIn` 상태를 추가하여 즉시 백엔드에서 데이터를 불러오도록 수정.

## 2026-06-11: B2B 계정 영구 탈퇴 기능 구현 [완료됨]
- Goal: B2B 보안 정책 및 데이터 격리 파기 요구사항에 맞추어, 프론트엔드 탈퇴 확인 경고 모달 UI 및 백엔드 DELETE /api/v1/auth/account API(Neo4j 노드 영구 삭제, 물리 마크다운 디렉토리 재귀 완전 삭제, RDBMS User 정보 삭제 일괄 트랜잭션 처리) 구현.

## 2026-06-23: AI 모델 선택 개선 및 가상 스레드 위키 쓰기 동시성 제어 [완료됨]
- **Goal:** 위키 페이지 생성 시 사용자가 선택한 AI 모델(DeepSeek v4 등)이 반영되지 않던 버그를 수정하고, 가상 스레드 환경에서 동일 위키 파일 동시 쓰기로 인한 Race Condition을 방지하도록 ReentrantLock 기반의 파일 락 장치 도입.
- **Affected Files:**
  - c:\second brain\aims-backend\src\main\java\com\aimsgraph\ingest\LlmService.java
  - c:\second brain\aims-backend\src\test\java\com\aimsgraph\ingest\LlmServiceTest.java

## 2026-06-23: LangChain4j 1.15.1 업그레이드 및 ChatModel 마이그레이션 [완료됨]
- **Goal:** 에이전트의 10회 도구 호출 제한 예외(Fallback 버그)를 근본적으로 차단하기 위해 LangChain4j 1.x 대 메이저 업그레이드를 수행하고, 이에 따른 ChatLanguageModel -> ChatModel API 마이그레이션을 적용합니다.
- **Affected Files:**
  - [build.gradle](file:///c:/second%20brain/aims-backend/build.gradle): langchain4j, langchain4j-open-ai 버전을 1.15.1로 상향 조정하고 langchain4j-core 의존성 추가
  - [LlmService.java](file:///c:/second%20brain/aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java): ChatLanguageModel을 ChatModel로 변경, generate()를 chat()으로 교체, AiServices 빌더에 maxToolCallingRoundTrips(50) 적용
  - [LlmServiceTest.java](file:///c:/second%20brain/aims-backend/src/test/java/com/aimsgraph/ingest/LlmServiceTest.java): ChatModel Mocking 및 ChatResponse 빌더 구조 테스트 적응
- **Details:**
  - 마이그레이션 완료 후 `./gradlew test` 검증 수행 완료.
  - 로컬 환경 로그인 토큰을 사용하여 `/api/v1/query` API를 연동 테스트하여 `searchGraph` -> `getNodeContext` -> `readWikiPage` 에 이르는 에이전트 sequential 툴 트래버설 흐름이 예외 없이 완벽하게 정상 동작함을 최종 검증 완료.

