---
title: "Phase 14: 지식 추출 안정화"
phase_number: 14
status: "completed"
created_at: 2026-06-30
updated_at: 2026-07-18
---

# Phase 14: 지식 추출 안정화
> **Status: [완료됨]**
> - **히스토리**: 2026-06-30 Structured Outputs 규격 도입 및 테스트 검증 완료.

## 1. 개요 및 목표
- 프롬프트 엔지니어링 기반의 지식 추출에서 발생할 수 있는 데이터 구조의 한계와 비정형 JSON 파싱 에러를 완전히 차단합니다.
- API 레벨의 **Structured Outputs** 규격을 도입하여 LLM 응답 포맷을 100% 안전하게 보장함으로써 전체 Ingestion 파이프라인의 안정성을 향상시킵니다.

## 2. 주요 구현 내용
- **Structured Outputs 연동**:
  - [LlmService.java](file:///c:/second%20brain/aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java) 내의 `extractKnowledge` 및 `callResponsesAPI` 메서드를 LangChain4j의 Structured Outputs 또는 명시적인 JSON Schema 강제 API로 마이그레이션.
  - LLM 호출 스펙에 맞추어 `json_schema` 스키마 주입 또는 LangChain4j `AiServices` 인터페이스 정의.
- **수동 파싱 로직 제거**:
  - 기존의 불안정한 정규식 가공 구문(`replaceAll`, `substring` 등) 제거.
- **안정성 테스트 작성**:
  - [LlmExtractionQualityTest.java](file:///c:/second%20brain/aims-backend/src/test/java/com/aimsgraph/ingest/LlmExtractionQualityTest.java)를 보강하여 동시 요청 및 대용량 텍스트 파싱 시 JSON 깨짐 현상이 없는지 검증.

## 3. 상태
- **완료됨**
