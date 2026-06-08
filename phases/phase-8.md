# Phase 8: 프론트엔드 초기 설정 및 구조화

> **Status: [완료됨]**
- **히스토리**:
  - Next.js 16 (App Router) + Tailwind CSS v4 환경으로 `frontend` 프로젝트 생성.
  - Tailwind v4 환경에 맞춰 Shadcn UI 초기 설정 (`globals.css` 기반 구성 적용, `tailwind.config.ts` 미사용).
  - `UI_GUIDE.md`를 준수하여 다크모드(`bg-zinc-900`) 기반의 1px 실선 분리(`border-zinc-800`)를 적용한 3-Pane 레이아웃(`Sidebar.tsx`, `RightPanel.tsx`, `page.tsx`) 구축.
  - 전역 상태 관리를 위한 Zustand 스토어(`useAppStore.ts`) 초기 세팅.

## 작업 범위
1. Next.js App Router 초기화 및 `frontend` 디렉토리 구성.
2. Tailwind CSS 및 Shadcn UI 연동 (`UI_GUIDE.md`에 명시된 다크모드 및 Color System 적용).
3. 3-Pane 레이아웃 뼈대 구축 (Left Sidebar, Central Canvas, Right Panel).
4. 전역 상태 관리 (Zustand) 초기 세팅.

## 생성 및 수정할 파일 목록
- `frontend/package.json`
- `frontend/tailwind.config.ts`
- `frontend/src/app/layout.tsx`
- `frontend/src/app/page.tsx`
- `frontend/src/components/layout/Sidebar.tsx`
- `frontend/src/components/layout/RightPanel.tsx`
- `frontend/src/store/useAppStore.ts`

## 성공 기준
- `npm run dev` 실행 시 에러 없이 3-Pane 구조의 정적인 화면이 나타남.
- 다크모드 배경색(`#18181B`)과 1px 실선 테두리가 정확하게 렌더링됨.
- AI Slop(Glassmorphism, 둥근 모서리 남용 등) 요소가 존재하지 않음.
