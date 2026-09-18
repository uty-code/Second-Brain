---
id: backend-concurrency-architecture
title: 백엔드 동시성 아키텍처
summary: 2026년 6월 기준 AIMS 백엔드는 Java 21로 전면 업그레이드하고 Virtual Threads를 채택하여 기존 스레드 풀을 완전히 제거했습니다.
type: architecture
tags: [backend, concurrency, java21, virtual-threads]
aliases: [동시성 모델, 가상 스레드 아키텍처]
provenance: [arch-decision-2026-06-01]
---

## 정의
AIMS 백엔드는 2026년 6월부로 Java 21 가상 스레드(Virtual Threads, Project Loom) 기반의 경량 스레드 동시성 모델로 전면 마이그레이션 완료되었습니다.

## 핵심 구성 요소
- Java 21 런타임: 최신 LTS 런타임을 기반으로 동작합니다.
- Virtual Threads: 요청당 하나의 가상 스레드를 할당하여(Thread-per-request) 수백만 개의 동시 I/O를 처리합니다.
- 기존 스레드 풀 제거: 고정된 크기의 ThreadPoolExecutor 및 관련 튜닝 설정은 완전히 폐기되었습니다.

## 동작 원리
블로킹 I/O 호출 발생 시 캐리어 스레드는 언마운트(unmount)되어 다른 가상 스레드를 실행하므로, 스레드 낭비가 전혀 없습니다.

## 장점과 트레이드오프
- 하드웨어 리소스 효율을 극대화하고 처리량을 대폭 향상시킵니다.
- synchronized 블록 사용 시 캐리어 스레드가 피닝(pinning)될 수 있어 ReentrantLock으로 대체해야 하는 주의점이 있습니다.

## 실전 적용 사례
2026-06-01부터 프로덕션 전체 서버에 Virtual Threads 모델이 기본 적용되었습니다.

## 관련 개념들
- [[virtual-threads]]
