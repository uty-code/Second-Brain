---
title: "AIMS-Graph Glossary"
type: reference
created_at: 2026-07-18
updated_at: 2026-07-18
---

# AIMS-Graph Glossary (핵심 도메인 용어집)

AIMS-Graph 프로젝트에서 쓰이는 핵심 도메인 용어들과 아키텍처 패턴의 상세 정의입니다.

## 1. 지식 위키 관련 개념

### LLM Wiki (Second Brain)
안드레이 카파시(Andrej Karpathy)가 제안한 지식 관리 사상으로, 정보를 매번 1회성 벡터 검색(RAG)으로 재조립하지 않고, 의미 있는 소스가 유입될 때마다 AI 에이전트가 이를 정제하고 상호 교차 참조 링크가 걸린 **마크다운 문서망**으로 병합·축적하는 패턴을 뜻합니다.

### The Wiki (지식 위키)
에이전틱 프로세스를 통해 생성된, AI가 완전히 소유하고 주기적으로 가꾸는 마크다운 파일들의 디렉토리입니다. `wiki/concepts/`, `wiki/entities/`, `wiki/insights/` 하위에 배치되며, 중앙 인덱스인 `index.md`와 연대기 변경 이력인 `log.md`에 의해 카탈로그화됩니다.

### 교차 참조 (Cross-Referencing)
문서 간에 `[[Concept Name]]` 형태로 거는 양방향 지식 링크를 말합니다. 이 링크들은 단순 텍스트가 아니라 `EXTENDS`, `CONTRADICTS`, `DEPENDS_ON` 등 논리적 성격을 갖는 온톨로지 에지(Edge) 형태로 변환되어 [SCHEMA.md](file:///c:/second%20brain/docs/SCHEMA.md)에 따라 Neo4j 데이터베이스에 저장됩니다.

### File-back (재귀적 영속화)
사용자와의 채팅(Query) 도중에 도출된 유용한 통찰이나 요약 분석 결과를 휘발성 대화 기록으로 버리지 않고, 에이전트가 자율적으로 판단하여 다시 지식 위키 디렉토리에 마크다운 파일로 내려쓰는 프로세스입니다.

---

## 2. 보안 및 테넌시 관련 개념

### Vault (금고 / 워크스페이스)
B2B 환경에서 사용자별로 소유하는 독립된 지식 창고의 논리적/물리적 단위입니다. 물리적 파일 디렉토리(`workspaces/ws-[username]`) 및 Neo4j의 노드 엣지가 타 사용자와 철저히 격리됩니다.

### BYOK (Bring Your Own Key)
사용자가 Notion, GitHub, OpenAI 등 외부 연동에 필요한 개인 API 토큰을 직접 제공하는 보안 모델입니다. 토큰은 브라우저에 임시 노출되지 않고, 백엔드 데이터베이스에 AES-256 알고리즘으로 암호화되어 관리됩니다.

---

## 3. 백엔드 아키텍처 패턴

### Transactional Outbox (트랜잭셔널 아웃박스)
관계형 DB(MSSQL) 트랜잭션의 일부로 이벤트를 `Outbox` 테이블에 영속화한 뒤, 비동기 poller가 이를 Kafka로 발행하는 패턴입니다. 물리 마크다운 파일 시스템 쓰기와 Neo4j DB 적재 시 발생할 수 있는 이중 쓰기 부분 장애(Partial Failure)를 방지하고 최종 일관성을 달성합니다.

### AI Gaze Tracking (실시간 탐색 시각화)
AI 에이전트가 지식 그래프를 종단/횡단 탐색할 때, 현재 읽고 있는 노드를 SSE(Server-Sent Events) 이벤트를 통해 브라우저로 스트리밍하여, 웹 UI에 글로우 링 및 에메랄드 하이라이트로 실시간 시각화해 주는 기능입니다.

### Self-Healing Daemon (자가 치유 린터)
백그라운드에서 주기적으로 고아 페이지(`ORPHAN_PAGE`), 깨진 링크(`BROKEN_LINK`), 모순 정보(`CONTRADICTION`) 등을 검사하고 에이전트를 통해 자동 치유(`Auto-fix`)하는 린터 스케줄러입니다.
