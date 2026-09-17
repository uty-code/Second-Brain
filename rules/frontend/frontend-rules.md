# AIMS-Graph 프론트엔드 개발 및 디자인 규칙 (frontend-rules.md)

이 파일은 AIMS-Graph 프론트엔드(Next.js) 개발 및 UI/UX 개선 시 에이전트가 준수해야 할 디자인 제약 사항과 코딩 표준을 정의합니다.

---

## 1. UI/UX 디자인 에스테틱 및 AI Slop 배제
- **가이드 준수**: 프론트엔드 UI 컴포넌트 추가/수정 전 반드시 [UI_GUIDE.md](file:///c:/second%20brain/docs/UI_GUIDE.md)를 탐독하고, 단순하고 저품질의 디자인 안티패턴(Plain CSS, 모호한 원색 매핑, 어색한 그라데이션)을 배제해야 한다.
- **풍부한 미학 구현**: 
  - 기본 브라우저 서체를 피하고 Google Fonts(Outfit, Inter 등)를 사용한다.
  - sleek한 다크 모드, HSL 기반의 테마 컬러 매칭, 부드러운 글라스모피즘(Glassmorphism) 및 세련된 미세 애니메이션(Micro-animations)을 풍부하게 적용하여 프리미엄 느낌을 준다.
  - 이미지가 필요한 곳에 더미 placeholder 대신 이미지 생성 툴 등을 사용해 고해상도 그래픽 리소스를 생성 및 매핑한다.

## 2. 스타일링 시스템 및 UI 컴포넌트 표준
- **Tailwind CSS 전역 규격 준수**: 전역 테마 변수(`globals.css` 및 `tailwind.config.js`)에 선언된 디자인 토큰(색상표, 라운딩 값 등)을 적극 재사용하며, 임의로 하드코딩된 원색 클래스(예: `bg-red-500`)를 지양한다.
- **Shadcn UI 연동**: Shadcn UI로 설치된 기본 UI 구성요소(`button`, `dialog`, `dropdown-menu` 등)의 구조적 계층을 파괴하지 않고 일관되게 활용한다.
- **3-Pane 레이아웃 준수**: 메인 지식 탐색 화면은 왼쪽 사이드바, 중앙 캔버스(그래프 렌더링 영역), 오른쪽 지식 상세 뷰어/챗 패널로 구성된 3-Pane 레이아웃의 비동기 상태 변화 규칙을 엄격히 유지해야 한다.

## 3. 상태 관리 및 렌더링 최적화
- **Zustand 전역 상태**: 사용자 인증 정보(`token`, `currentUser`), 그래프 데이터, 활성화된 노드 정보는 Zustand 전역 상점(`useAppStore`)에 정의하여 관리한다.
- **렌더링 최적화**: 캔버스 렌더링과 대규모 노드 업데이트 시 렌더링 루프 병목이 발생하지 않도록 렌더링 의존성(dependency array)과 React Memoization을 정교하게 제어한다.

## 4. B2B 라우팅 및 접근 제어
- **Auth Guard**: Next.js의 라우팅 필터 또는 미들웨어를 통해 JWT가 만료되거나 없는 사용자가 `/login` 이외의 주소로 접근 시 `/login` 페이지로 강제 리다이렉트시키는 보안 장치를 엄격히 준수한다.
- **로그아웃 및 세션 폭파**: 로그아웃 시 Zustand 전역 상태 초기화 및 RDBMS/Redis 백엔드 세션 파기를 즉시 연동하여 브라우저 로컬 데이터 오염을 원천 방어한다.
