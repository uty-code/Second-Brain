# Phase 3 지시서: Ingestion Worker 구현
> **Status: [완료됨]**
> - **히스토리**: Kafka 이벤트 수신 및 LLM 에이전트 지식 추출, Redis 분산 락 기반 마크다운 위키 파일 시스템 및 Neo4j 동시 갱신 구현 완료.

## 1. 개요 및 목표
- Kafka 이벤트를 수신하여 동작하는 Worker 로직(Agentic Core 레이어)을 개발합니다.
- 수집된 원본 문서를 바탕으로 위키(Markdown)를 파일 시스템에 생성/갱신하고, Neo4j에 노드/관계를 증분 업데이트합니다.
- Redis 분산 락을 통해 `index.md`와 `log.md` 동시성 문제를 제어합니다.

## 2. 참조 문서 (반드시 읽을 것)
- `GEMINI.md` 및 `rules/common/project-rules.md` (TDD Guard 준수)
- `docs/ADR.md` (ADR-002: Ingest 시 Index & Log 파일 관리와 증분 업데이트)
- `docs/WIKI_SCHEMA.md` (위키 파일 생성 및 카탈로그 갱신 규칙)

## 3. 구현 내용 목록
1. **Kafka Consumer 구현 (`com.aimsgraph.ingest` 패키지)**
   - `aims.outbox.events` 토픽에서 이벤트를 수신하는 Listener 작성.
   - 테넌트(`workspaceId`)별 API Key 복호화 및 에이전트 지식 추출 로직(Mock LLM Call) 작성.
2. **분산 락을 활용한 위키 파일 갱신**
   - Redisson을 사용해 `wiki:index.md`, `wiki:log.md` 락을 획득하는 로직 작성.
   - 로컬 파일 시스템(`wiki/...`)에 위키 파일 생성/갱신 처리 (`WIKI_SCHEMA.md` 준수).
3. **Neo4j 증분 갱신 로직**
   - 수집된 Concept/Entity 노드를 `Neo4jTemplate` 혹은 Repository로 머지(MERGE)하는 로직 개발.

## 4. 제약 사항
- 반드시 테스트 코드 작성 후 기능 구현 (TDD 방식). 
- (※ 주의: 현재 Docker 데스크톱 미구동으로 Testcontainers 기동 에러가 발생할 수 있습니다. 로직상 테스트 코드는 반드시 작성하되, Connection Refused 에러로 인한 Circuit Breaker Hook 실패 처리는 무시하고 구현을 완료하세요.)
- 작업 완료 후 `c:\second brain\tasks.md` 파일에서 Phase 3를 `[x]`로 갱신하고 보고하세요.
