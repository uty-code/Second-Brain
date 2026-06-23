# Architecture Specification: AIMS-Graph

## 1. 시스템 핵심 철학 (Andrej Karpathy's LLM Wiki Pattern)
엔터프라이즈 AIMS-Graph 시스템은 안드레이 카파시(Andrej Karpathy)가 제안한 **'LLM 위키(LLM Wiki)' 패턴**을 근간으로 설계되었습니다.
> **[CRITICAL] RAG(문서 청킹, 벡터 임베딩, 벡터 DB)의 사용은 어떠한 경우에도 금지됩니다.**
> 오직 에이전틱 워크플로우(Agentic Workflow)를 통해 입력된 원본 소스(Raw sources)를 분석하여 상호 링크가 촘촘하게 걸린 정형화된 마크다운 위키(Processed Wiki) 파일과 인덱스(Index) 형태로 변환 및 누적하는 아키텍처만을 유지합니다.

1. **Raw Sources (원본 소스)**: 수집된 원본 데이터(Notion, Slack, Git 등)로, LLM이 직접 읽기만 하며 변경하지 않습니다.
2. **The Wiki (지식 위키)**: 에이전틱 프로세스를 거친 LLM이 100% 소유하고 관리하는 마크다운 파일 및 Neo4j 그래프 디렉토리입니다. 엔티티 페이지, 요약, 상호 연결된 지식망(인덱스 포함)이 이곳에 축적됩니다.
3. **The Schema (규칙/스키마)**: LLM이 위키를 일관되게 관리하도록 지시하는 규칙(예: `GEMINI.md`, `rules/common/project-rules.md`)입니다.

## 1.1 디렉토리 구조 (Java 패키지)
```text
AIMS-Graph-Backend/
├── docker-compose.yml
├── build.gradle
├── src/main/java/com/aimsgraph/
│   ├── AimsGraphApplication.java
│   ├── config/                    # Spring 설정 (DB, Kafka, Redis, Neo4j)
│   ├── auth/                      # 멀티테넌시 인증 및 JWT 필터/인터셉터 (블랙리스트 검증 포함)
│   ├── domain/                    # 도메인 엔티티 및 비즈니스 로직
│   │   ├── source/                # RawSource 관련 (Entity, Repository, Service)
│   │   ├── wiki/                  # WikiPage 관련
│   │   ├── graph/                 # Neo4j 노드/관계 관련
│   │   └── workspace/             # 워크스페이스 격리 및 자격증명(WorkspaceCredentials) 관련
│   ├── outbox/                    # 아웃박스 패턴 (OutboxEvent Entity, Poller)
│   ├── ingest/                    # Ingest 파이프라인 (Kafka Consumer, Worker, LlmService)
│   ├── query/                     # Query 파이프라인 (LLM 연동, Agentic Graph Traversal)
│   ├── lint/                      # Lint 파이프라인 (Self-Healing Daemon)
│   ├── notification/              # 실시간 알림 전송 (SSE)
│   └── api/                       # REST API 컨트롤러
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── mapper/                    # MyBatis XML 매퍼
└── src/test/java/com/aimsgraph/   # 테스트 코드 (도메인별 미러링, TDD 준수)
```

## 2. 핵심 파이프라인 (Ingest, Query, Lint)
1. **Ingest (수집 및 통합)**: Kafka를 통해 새로운 소스가 인입되면, Worker Agent가 소스를 분석하고 요약을 생성하며, 관련 엔티티/개념 페이지 및 `index.md`(전체 카탈로그)와 `log.md`(이력 기록)를 갱신합니다. 위키 페이지 생성 시 `relatedConcepts`는 LLM 환각 방지를 위해 Neo4j 그래프 상의 실제 엣지(Edge) 정보를 교차 검증하여 유효한 연결만 마크다운 파일에 렌더링합니다.
2. **Query (검색 및 추론 - Agentic Graph Traversal)**: 
   - 단순 벡터 검색 대신, **LangChain4j AiServices 및 Tool Calling** 기능을 활용하여 지식을 탐색합니다.
   - LLM 에이전트가 질문을 받으면 스스로 판단하여 `searchGraph`(Neo4j 검색), `getNodeContext`(인접 컨텍스트 조회), `readWikiPage`(위키 본문 읽기) 등의 도구를 연속적으로 호출(Multi-turn)하여 그래프를 종단 및 횡단하며 논리적 답변을 도출합니다.
   - 대화 내용 중 새로 습득된 고차원 지식은 다시 위키 페이지에 기록(`file-back`)하여 지식을 복리로 증식시킵니다.
