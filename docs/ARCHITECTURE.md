# Architecture Specification: AIMS-Graph

## 1. 시스템 핵심 철학 (Andrej Karpathy's LLM Wiki Pattern)
이 엔터프라이즈 AIMS-Graph 시스템은 안드레이 카파시(Andrej Karpathy)가 제안한 **'LLM 위키(LLM Wiki)' 패턴**을 근간으로 설계되었습니다. 
> 🚫 **RAG(문서 청킹, 벡터 임베딩, 벡터 DB)의 사용을 전면 금지합니다.** 
> 오직 에이전틱 워크플로우(Agentic Workflow)를 통해 입력된 원본 소스(Raw sources)를 상호 링크가 걸린 정형화된 마크다운 위키(Processed Wiki) 파일과 인덱스(Index)로 변환·누적하는 방식만을 취합니다.

1. **Raw Sources (불변 원본)**: 수집된 원본 데이터(Notion, Slack, Git 등). LLM은 이를 읽기만 하며 절대 수정하지 않습니다.
2. **The Wiki (지식 위키)**: 에이전틱 워크플로우를 거친 LLM이 100% 소유하고 관리하는 마크다운 파일(및 Neo4j 그래프) 디렉토리. 엔티티 페이지, 요약, 서로 연결된 지식망(Index 포함)이 이곳에 축적됩니다.
3. **The Schema (규칙/스키마)**: LLM이 위키를 일관되게 관리하도록 지시하는 규칙(예: `GEMINI.md`, `rules/common/project-rules.md`).

## 1.1 디렉토리 구조 (Java 패키지)
```text
AIMS-Graph-Backend/
├── docker-compose.yml
├── build.gradle
├── src/main/java/com/aimsgraph/
│   ├── AimsGraphApplication.java
│   ├── config/                    # Spring 설정 (DB, Kafka, Redis, Neo4j)
│   ├── auth/                      # 멀티 테넌시 인증 및 JWT 인터셉터 (workspace_id 주입)
│   ├── domain/                    # 도메인 엔티티 및 비즈니스 로직
│   │   ├── source/                # RawSource 관련 (Entity, Repository, Service)
│   │   ├── wiki/                  # WikiPage 관련
│   │   └── graph/                 # Neo4j 노드/관계 관련
│   ├── outbox/                    # 아웃박스 패턴 (OutboxEvent Entity, Poller)
│   ├── ingest/                    # Ingest 오퍼레이션 (Kafka Consumer, Worker)
│   ├── query/                     # Query 오퍼레이션 (LLM 연동, File-back)
│   ├── lint/                      # Lint 오퍼레이션 (Self-Healing Daemon)
│   ├── mcp/                       # MCP 표준 엔드포인트 (Tools, Resources)
│   └── api/                       # REST API 컨트롤러
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── mapper/                    # MyBatis XML 매퍼
└── src/test/java/com/aimsgraph/   # 테스트 (도메인별 미러링)
```

## 2. 핵심 오퍼레이션 파이프라인 (Ingest, Query, Lint)
[Client API] ──> 1. MSSQL(로컬 DB)에 노트 메타데이터 및 변경 이벤트 저장 (동일 트랜잭션)
 │
 └──> 2. Debezium / CDC (또는 Poller)가 Outbox 테이블 감지
 │
 └──> 3. Apache Kafka 고속 메시지 발행
 │
 ├──> 4. Worker A: S3 / Git 파일 시스템에 마크다운 영속화
 └──> 5. Worker B: Neo4j Cypher 쿼리 실행 (Graph DB 반영)

- **Ingest (수집 및 통합)**: Kafka를 통해 새로운 소스가 인입되면, Worker Agent가 소스를 읽고 요약을 작성하며, 관련 엔티티/개념 페이지 및 `index.md`(전체 카탈로그)와 `log.md`(연대기적 작업 기록)를 동시에 업데이트합니다. 특히, 생성되는 위키 페이지의 `relatedConcepts`는 LLM 환각(Hallucination) 방지를 위해 실제 그래프 상의 엣지 정보와 대조하여 실제 관계가 존재하는 노드만 교차 검증하여 위키 파일 내에 렌더링합니다.
- **Query (탐색 및 재기록)**: MCP 엔드포인트를 통해 질문이 들어오면 LLM이 위키를 검색해 답변합니다. 도출된 훌륭한 분석이나 비교표는 휘발성 채팅으로 남기지 않고 **새로운 위키 페이지로 다시 저장(Filed back)**되어 지식을 복리(Compound)로 증식시킵니다.
- **Lint (자가 치유 및 검증)**: 데몬(Daemon)이 정기적으로 위키를 스캔하여 모순된 주장, 구식 데이터, 고아 문서(Orphan pages), 누락된 교차 참조를 찾아내고 갱신합니다.

## 3. 전체 시스템 아키텍처 (Layered & EDA)
1. **Ingestion 레이어**: Webhook Receivers, Log Parsers, Apache Kafka
   - Raw Sources로부터 이벤트를 받아 유실 없이 실시간 버퍼링(Ingest 트리거).
2. **Agentic Core 레이어**: Multi-Agent Worker Group, Redis Distributed Lock
   - Kafka 메시지(이벤트)를 컨슘할 때 시스템 전역 환경 변수(`OPENAI_API_KEY`) 또는 `llm.api-key` 설정을 로드하여 LLM 서비스를 구동합니다. (데이터베이스 내 개별 API 키 관리 및 BYOK 구조 폐기)
   - 전역 API 키를 활용하여 LLM Wiki Ingest 프로세스 수행 (문서 요약, 교차 참조, `index.md`, `log.md` 업데이트).
3. **Storage & Graph 레이어**: MSSQL, Git/S3 (Markdown), Neo4j, Redis Cache
   - RDBMS(MSSQL)를 통한 트랜잭션 관리와, 마크다운 위키(The Wiki), 고속 조회를 위한 온톨로지 그래프(Neo4j)의 이원화 동기화.
4. **Knowledge Lifecycle 레이어**: Self-Healing Daemon
   - 백그라운드 Lint 프로세스를 가동하여 지식 린팅 및 무결성 유지.
5. **Serving 레이어**: Spring Boot API Server, Spring AI MCP Server, JWT Auth
   - 인증 토큰을 파싱하여 해당 계정/토큰에 접근이 허용된 '세컨드 브레인(workspace_id)' 목록을 추출하고, 쿼리 실행 시 허용된 공간만 탐색하도록 다중 테넌트 접근 제어(RBAC) 적용.
   - 공식 Spring AI MCP (SSE) 표준 인터페이스를 통해 Query 오퍼레이션을 수행하고, RBAC 필터링 서빙.

## 4. 기술 스택
> 상세 기술 스택은 프로젝트 헌법 `GEMINI.md`의 `## 기술 스택` 섹션을 참조.
