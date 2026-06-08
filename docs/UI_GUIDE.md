# UI/UX 가이드라인 (UI_GUIDE.md)

이 문서는 AIMS-Graph 프론트엔드의 디자인 시스템과 시각적 품질을 통제하기 위한 가이드입니다. 
AI가 코드를 생성할 때 흔히 저지르는 **'AI Slop(저품질 양산형 디자인)'을 철저히 배제**하고, Linear나 Vercel과 같은 모던하고 프로페셔널한 B2B SaaS 느낌(솔리드하고 미니멀한 디자인)을 지향합니다.

## 1. 프론트엔드 프레임워크 스택
- **Core**: Next.js (App Router), React
- **Styling**: Tailwind CSS
- **Components**: Shadcn UI (접근성 높고 깔끔한 기본 컴포넌트 복사/붙여넣기 방식)
- **Graph Visualization**: `react-force-graph` 등 사용 (화려함 배제, 상세 스타일은 하단 5번 항목 참조)
- **State Management**: Zustand

## 2. 🚫 AI Slop 안티패턴 (절대 금지)
AI 코딩 에이전트가 습관적으로 추가하는 다음의 시각적 요소들은 **절대 사용하지 않습니다.**
- **Glassmorphism 금지**: `backdrop-blur`, 투명한 패널 겹치기 등 렌더링 리소스를 잡아먹고 난잡해 보이는 유리 질감 절대 금지.
- **네온 글로우 및 과도한 그림자 금지**: `drop-shadow-2xl`, 야광 효과, 빛 번짐 금지. 패널을 구분할 때는 그림자 대신 **아주 얇고 연한 테두리선(Border)**만 사용.
- **보라색/분홍색 그라데이션 텍스트 금지**: `bg-gradient-to-r from-purple-400 to-pink-600 text-transparent bg-clip-text`와 같은 뻔하고 유치한 AI 스타일 타이포그래피 절대 금지. 솔리드(단색) 컬러 텍스트 유지.
- **과도한 둥근 모서리 금지**: `rounded-full`, `rounded-3xl` 등 둥근 모서리 남용 금지. 최대 `rounded-lg` 또는 `rounded-md`로 단정함을 유지.

## 3. 색상 팔레트 (Color System)
차분하고 신뢰감 있는 모노크롬 기반의 팔레트를 사용합니다.
- **배경 (Background)**:
  - Light Mode: 순백색(`#FFFFFF`) 및 매우 옅은 회색(`bg-zinc-50`).
  - Dark Mode: 깊은 무채색 회색(`bg-zinc-950` 또는 `#09090B`). 순수 검정(`#000000`)은 눈이 부시므로 지양.
- **테두리 (Borders)**: 
  - 굉장히 얇고 미세한 선 (`border-zinc-200` / Dark: `border-zinc-800`).
- **포인트 색상 (Accent)**:
  - 눈이 아프지 않은 단일 솔리드 컬러 사용 (예: Vercel/Linear 스타일의 모노크롬 반전 색상이나 차분한 블루 `blue-600`).

## 4. 타이포그래피 및 레이아웃
- **폰트**: 기본 sans-serif 폰트(Inter, Geist) 혹은 한국어 지원 폰트(Pretendard). 복잡한 폰트 섞어 쓰기 금지.
- **여백 (Spacing)**: 컴포넌트 간 일관된 여백 사용. 빽빽한 배치를 피하고 여백을 충분히 두어 시선 분산을 막음.
- **구조 분리**: 사이드바(Vault), 중앙(Graph), 우측(Chat)의 3-Pane 구조를 따르되, 각 영역의 구분은 오직 1px의 실선(`border-r`, `border-l`)으로만 깔끔하게 분리.

## 5. 지식 그래프 시각화 (Graph Visualization) 스타일링
화려한 이펙트(네온, 3D, 글로우)를 완전히 배제하고, 극도로 정제된 옵시디언(Obsidian) 스타일의 단색 렌더링을 구현하기 위한 **구체적인 수치와 CSS/Hex 코드**입니다.

- **배경 (Canvas Background)**: 
  - 색상: `#18181B` (Tailwind `zinc-900`)
- **노드 (Nodes)**: 
  - 색상: `#E4E4E7` (Tailwind `zinc-200`)
  - 형태: 완벽한 평면 원형 (`box-shadow: none`)
  - 크기: 기본 노드 반경 `R=4px` (중요도/연결된 간선 수에 따라 최대 `R=8px`까지 비례 확대)
- **간선 (Edges/Links)**: 
  - 색상: `#52525B` (Tailwind `zinc-600`)
  - 두께: `width: 1px` (고정 실선)
  - 형태: 곡선(Bezier) 배제, 순수 직선(Straight Line) 렌더링. 끝부분에 길이 `4px`의 작고 예리한 화살표(Arrowhead) 추가.
- **텍스트 라벨 (Labels)**: 
  - 색상: 기본 `#A1A1AA` (Tailwind `zinc-400`), 활성화(Hover) 시 `#FFFFFF`
  - 폰트: `font-size: 10px` ~ `12px`, `font-weight: 400`, `font-family: 'Inter', sans-serif`
  - 배경: 완전 투명 (`background-color: transparent`), 텍스트 배경 박스(Bounding Box) 렌더링 절대 금지.

## 6. 인터랙션 및 UX (Interaction & User Experience)
전문적이고 부드러운 사용자 경험을 제공하기 위한 동적 동작 규칙입니다.

- **노드 호버(Hover) 및 클릭(Click) 액션**:
  - **Hover**: 특정 노드에 마우스를 올리면, 해당 노드와 직접 연결된(1-hop) 노드와 선만 100% 불투명도로 유지하고 나머지 전체 그래프는 투명도를 20%로 낮춰 시각적 집중(Focus)을 유도합니다.
  - **Click**: 노드를 클릭하면 카메라가 해당 노드로 부드럽게 이동(Pan & Zoom)하며, 즉시 우측 패널(Viewer)에 해당 개념의 마크다운 문서가 열리도록 연동합니다.
- **로딩 상태 (AI 대기 시간 처리)**:
  - AI가 대답을 생성하거나 그래프를 불러오는 긴 시간 동안 촌스러운 스피너(Spinner) 원형 아이콘을 지양합니다.
  - 대신, Linear나 Vercel에서 사용하는 은은하게 빛이 스쳐 지나가는 **스켈레톤(Skeleton) UI**나 텍스트 스트리밍 타이핑 효과를 사용하여 체감 대기 시간을 줄입니다.
- **Empty State (초기 빈 화면 / 온보딩 처리)**:
  - 처음 금고(Vault)를 생성하여 노드가 하나도 없을 때, 우측 채팅창으로 업로드를 유도하기보다는 **중앙 캔버스 정중앙에 직관적인 CTA(Call to Action) 영역**을 배치합니다.
  - 예: *"지식의 우주가 비어있습니다. PDF를 드래그 앤 드롭하거나 Notion URL을 붙여넣어 첫 번째 뇌세포를 깨우세요."*
  - 사용자가 파일을 떨어뜨리는(Drop) 즉시 중앙에서 첫 번째 노드가 뿅 하고 생성되는 애니메이션을 연출하여 강렬한 첫인상(Onboarding Experience)을 제공합니다.
- **반응형(Responsive) 폴딩**:
  - 3-Pane 구조는 데스크탑에 최적화되어 있으므로, 화면이 좁아지면(모바일/태블릿) 좌측(LNB)이나 중앙(Graph) 패널이 햄버거 메뉴나 슬라이드 서랍(Drawer) 형태로 자연스럽게 접히도록(Collapse) 설계합니다.
