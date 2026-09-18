# 트랜잭셔널 아웃박스 패턴(Transactional Outbox Pattern)

## 1. 문제 상황: 이중 쓰기(Dual Write) 문제
마이크로서비스에서 DB에 비즈니스 데이터를 저장하면서 동시에 Kafka와 같은 메시지 브로커로 이벤트를 전송해야 할 때가 있습니다.
이때 DB 저장은 성공했으나 네트워크 단절로 메시지 발행이 실패하거나, 반대로 메시지는 발행되었으나 DB 롤백이 발생하는 불일치(Dual-Write inconsistency) 문제가 흔히 발생합니다.

## 2. 동작 원리
트랜잭셔널 아웃박스 패턴은 동일한 데이터베이스 트랜잭션 내에서 비즈니스 데이터 테이블과 `Outbox` 테이블에 함께 이벤트를 기록함으로써 원자성을 완벽하게 보장합니다.
1. 비즈니스 로직 실행 시 `INSERT INTO Orders ...`와 `INSERT INTO Outbox (event_id, payload, status='PENDING')`를 단일 로컬 트랜잭션으로 커밋합니다.
2. 백그라운드의 메시지 릴레이(Message Relay) 또는 Polling Publisher 컴포넌트가 Outbox 테이블의 PENDING 레코드를 감지하여 Kafka로 안전하게 발행합니다.
3. 메시지 브로커로부터 수신 ACK를 받으면 Outbox 상태를 `PUBLISHED`로 변경하거나 해당 레코드를 삭제합니다.

## 3. 핵심 이점
- 분산 트랜잭션(2PC) 없이도 최소 1회(At-Least-Once) 메시지 발행 보장.
- 메시지 브로커가 일시 다운되더라도 비즈니스 요청은 로컬 DB에 안전하게 커밋됨.
