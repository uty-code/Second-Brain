---
id: backend-concurrency-architecture
title: 백엔드 동시성 아키텍처
summary: 2024년 3월 기준 AIMS 백엔드는 Java 17 LTS와 플랫폼 스레드 풀 기반의 동시성 모델을 운영하고 있습니다.
type: architecture
tags: [backend, concurrency, java17]
aliases: [동시성 모델]
provenance: [arch-decision-2024-03-01]
---

## 정의
AIMS 백엔드의 기본 동시성 처리 구조는 OS 플랫폼 스레드 1:1 매핑 기반의 Thread Pool 모델입니다.

## 핵심 구성 요소
- Java 17 LTS 런타임: 안정적인 LTS 버전을 기반으로 실행됩니다.
- ThreadPoolExecutor: 고정된 크기(Core 50, Max 200)의 스레드 풀을 구성하여 블로킹 I/O 작업을 처리합니다.

## 동작 원리
요청이 인입되면 풀에서 유휴 스레드를 할당받아 작업을 수행하며, 풀이 가득 차면 작업 큐에 적재됩니다.

## 장점과 트레이드오프
- OS 레벨에서 검증된 스레드 모델로 안정적입니다.
- I/O 블로킹 시 스레드가 점유되어 메모리 낭비 및 컨텍스트 스위칭 오버헤드가 발생합니다.

## 실전 적용 사례
2024-03-01 기준 모든 REST API 및 DB 트랜잭션 요청 처리에 적용 중입니다.

## 관련 개념들
- [[thread-pool-tuning]]
