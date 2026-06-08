# Phase 10: 인터랙션 구현 및 백엔드(SSE/MCP) 연동

> **Status: [완료됨]**
- **히스토리**:
  - `GraphCanvas.tsx` 내 노드 Hover, Click 이벤트 (카메라 줌/패닝 및 인접 노드 포커싱) 구현.
  - `useAppStore`를 통해 노드 클릭 시 전역 상태에 `selectedNodeId` 업데이트 로직 추가 (`page.tsx`).
  - `RightPanel.tsx`에 `Skeleton` 컴포넌트를 이용한 비동기 지연 시간 대응 로직 구현.
  - `react-markdown` 패키지 추가 및 `MarkdownViewer.tsx` 작성하여 우측 패널에 연동.
  - `api.ts` 파일 생성하여 `EventSource` 기반 SSE/MCP 통신 클라이언트 초안 구현.


## 작업 범위
1. 노드 Hover/Click 인터랙션 구현 (Hover 시 인접 노드 포커싱, Click 시 우측 패널 연동 및 카메라 이동).
2. 우측 패널에 Markdown 문서 뷰어 컴포넌트 추가 (`react-markdown` 등).
3. 스켈레톤(Skeleton) 로딩 UI 구현.
4. 프론트엔드를 MCP 클라이언트로 동작하게 하여 백엔드 SSE 엔드포인트와 통신 연동 (초안).

## 생성 및 수정할 파일 목록
- `frontend/src/components/graph/GraphCanvas.tsx` (인터랙션 이벤트 추가)
- `frontend/src/components/viewer/MarkdownViewer.tsx`
- `frontend/src/components/ui/Skeleton.tsx`
- `frontend/src/services/api.ts` (SSE 연동 로직)

## 성공 기준
- 특정 노드 클릭 시 우측 패널에 해당 노드의 문서가 마크다운으로 렌더링됨.
- 노드 호버 시 관련된 간선과 노드만 하이라이트 처리됨.
- 긴 대기 시간 발생 시 스피너 대신 스켈레톤 로딩이 표시됨.
