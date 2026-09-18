---
id: apache-kafka-architecture
title: Apache Kafka 아키텍처
summary: Apache Kafka는 분산 커밋 로그 기반의 대규모 이벤트 스트리밍 플랫폼입니다.
type: concept
tags: [kafka, streaming, distributed-systems]
aliases: [카프카 아키텍처]
provenance: [source-2024]
---

## 정의
Apache Kafka는 분산 커밋 로그(distributed commit log) 구조를 핵심으로 하는 분산 이벤트 스트리밍 플랫폼입니다.

## 핵심 구성 요소
- 브로커(Broker): 메시지를 디스크에 영구 저장하고 클라이언트의 읽기/쓰기 요청을 중계하는 카프카 서버 노드입니다.
- 파티션(Partition): 토픽을 물리적으로 분할한 순서가 보장되는 불변의 추가 전용(append-only) 로그 단위입니다.

## 동작 원리
프로듀서가 발행한 이벤트는 해시 키에 따라 특정 파티션의 브로커로 전송되며, 순차적으로 로그 끝에 기록됩니다.

## 장점과 트레이드오프
- 높은 처리량과 수평 확장성을 제공합니다.
- 순서 보장은 파티션 단위로만 제한되는 트레이드오프가 존재합니다.

## 실전 적용 사례
대규모 결제 이벤트 파이프라인 및 실시간 로그 수집에 표준으로 사용됩니다.

## 관련 개념들
- [[event-driven-architecture]]
- [[transactional-outbox-pattern]]