3. **Lint (자가 치유 및 검증)**: 데몬(Daemon) 프로세스가 주기적으로 위키를 스캔하여 모순된 주장, 고아 문서(Orphan pages), 누락된 교차 참조, 깨진 링크 등을 찾아내어 자동으로 교정합니다.

## 3. 전체 시스템 아키텍처 (Layered & EDA)
AIMS-Graph는 다중 계층 아키텍처 및 이벤트 주도 아키텍처(EDA)를 따릅니다.

1. **Ingestion Layer (수집 계층)**: Webhook Receiver, Log Parser, Apache Kafka 등을 통해 원본 이벤트를 버퍼링하고 순차 처리합니다.
2. **Agentic Core Layer (에이전트 계층)**: 
   - Kafka 메시지를 소비하여 LLM 서비스를 구동합니다.
   - **LangChain4j 에이전트**는 주어진 작업 지침에 맞게 위키 마크다운 파일 및 Neo4j 그래프 정보를 바탕으로 동적 순회를 처리합니다.
   - 데이터베이스 암호화 저장소(`WorkspaceCredentials`)에서 워크스페이스별 복호화된 외부 API 키(Notion, GitHub 등)를 조회하여 외부 서비스 데이터를 수집합니다.
3. **Storage & Graph Layer (저장 및 그래프 계층)**:
   - **MSSQL**: 트랜잭션 관리, 원본 메타데이터, 아웃박스 이벤트, 사용자 정보, 암호화된 API 토큰 관리.
   - **Git/로컬 디렉토리**: Zettelkasten 마크다운 위키 파일들을 실제 파일 시스템에 물리적으로 관리 및 보관. (가상 스레드 환경 내 병렬 파일 I/O 충돌을 방지하기 위해 파일 경로 기반의 `ReentrantLock` 동적 동기화 메커니즘을 적용하여 스레드 피닝 현상을 차단하고 데이터 무결성을 보장)
   - **Neo4j**: 마크다운 위키의 구조를 지식 그래프 온톨로지 스키마로 모델링하여 초고속 관계 탐색 제공.
   - **Redis (Redisson)**: 분산 락 구현, JWT 블랙리스트 캐싱.
4. **Knowledge Lifecycle Layer (생명주기 계층)**: 백그라운드 Self-Healing Daemon이 동작하며 지식 품질 린팅 및 정합성을 주기적으로 복구합니다.
5. **Serving Layer (서빙 및 보안 계층)**:
   - Spring Security 및 JWT 토큰 파싱을 적용하여 B2B 멀티테넌시 구조를 가집니다.
   - 요청 시 헤더 또는 쿼리 파라미터에서 추출한 토큰 정보를 바탕으로 사용자의 고유 워크스페이스 영역(`ws-username`, `username_`)에 대한 논리적/물리적 접근 제어를 제공합니다.
   - Spring AI MCP 및 SSE 엔드포인트를 구현하여 외부 인공지능 에이전트 및 웹 클라이언트 인터랙션을 서빙합니다.

## 4. 실시간 탐색 시각화 (AI Gaze Tracking)
AI 에이전트가 지식 그래프를 자율적으로 순회할 때, 사용자가 AI의 현재 관심 사항(Gaze)을 실시간으로 인지할 수 있도록 돕는 시각화 아키텍처입니다.
- **백엔드**: `LlmService` 내의 에이전트 도구(`getNodeContext`, `readWikiPage` 등)가 실행될 때마다, `NotificationController`를 통해 해당 노드 ID 정보를 포함한 `ai_reading` 이벤트를 특정 워크스페이스 사용자에게 SSE로 실시간 브로드캐스트합니다.
- **프론트엔드**: 클라이언트는 `/v1/notifications/sse` 커넥션을 맺고 실시간 스트림을 구독합니다. `ai_reading` 수신 시 `activeAiNodes` 전역 상태에 노드 ID를 등록하여 캔버스 내 해당 노드를 **에메랄드 그린 컬러 및 글로우 링(Glow Ring)** 효과로 3초 동안 하이라이팅 처리합니다.

