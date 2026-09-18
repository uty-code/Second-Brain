---
id: distributed-transaction-strategy
title: 분산 트랜잭션 처리 전략
summary: 분산 데이터베이스 환경에서 데이터 원자성을 완벽히 보장하기 위해 2PC(Two-Phase Commit) 프로토콜을 중심으로 설계합니다.
type: architecture
tags: [transaction, 2pc, consistency]
aliases: [분산 트랜잭션 전략]
provenance: [fintech-core-design]
---

## 정의
분산 트랜잭션 처리 전략은 여러 데이터베이스 노드에 걸친 변경 사항을 하나의 원자적 트랜잭션으로 커밋하기 위한 아키텍처 가이드입니다.

## 핵심 구성 요소
- 2PC 코디네이터(Coordinator): 모든 참여자(Participant) 노드에 준비(Prepare) 및 커밋(Commit) 명령을 전송하고 결과를 수합합니다.
- 강한 일관성(Strong Consistency): 모든 노드가 동시에 커밋되거나 롤백되므로 즉각적인 ACID 정합성을 보장합니다.

## 동작 원리
1단계(Prepare)에서 모든 참여자가 쓰기 준비 완료 응답을 보내면, 2단계(Commit)에서 최종 영구 반영을 지시합니다.

## 장점과 트레이드오프
- 금융 결제와 같이 단 1원의 오차도 허용되지 않는 도메인에서 완벽한 정합성을 제공합니다.
- 코디네이터 장애 시 블로킹이 발생하며 네트워크 지연에 취약한 트레이드오프가 있습니다.

## 실전 적용 사례
은행 계좌 이체 및 핵심 원장 데이터베이스 간의 동기화에 필수적으로 적용됩니다.

## 관련 개념들
- [[database-acid]]
