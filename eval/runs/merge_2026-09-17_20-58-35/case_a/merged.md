---
id: apache-kafka-architecture
title: Apache Kafka 아키텍처
summary: Apache Kafka는 분산 커밋 로그 기반의 고성능 이벤트 스트리밍 플랫폼입니다.
type: concept
tags: [kafka, streaming, distributed-systems, event-broker, partition]
aliases: [카프카 아키텍처, 카프카 시스템 아키텍처]
provenance: [source-2024, source-2025-notes]
---

## 정의
Apache Kafka는 분산 커밋 로그(distributed commit log) 구조를 핵심으로 하는 고성능 메시징 인프라입니다. 카프카 클러스터는 다수의 브로커 서버와 세부 파티션 로그로 구성되어 동작합니다.

## 핵심 구성 요소
- **브로커(Broker)**: 클라이언트의 레코드 송수신을 담당하며, 메시지를 디스크에 영구 저장하고 클라이언트의 읽기/쓰기 요청을 중계하는 카프카 서버 노드입니다.
- **파티션(Partition)**: 각 토픽은 여러 개의 파티션으로 쪼개져 서로 다른 브로커들에 고르게 분산 배치됩니다. 파티션은 순서가 보장되는 불변의 추가 전용(append-only) 로그 단위입니다.

## 동작 원리
프로듀서가 발행한 이벤트는 해시 키에 따라 특정 파티션의 브로커로 전송되며, 순차적으로 로그 끝에 기록됩니다. 카프카는 추가 전용 분산 로그 파일 시스템에 레코드를 기록하므로 데이터 유실 없는 안정적인 스트리밍을 구현합니다.

## 장점과 트레이드오프
- 높은 처리량과 수평 확장성을 제공합니다. 클러스터에 브로커를 추가함으로써 선형적인 수평 확장이 가능합니다.
- 순서 보장은 파티션 단위로만 제한되며, 토픽 파티션 개수 변경 시 리밸런싱 비용이 발생하는 트레이드오프가 존재합니다.

## 실전 적용 사례
대규모 결제 이벤트 파이프라인 및 실시간 로그 수집, 실시간 결제 시스템, 분산 이벤트 아키텍처의 중심 허브로 활용됩니다.

## 관련 개념들
- [[event-driven-architecture]]
- [[transactional-outbox-pattern]]