## 5. 멀티테넌시 격리 및 계정 탈퇴 정책
- **워크스페이스 격리**: 사용자별 워크스페이스 접근과 생성을 보장하기 위해 워크스페이스 디렉토리 및 Neo4j 노드 소유 식별자에 `ws-username` 및 `username_` 접두사를 강제 적용합니다. 타 사용자의 워크스페이스 명칭 조회를 원천 방어합니다.
- **계정 영구 탈퇴 (DELETE /v1/auth/account)**:
  - 사용자가 계정을 영구 탈퇴할 경우, 다음 3단계가 트랜잭션 및 파괴적 리소스 정리 프로세스로 동기 수행됩니다.
    1. **Neo4j 노드 삭제**: `MATCH (n) WHERE n.workspaceId = 'ws-' + $username OR n.workspaceId STARTS WITH $username + '_' DETACH DELETE n` 수행을 통해 해당 사용자의 모든 노드 및 관계 영구 제거.
    2. **물리적 디렉토리 삭제**: 파일 시스템 상의 사용자 소유 워크스페이스 폴더(`ws-username`, `username_`으로 시작하는 모든 디렉토리)를 물리적으로 재귀 삭제하여 잔여 캐시 및 지식 찌꺼기 완벽 클린업.
    3. **RDBMS 유저 레코드 삭제**: `Users` 테이블에서 유저 데이터를 영구 삭제.

## 6. 기술 스택
- **Back-end Core**: Spring Boot 3.x (Java 21, Virtual Threads 및 ReentrantLock 기반 동시성 제어 적용)
- **Database**: MSSQL (MyBatis 매핑), Neo4j 5 (지식 그래프 온톨로지)
- **Middleware**: Apache Kafka (이벤트 스트림), Redis 7 (RedissonClient 분산 락 및 호스트 포트 6300 매핑)
- **Agent Integration**: LangChain4j (AiServices, Tool Calling)
- **Security**: Spring Security, JWT (Nimbus-JOSE-JWT), AES-256 DB 자격증명 암호화
- **Front-end**: Next.js (Vite/Tailwind CSS, Zustand 상태 관리, React Force Graph 2D 캔버스)
- **SSE Stream**: Server-Sent Events 기반 Notification Stream

## 7. BYOK (Bring Your Own Key) 및 자격증명 보안
- 워크스페이스별 Notion, GitHub, DeepSeek 등 외부 통합 자격증명 토큰은 로컬스토리지에 저장하지 않고, 백엔드 `WorkspaceCredentials` 테이블에 **AES-256 암호화**되어 엄격하게 관리됩니다.
- 에이전트 구동이나 Ingest 수행 시 데이터베이스에서 해당 토큰을 조회하여 즉석에서 복호화해 외부 API 통신에 주입합니다.
- 프론트엔드에서는 자격증명 노출을 원천 방어하며, 단지 저장 여부 상태(`credentialsStatus`)만 리스트업하여 설정 UI에 반영합니다.

## 8. 다중 AI 모델 추론 및 위키 생성 아키텍처
- 사용자는 UI를 통해 `gpt-4o-mini` 또는 `deepseek-v4` 모델을 선택적으로 요청에 실어 보낼 수 있습니다.
- 백엔드 `LlmService`는 수신한 `X-AI-Model` 헤더에 따라 다이렉트 쿼리, 에이전트 순회, 그리고 위키 생성 작업을 처리할 때 각 AI 엔진을 동적으로 전환합니다.
- `deepseek-v4`인 경우, OpenRouter API 주소(`https://openrouter.ai/api/v1/chat/completions`)와 적절한 API 키(DB에 보관된 테넌트별 암호화 키 또는 환경 변수)를 사용하여 AI 추론 엔진 및 completions 응답 포맷을 기동합니다.
