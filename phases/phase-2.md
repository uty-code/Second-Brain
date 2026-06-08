# Phase 2 지시서: 데이터 정합성 파이프라인 (Transactional Outbox)
> **Status: [완료됨]**
> - **히스토리**: Entity/Neo4j 스키마 설계, Transactional Outbox 패턴을 적용한 MSSQL -> Kafka 이벤트 스트리밍 및 발행 보장 파이프라인 구축 완료.

## 1. 개요 및 목표
- 원본 메타데이터(MSSQL)와 그래프 구조(Neo4j) 동기화를 위한 **Transactional Outbox 패턴**을 구축합니다.
- Entity 정의 및 Kafka 연동 Producer 뼈대를 만듭니다.

## 2. 참조 문서 (반드시 읽을 것)
- `GEMINI.md` 및 `rules/common/project-rules.md` (TDD Guard, 파괴적 명령 금지 등)
- `docs/SCHEMA.md` (MSSQL `RawSource`, `WikiPage`, `OutboxEvent` 스키마 명세 확인)
- `docs/ARCHITECTURE.md` (Outbox -> Kafka 흐름 확인)

## 3. 구현 내용 목록 (Task Breakdown)
1. **엔티티 및 테이블 정의**
   - `docs/SCHEMA.md`를 바탕으로 `RawSource`, `WikiPage`, `OutboxEvent` 엔티티 클래스 생성.
   - H2 대신 MSSQL Testcontainers를 이용해 Flyway/schema.sql 등으로 테이블 생성 쿼리(또는 Mybatis 매퍼 설정) 작성.
2. **MyBatis Mapper 및 저장 로직**
   - 비즈니스 로직(예: 새 문서 수집 시)에서 `RawSource` 데이터를 저장함과 동시에, **동일 트랜잭션 내에서** `OutboxEvent` 테이블에도 이벤트를 기록하는 로직을 TDD로 작성.
3. **Kafka 연동 (Outbox Poller)**
   - 스케줄러(`@Scheduled` 등)를 통해 `OutboxEvent` 테이블의 `PENDING` 상태 레코드를 주기적으로 조회.
   - 조회된 이벤트를 Kafka 토픽(`aims.outbox.events`)에 발행.
   - 발행 완료 후 `PROCESSED` 상태로 변경하는 기능 개발 (TDD 준수).

## 4. 제약 사항
- 반드시 테스트 코드(`src/test/...`)를 먼저 작성하여(Red) 의도된 실패를 확인한 후 구현(Green)할 것.
- 인메모리 DB(H2 등) 사용 불가. Testcontainers(mssql, kafka 등)를 사용하여 테스트 환경을 격리 구성할 것.
- 작업 완료 후 `c:\second brain\tasks.md` 파일에서 Phase 2를 `[x]`로 갱신하고, 사용자(메인 에이전트)에게 보고할 것.
