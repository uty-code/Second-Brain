# AIMS-Graph 백엔드 개발 및 설계 규칙 (backend-rules.md)

이 파일은 AIMS-Graph 백엔드(Spring Boot) 개발 및 리팩토링 시 에이전트가 준수해야 할 핵심 아키텍처 제약 사항과 코딩 표준을 정의합니다.

---

## 1. 가상 스레드(Virtual Threads) 친화적 동기화
- **절대 원칙**: Java 21의 가상 스레드가 동작하는 환경이므로, 파일 I/O나 DB 커넥션 등 **블로킹 I/O 작업이 수반되는 영역에는 절대 전통적인 `synchronized` 키워드를 사용하지 않는다.**
- **락(Lock) 메커니즘**: 캐리어 스레드 피닝(Carrier Thread Pinning) 현상을 원천 차단하기 위해, 동기화가 필요한 공유 자원 접근부에는 가상 스레드 친화적인 **`ReentrantLock`** 또는 분산 환경인 경우 **`RedissonClient` 분산 락**을 채택하여 구현한다.

## 2. 데이터베이스 연동 및 트랜잭션 규칙
- **Transactional Outbox 패턴**: DB 상태 변경 시, 외부 비동기 이벤트 스트림(Kafka) 발행과의 원자성을 보장하기 위해 반드시 **트랜잭셔널 아웃박스 패턴**을 이용해야 한다. 서비스 레이어 내부에서 직접 이벤트를 발행하지 말고, 동일 트랜잭션 내에서 `OutboxEvent` 테이블에 PENDING 레코드를 삽입하는 방식으로 구현한다.
- **MyBatis & MSSQL**: RDBMS 원본 메타데이터 및 아웃박스 이벤트 관리는 MyBatis 매퍼를 정교하게 정의하여 처리한다.
- **Neo4j 5 온톨로지**: 지식 그래프 온톨로지는 [SCHEMA.md](file:///c:/second%20brain/docs/SCHEMA.md)에 기술된 스키마 스펙을 반드시 엄수하여 노드와 관계를 지식 위키 마크다운 파일과 일치하게 동기화해야 한다.

## 3. 테스트 의존성 제약
- **인메모리 DB 금지**: 테스트를 빠르게 실행하겠다는 이유로 H2 등 인메모리 DB 환경으로 테스트하는 것을 전면 금지한다.
- **Testcontainers 기반 통합 테스트**: 데이터베이스 및 메시지 브로커 의존성을 검증할 때는 반드시 Testcontainers 환경을 상속하여 구동한다. 모든 통합 테스트는 컨테이너 구동 오버헤드를 아끼기 위해 **[AbstractContainerBaseTest.java](file:///c:/second%20brain/aims-backend/src/test/java/com/aimsgraph/testcontainers/AbstractContainerBaseTest.java)** 클래스를 상속받아 싱글톤 컨테이너를 공유하도록 구현해야 한다.

## 4. B2B 멀티테넌시 및 자격증명 보안
- **테넌트 물리/논리 격리**: 사용자별 워크스페이스 디렉토리 및 Neo4j 그래프 내 `workspaceId` 소유권 식별자에는 반드시 `ws-username` 및 `username_` 접두사를 엄격히 강제한다.
- **자격증명 암호화**: Notion, GitHub 등의 외부 통합 자격증명 API Key는 프론트엔드 또는 미가공 상태로 DB에 저장하지 않고, 백엔드 `WorkspaceCredentials` 테이블에 **AES-256 알고리즘**으로 암호화하여 저장하며, 서비스 구동 시에만 복호화하여 사용하도록 설계한다.
