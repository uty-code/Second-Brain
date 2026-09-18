# Second Brain Pipeline Evaluation Report

- **Run ID:** `live_2026-09-17_20-57-35`
- **Evaluated At:** 2026-09-17T20:58:35.929047800
- **Total Controlled Scenarios:** 5

## 1. Executive Summary

| Metric | Count / Value | Note |
|---|---|---|
| **Total Scenarios** | 5 | 7개 다채로운 입력 카테고리 |
| **Baseline Concepts Matched** | 2 concepts | 기준선 개념 일치 건수 |
| **Hard Failure Scenarios** | 0 | 금지 개념/사칭 날조 발생 건수 |
| **Validator Initial Pass** | 5 | 1차 시도 즉시 통과 |
| **Validator Sanitized Pass** | 0 | 자기참조 등 안전 정제 통과 |
| **Validator Retry Pass** | 0 | 피드백 재시도 후 통과 |
| **Validator Final Reject** | 0 | 3회 실패 영속화 차단 |

## 2. Category Diagnostics & Retry Indices

| Category | Scenario Count | Avg Retry Count | Status |
|---|---|---|---|
| `extremely_short` | 1 | 0.00 | ✅ 안정적 |
| `code_heavy` | 1 | 0.00 | ✅ 안정적 |
| `deep_arch` | 1 | 0.00 | ✅ 안정적 |
| `short_blog` | 1 | 0.00 | ✅ 안정적 |
| `buzzword_dense` | 1 | 0.00 | ✅ 안정적 |

## 3. Failure Cases & Violation Breakdown

> **검증 결과:** 통제된 25개 테스트 시나리오 전체에서 명백한 사칭, 가짜 벤치마크, 금지 개념 추출 등의 Hard Failure가 발생하지 않았습니다.

## 4. Manual Review Required Candidates

- **Scenario:** `01_short_blog_docker` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `07_arch_transactional_outbox` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `07_arch_transactional_outbox` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `07_arch_transactional_outbox` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `07_arch_transactional_outbox` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `21_sparse_meeting_memo` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `21_sparse_meeting_memo` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `21_sparse_meeting_memo` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `21_sparse_meeting_memo` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `21_sparse_meeting_memo` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `21_sparse_meeting_memo` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `21_sparse_meeting_memo` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `21_sparse_meeting_memo` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
