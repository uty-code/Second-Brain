# Second Brain Pipeline Evaluation Report

- **Run ID:** `2026-09-17_19-19-01`
- **Evaluated At:** 2026-09-17T19:19:01.714245400
- **Total Controlled Scenarios:** 25

## 1. Executive Summary

| Metric | Count / Value | Note |
|---|---|---|
| **Total Scenarios** | 25 | 7개 다채로운 입력 카테고리 |
| **Baseline Concepts Matched** | 61 concepts | 기준선 개념 일치 건수 |
| **Hard Failure Scenarios** | 0 | 금지 개념/사칭 날조 발생 건수 |
| **Validator Initial Pass** | 25 | 1차 시도 즉시 통과 |
| **Validator Sanitized Pass** | 0 | 자기참조 등 안전 정제 통과 |
| **Validator Retry Pass** | 0 | 피드백 재시도 후 통과 |
| **Validator Final Reject** | 0 | 3회 실패 영속화 차단 |

## 2. Category Diagnostics & Retry Indices

| Category | Scenario Count | Avg Retry Count | Status |
|---|---|---|---|
| `extremely_short` | 3 | 0.00 | ✅ 안정적 |
| `code_heavy` | 4 | 0.00 | ✅ 안정적 |
| `deep_arch` | 4 | 0.00 | ✅ 안정적 |
| `mixed_technologies` | 3 | 0.00 | ✅ 안정적 |
| `tool_intro` | 4 | 0.00 | ✅ 안정적 |
| `short_blog` | 4 | 0.00 | ✅ 안정적 |
| `buzzword_dense` | 3 | 0.00 | ✅ 안정적 |

## 3. Failure Cases & Violation Breakdown

> **검증 결과:** 통제된 25개 테스트 시나리오 전체에서 명백한 사칭, 가짜 벤치마크, 금지 개념 추출 등의 Hard Failure가 발생하지 않았습니다.

## 4. Manual Review Required Candidates

- **Scenario:** `01_short_blog_docker` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `01_short_blog_docker` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `02_short_blog_http2` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `02_short_blog_http2` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `03_short_blog_jwt` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `03_short_blog_jwt` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `04_short_blog_connection_pool` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `04_short_blog_connection_pool` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `05_arch_event_driven` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `05_arch_event_driven` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `05_arch_event_driven` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `05_arch_event_driven` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `06_arch_cqrs_saga` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `06_arch_cqrs_saga` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `06_arch_cqrs_saga` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `07_arch_transactional_outbox` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `07_arch_transactional_outbox` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `07_arch_transactional_outbox` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `08_arch_zero_trust_security` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `08_arch_zero_trust_security` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `08_arch_zero_trust_security` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `09_code_spring_security_filter` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `10_code_kafka_consumer_batch` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `10_code_kafka_consumer_batch` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `10_code_kafka_consumer_batch` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `11_code_mybatis_dynamic_sql` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `11_code_mybatis_dynamic_sql` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `12_code_redis_distributed_lock` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `12_code_redis_distributed_lock` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `12_code_redis_distributed_lock` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `13_tool_redis_streams` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `13_tool_redis_streams` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `14_tool_envoy_proxy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `14_tool_envoy_proxy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `14_tool_envoy_proxy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `15_tool_neo4j_graph_db` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `15_tool_neo4j_graph_db` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `15_tool_neo4j_graph_db` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `16_tool_apache_zookeeper` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `16_tool_apache_zookeeper` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `16_tool_apache_zookeeper` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `17_mixed_react_postgres_k8s` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `17_mixed_react_postgres_k8s` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `17_mixed_react_postgres_k8s` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `18_mixed_grpc_rest_graphql` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `18_mixed_grpc_rest_graphql` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `18_mixed_grpc_rest_graphql` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `19_mixed_elasticsearch_redis_kafka` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `19_mixed_elasticsearch_redis_kafka` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `19_mixed_elasticsearch_redis_kafka` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `20_sparse_single_sentence` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `21_sparse_meeting_memo` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `22_sparse_slack_snippet` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `23_buzzword_agile_cloud_synergy` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `24_buzzword_ai_driven_transformation` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `24_buzzword_ai_driven_transformation` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `25_buzzword_blockchain_web3_metaverse` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
- **Scenario:** `25_buzzword_blockchain_web3_metaverse` | **Type:** `KEY_FACT_ABSENCE_REVIEW`
  - Context: Wiki page does not clearly mention any of the expected key facts from the source.
