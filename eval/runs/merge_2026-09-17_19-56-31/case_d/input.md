---
id: distributed-transaction-strategy
title: 분산 트랜잭션 처리 전략
summary: 대규모 마이크로서비스 환경에서는 가용성과 장애 격리를 극대화하기 위해 Saga 패턴 기반의 최종 일관성을 권장합니다.
type: architecture
tags: [transaction, saga-pattern, eventual-consistency]
aliases: [분산 트랜잭션 패턴]
provenance: [msa-arch-guild-2025]
---

## 정의
현대적 분산 시스템에서는 서비스 간 결합도를 낮추고 고가용성을 확보하기 위해 이벤트 기반 비동기 트랜잭션 분할 기법을 적용합니다.

## 핵심 구성 요소
- Saga 패턴: 전체 비즈니스 프로세스를 각 서비스의 로컬 트랜잭션 연속으로 분할하여 실행합니다.
- 보상 트랜잭션(Compensating Transaction): 중간 단계 실패 시 앞서 완료된 로컬 트랜잭션들을 비즈니스적으로 되돌립니다.
- 최종 일관성(Eventual Consistency): 일시적 불일치를 허용하되 비동기 이벤트 전달을 통해 궁극적인 정합성을 달성합니다.

## 동작 원리
주문 서비스가 로컬 커밋 후 이벤트를 발행하면, 결제 및 재고 서비스가 이를 구독하여 순차적으로 자체 커밋을 수행합니다.

## 장점과 트레이드오프
- 2PC의 전역 락(Global Lock)과 블로킹을 배제하여 고가용성과 높은 처리량을 달성합니다.
- 데이터 정합성이 즉각적이지 않고 보상 로직 설계 복잡도가 증가하는 트레이드오프가 있습니다.

## 실전 적용 사례
글로벌 이커머스 장바구니/주문 서비스 및 대용량 트래픽 인입 구간에 적용됩니다.

## 관련 개념들
- [[event-driven-architecture]]
