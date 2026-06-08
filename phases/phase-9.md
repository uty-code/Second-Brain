# Phase 9: 중앙 캔버스 및 지식 그래프 렌더링

> **Status: [완료됨]**
> - **히스토리**:
>   - `react-force-graph-2d`, `react-dropzone`, `lucide-react` 패키지 설치 완료.
>   - `UI_GUIDE.md` 5번 항목의 지침에 따라 `GraphCanvas` 컴포넌트 생성. 단색(zinc-200), 직선(zinc-600), 투명 라벨, Hover 상호작용 구현 완료.
>   - `react-dropzone`을 활용한 파일 업로드 대응 `EmptyState` 컴포넌트 구현.
>   - `page.tsx`에 더미 데이터를 주입하여 그래프 렌더링 테스트 통과.

## 작업 범위
1. `react-force-graph-2d` 설치 및 중앙 캔버스 컴포넌트 생성.
2. 노드, 간선, 라벨에 대한 `UI_GUIDE.md` 5번 항목(옵시디언 스타일)의 엄격한 스타일링 적용.
3. 데이터가 없을 때 표시될 직관적인 Empty State UI 및 업로드 드롭존(Dropzone) 구현.
4. 더미 그래프 데이터를 활용한 렌더링 테스트.

## 생성 및 수정할 파일 목록
- `frontend/src/components/graph/GraphCanvas.tsx`
- `frontend/src/components/graph/EmptyState.tsx`
- `frontend/src/types/graph.ts`

## 성공 기준
- 중앙 영역에 그래프 캔버스가 표시되며, 더미 노드들이 단색/직선으로 깔끔하게 그려짐.
- 데이터가 없을 때 중앙에 업로드 CTA 문구와 드롭존 UI가 나타남.
