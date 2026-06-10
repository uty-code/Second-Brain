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

## 2026-06-10: B2B Login Page & JWT Auth [완료됨]
- Goal: Implement JWT-based login page and Spring Security on backend for multi-user support.
- Status: Done
