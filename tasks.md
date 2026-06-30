# Tasks: AIMS-Graph Backend Project

## Objective
`docs/` 폴더를 기반으로 세컨드 브레인 백엔드 파이프라인 개발

## Phases
- [x] Phase 1: 기반 인프라 및 보안 설정 (Spring Boot 3.x, DB 연동, JWT/BYOK 설정)
- [x] Phase 2: 데이터 통합 파이프라인 (Entity/Neo4j 스키마, Transactional Outbox, Kafka 연동)
- [x] Phase 3: Ingestion Worker (에이전틱 지식 추출, Redis 및 마크다운 스키마 및 Neo4j 증분 갱신)
- [x] Phase 4: API 서빙 및 MCP 통합 (REST API, Anthropic MCP 엔드포인트, File-back 로직)
- [x] Phase 5: 자가 치유 지식 린터 (Self-Healing Daemon 및 Auto-fix)
- [x] Phase 6: 공식 MCP (SSE) 서버 마이그레이션
- [x] Phase 7: 백엔드 고도화 (다수 에이전틱 그래프 탐색, Rate Limiting, 알림 SSE, Export API, 동적 온톨로지 주입)
- [x] Phase 8: 프론트엔드 초기 설정 및 구조화 (Next.js, 3-Pane 레이아웃)
- [x] Phase 9: 중앙 캔버스 및 지식 그래프 렌더링 (리액션 이벤트 및 Empty State)
- [x] Phase 10: 인터랙션 구현 및 백엔드 SSE/MCP 연동 (Hover/Click 이벤트, 마크다운 뷰어)
- [x] Phase 11: LLM 쿼리 컨텍스트 최적화 (RAG 지양, Local Wiki Markdown 기반 Context Retrieval 테스트 구축)
- [x] Phase 12: B2B 멀티테넌시 및 접근 제어 (JWT, Spring Security, Next.js AuthGuard)
- [x] Phase 13: B2B 로그아웃 및 탈퇴 (Redis Blacklisting, Frontend 초기화)
- [x] Phase 14: 지식 추출 안정화 (API 레벨 Structured Outputs 통합)
