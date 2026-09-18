## [2026-09-17] Wiki 문서 고정 섹션 구조 제거 & 범용 자율 구조 리팩터링 [완료됨]
- **Goal:** 소프트웨어/IT 기술 문서에 편향되어 있던 5개 고정 섹션(`## 정의`, `## 핵심 구성 요소`, `## 동작 원리`, `## 장점과 트레이드오프`, `## 실전 적용 사례`) 강제를 제거하고, 수학·과학·역사·인문학·제품·인물 등 전 분야의 개념을 자연스럽게 처리할 수 있는 범용 Wiki 생성/검증 시스템으로 전환.
- **Key Implementation Details:**
  1. **Validator 책임 전환 (콘텐츠 구조 강제 ❌ -> 문서 무결성 검증 ⭕)**:
     - `ErrorType`에서 `MISSING_SECTION`, `SECTION_TOO_SHORT` 영구 삭제.
     - `BODY_EMPTY` 추가: Markdown 헤더/서식 기호 제외 후 실질적 텍스트가 전혀 없는 경우에만 Hard Fail 처리 (숫자 기반 길이 제한 배제).
     - ID, Summary, Frontmatter, Placeholder, Cross-reference(`[[slug]]`), Self-reference 정제, Unknown reference 차단 등 핵심 무결성 검증은 100% 엄격 유지.
  2. **LLM 생성 프롬프트(`generateWikiPages`) 범용화**:
     - 고정 5섹션 강제 문구 제거 -> 개념 본질에 맞게 최소 2개 이상의 `##` 소제목으로 자율 구성하도록 가이드.
     - IT 특화 일변도의 Few-shot을 다분야 유연 구조 예시로 교체.
  3. **LLM 병합 엔진(`mergeWikiContent`) 및 카탈로그(`index.md`) 개편**:
     - Merge 프롬프트에서 고정 5섹션 강제 제거 및 기존 문서의 자율적 섹션 흐름 보존 규칙 적용.
     - `index.md` 요약 추출 시 `## 정의` 하드코딩 의존을 제거하고 Frontmatter `summary:` 우선 참조로 변경.
  4. **테스트 스위트 전면 개편 & 100% 통과 검증**:
     - `WikiPageValidatorTest`: 단일 섹션 문서, 헤더 없는 문서, 수학/과학 등 다분야 구조 통과 및 빈 본문 차단 테스트 추가.
     - `WikiGenerationPipelineTest`, `HallucinationInspectorTest`, `KnowledgePipelineEvaluationTest`: 범용 자유 구조로 마이그레이션.
- **Modified Files:**
  - `aims-backend/src/main/java/com/aimsgraph/ingest/validator/ErrorType.java`
  - `aims-backend/src/main/java/com/aimsgraph/ingest/validator/WikiPageValidator.java`
  - `aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java`
  - `aims-backend/src/main/resources/few-shot-wiki-examples.json`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/validator/WikiPageValidatorTest.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/WikiGenerationPipelineTest.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/HallucinationInspectorTest.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/KnowledgePipelineEvaluationTest.java`
  - `docs/WIKI_SCHEMA.md`
  - `docs/updates.md`

## [2026-09-17] Knowledge Graph 관계 생성 정밀도(Edge Precision) 고도화 & False Positive 차단 [완료됨]
- **Goal:** 관계가 약하거나 전무한 개념 사이에 엣지가 무분별하게 생성되던 False Positive / Hairball(스파게티 그래프) 문제를 해결하고, 'Graph Density' 대신 **'Edge Precision'을 극대화**하는 정밀 관계 추출 및 다계층 방어 파이프라인 구축.
- **Key Principles & Implementation:**
  1. **`UnifiedGraphExtractor` 프롬프트 정밀화 (Rule 9)**:
     - 과도한 연결 강제 문구(`"For every node, connect it..."`, `"NEVER return empty links"`) 완전 제거.
     - 5대 명확한 기술적 관계(Dependency, Composition, Implementation, Direct Interaction, Explicit Reference)만 허용하고 단순 도메인 중복, 동시 출현, 잠재적 조합은 링크 생성 금지.
     - 고립 노드(Isolated Node, `links: []`)를 정상적이고 기대되는 결과로 공식 수용.
  2. **단순 문자열 매칭(`contains()`) 100% 원천 제거**:
     - `syncWikiLinksToNeo4j`에서 본문 단어 부분 일치로 엣지를 자동 생성하던 Fallback 로직을 영구 삭제하여 텍스트 단순 언급에 따른 가짜 연결 차단.
  3. **3대 추가 방어 가드레일 확립**:
     - **[추가 1] 엄격한 `allConceptIds` 수집**: 현재 워크스페이스의 실제 유효 Concept ID(`^[a-z0-9]+(?:-[a-z0-9]+)*$` 규격)만 정확히 수집.
     - **[추가 2] UNKNOWN_REFERENCE 이중 방어선**: 위키 계층(Validator Hard Error)과 그래프 영속 계층(Phantom Node 생성 원천 차단 및 WARN 로깅)으로 DB 오염 방어.
     - **[추가 3] 단일 `[:REFERENCES]` 관계 및 Provenance(출처) 합성**: 동일한 `source -> target` 관계가 `LLM_INFERRED`와 `EXPLICIT_WIKI_LINK` 양쪽에서 발생해도 중복 엣지 생성을 차단하고 `r.sources` 배열로 출처를 누적 관리.
  4. **False Positive 회귀 평가 Fixture (5대 케이스) 및 자동화 테스트 구축**:
     - `eval/corpus/graph/` (`case_01_strong_relation`, `case_02_unrelated_domains`, `case_03_text_only_mention`, `case_04_explicit_wiki_link`, `case_05_isolated_nodes`)
     - `GraphEdgePrecisionTest.java` (7개 시나리오 100% 통과).
- **New Files:**
  - `eval/corpus/graph/case_01_strong_relation/expected.json`
  - `eval/corpus/graph/case_02_unrelated_domains/expected.json`
  - `eval/corpus/graph/case_03_text_only_mention/expected.json`
  - `eval/corpus/graph/case_04_explicit_wiki_link/expected.json`
  - `eval/corpus/graph/case_05_isolated_nodes/expected.json`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/GraphEdgePrecisionTest.java`
- **Modified Files:**
  - `aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/LlmServiceTest.java`

## [2026-09-17] 지식 그래프 노드 간 관계(Edge/Link) 미연결 버그 수정 & 위키 크로스 레퍼런스 자동 동기화 [완료됨]
- **Goal:** 웹 UI에서 텍스트 업로드 후 지식 그래프 생성 시 노드(Concept)들은 정상 추출되나 노드 간의 선(Edge/Link)이 연결되지 않던 문제를 해결하고, Karpathy LLM Wiki 패턴의 핵심인 크로스 레퍼런스(`[[slug]]`) 자동 연결 및 자가 치유(Self-Healing) 파이프라인 구축.
- **Root Cause Analysis:**
  1. **지식 그래프 추출 프롬프트 관계 규칙 부재**: `UnifiedGraphExtractor` 프롬프트에 노드(Concept) 추출 규칙만 존재하고 관계(`links`) 추출 규칙이 명시되지 않아 LLM이 항상 `links: []`를 반환함.
  2. **위키 마크다운 상호 링크 미강제**: 위키 본문 생성 시 다른 개념들을 `[[concept-slug]]` 형태로 링크하도록 프롬프트에서 강제하지 않아 본문 내 상호 연결이 텍스트로만 남아있었음.
  3. **위키 마크다운-Neo4j 동기화 누락**: 생성된 위키 파일들의 `[[slug]]` 링크를 Neo4j 그래프의 엣지(`(c)-[:REFERENCES]->(t)`)로 동기화하는 영속 계층 로직이 없었음.
- **Key Fixes & Implementation:**
  1. **관계(Link) 추출 프롬프트 규칙 보강 (`UnifiedGraphExtractor`)**:
     - `9) RELATIONSHIP & LINK RULES`: 유의미한 관계(`USES`, `SOLVES`, `INTEGRATES_WITH`, `MANAGES` 등)를 반드시 추출하도록 강제.
  2. **위키 마크다운 본문 상호 연결 프롬프트 보강 (`generateWikiPages`)**:
     - 원문 및 다른 추출 개념을 인용할 때 반드시 `[[english-slug]]` 위키 링크를 본문과 연관 개념 섹션에 삽입하도록 규칙 명시.
  3. **Neo4j 크로스 레퍼런스 엣지 동기화 엔진 (`syncWikiLinksToNeo4j`)**:
     - 위키 디렉토리의 모든 마크다운을 검사하여 `[[slug]]` 패턴 및 본문 내 타 개념 언급을 분석하고 Neo4j에 `[:REFERENCES]` 엣지로 일괄 동기화.
  4. **그래프 API 자가 치유(Self-Healing) 로직 (`WorkspaceController.getWorkspaceGraph`)**:
     - 그래프 조회 시 노드는 존재하나 링크가 비어있을 경우, 자동으로 `syncWikiLinksToNeo4j`를 호출하여 위키 마크다운 기반으로 관계를 즉각 복원/반환.
  5. **기존 워크스페이스(`sewo1218_test`) 복원 완료**:
     - 기존 5개 위키 문서에 크로스 레퍼런스 링크 반영 및 Neo4j 동기화 완료 (12개 엣지 정상 생성 확인).
- **Modified Files:**
  - `aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java`
  - `aims-backend/src/main/java/com/aimsgraph/api/WorkspaceController.java`
  - `aims-backend/src/test/java/com/aimsgraph/api/WorkspaceControllerTest.java`
  - `aims-backend/workspaces/sewo1218_test/wiki/concepts/*.md`

## [2026-09-17] 지능형 병합 (Merge & Refine) 5대 난제 정량 회귀 평가 & 실전 라이브 검증 (3단계) [완료됨]
- **Goal:** 지식 컴파일러의 핵심 생명선인 Intelligent Merge & Refine 엔진의 5대 핵심 실패 모드(중복 나열, 신규 정보 손실, 시간축 왜곡, 관점 편향 삭제, 10회 연속 문서 폭발)를 통제 코퍼스 및 정량 계약(`expected.json`, `evaluation.json`)을 기반으로 실시간 OpenAI API(`gpt-4o-mini`) 라이브 검증 수행.
- **Key Principles & Implementation:**
  1. **5대 검증 시나리오 및 정량 계약 체계 (`eval/corpus/merge/`)**:
     - **Case A (Deduplication & Synthesis - Kafka)**: 단순 문장 반복(duplicateRatio=0.0 <= 0.15 threshold) 차단, 문서 팽창률 1.43배 통제, 핵심 지식 보존.
     - **Case B (Incremental Accumulation - HTTP/2)**: 5대 핵심 사실(멀티플렉싱, HPACK, 서버 푸시, 우선순위, HOL) 누락 없이 100% 보존 (`retentionRate=1.0`, `informationLoss=0`).
     - **Case C (Temporal Evolution & Correctness - Java 17 -> 21)**: Java 21 및 가상 스레드를 현재 운영 상태로 정상 반영하고, 이전 스레드 풀 구조를 역사적 배경으로 온전히 보존하며, 과거 상태가 현재인 양 기술된 왜곡 구문 0건 확인.
     - **Case D (Divergent Perspectives & Trade-offs - 2PC vs Saga)**: 2PC vs Saga를 참/거짓 모순이 아닌 설계 트레이드오프로 정의하고, 강한 일관성(ACID) 관점과 가용성/장애 격리(High Availability) 관점을 자의적 삭제 없이 모두 공존/병기.
     - **Case E (Bloat Prevention - Redis Lock 10회 연속 병합)**: 10단계 연속 점진 병합 시 단순 Append 폭발(4,000~8,000자)을 억제하고 이상적인 압축 곡선(`[837자 -> 981자 -> 1104자 -> ... -> 2414자]`)을 유지하며 2,500자 상한선 이내로 통제.
  2. **Merge 엔진의 가드레일 체계 (`WikiPageValidator` 연결)**:
     - `Merge LLM 호출 -> 마크다운 파싱 -> WikiPageValidator 검증 -> 피드백 재시도 -> 통과/보류`
     - **최대 3회 시도 (총 3 calls: 최초 1회 + 재시도 2회)** 정책 엄수.
     - 실제로 Case C, Case D 등에서 Summary 길이(30~80자) 초과 시 재시도 피드백 루프가 실시간으로 동작하여 2회차에 자동 교정 후 통과.
     - **안전 Fallback (격리 원칙)**: 3회 시도 모두 실패 시 단순 덧붙이기(Append)를 금지하고, 기존 문서를 100% 보존(`return existingContent`)하여 지식 오염 차단.
  3. **정량적 판정 지표 및 `evaluation.json` 영구 아카이빙**:
     - `MergeQualityInspector`를 통해 정량 지표 산출.
     - `eval/runs/merge_{timestamp}/` 하위에 모든 원본/병합 문서와 함께 `evaluation.json`, `merge_eval_report.md` 영구 저장.
     - `eval/reports/merge_report_latest.md` 최신화.
- **New Files:**
  - `eval/corpus/merge/case_a_dedup/` (existing.md, input.md, expected.json)
  - `eval/corpus/merge/case_b_accumulation/` (existing.md, input.md, expected.json)
  - `eval/corpus/merge/case_c_temporal/` (existing.md, input.md, expected.json)
  - `eval/corpus/merge/case_d_perspective/` (existing.md, input.md, expected.json)
  - `eval/corpus/merge/case_e_bloat/` (existing.md, inputs/step_01~10.md, expected.json)
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/MergeExpectedMetadata.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/MergeEvaluationResult.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/MergeQualityInspector.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/WikiMergeQualityEvaluationTest.java`
  - `eval/reports/merge_report_latest.md`
- **Modified Files:**
  - `aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java`
  - `aims-backend/src/main/java/com/aimsgraph/ingest/validator/WikiPageValidator.java`

## [2026-09-17] 통제 코퍼스 25종 기반 환각/품질 검증 & 실패 사례 진단 프레임워크 구축 (2단계) [완료됨]
- **Goal:** 프롬프트의 무분별한 비대화를 지양하고, 25개 통제 코퍼스(Test Corpus)와 Ground Truth 메타데이터(`.expected.json`)를 기반으로 환각(Hallucination), 가짜 수치/코드 날조, 억지 개념 추출을 재현 가능하게 진단하는 평가 자동화 체계 구축.
- **Key Principles & Implementation:**
  1. **통제된 25개 코퍼스 및 Ground Truth 세트 (`eval/corpus/`)**:
     - 7개 다채로운 입력군 구성: Short Blog(4), Deep Arch Spec(4), Code-Heavy(4), Tool Intro(4), Mixed Technologies(3), Extremely Short / Sparse(3), Buzzword Dense(3).
     - 각 문서마다 대응하는 `.expected.json`을 작성하여 `expectedConcepts`(기준선), `forbiddenConcepts`(금지 개념), `forbiddenProjectTerms`(사칭 방지), `min/maxConcepts`, `keyFacts` 명시.
  2. **지식 출처 4단계 분류 체계 (`KnowledgeProvenance`)**:
     - 원문에 없다고 무조건 환각으로 단정하지 않고 보편 지식 보충을 고려:
       - `SOURCE_SUPPORTED`: 원문에서 직접 뒷받침됨
       - `UNSUPPORTED_OR_FABRICATED`: 금지어, 사칭 패키지, 날조된 벤치마크 (Hard Failure)
       - `GENERAL_KNOWLEDGE_CANDIDATE`: 널리 알려진 표준 패턴/기술 설명
       - `MANUAL_REVIEW_REQUIRED`: 자동 판정이 모호하여 사람의 맥락 검토가 필요한 항목
  3. **환각 인스펙터 (`HallucinationInspector`)**:
     - 원문과 위키 생성 결과, 기대 메타데이터 간 다각적 대조.
     - 금지 개념 추출(`FORBIDDEN_CONCEPT_EXTRACTED`), 프로젝트 사칭(`PROJECT_FABRICATION`), 가짜 벤치마크(`FABRICATED_BENCHMARK_METRIC`), 단문 과잉 추출 경고(`DENSITY_OVER_EXTRACTION_WARNING`).
  4. **실행 결과 아카이빙 및 최신 리포트 자동화 (`EvaluationRunner`)**:
     - 실행 이력을 `eval/runs/{timestamp}/` 하위(`raw/`, `concepts/`, `wiki/`, `results.json`, `eval_report.md`)에 영구 보존하여 프롬프트 버전 간 비교 토대 마련.
     - `eval/reports/eval_report_latest.md`에 카테고리별 Retry 지수 표, 실패 모드 상세 문맥, 수동 검토 후보군을 체계적으로 출력.
- **New Files:**
  - `eval/corpus/01_*.md` ~ `25_*.md` 및 `.expected.json` (총 50개 파일)
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/CorpusExpectedMetadata.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/KnowledgeProvenance.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/HallucinationViolation.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/EvaluationResult.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/HallucinationInspector.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/HallucinationInspectorTest.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/EvaluationRunner.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/eval/KnowledgePipelineEvaluationTest.java`
  - `eval/reports/eval_report_latest.md`

## [2026-09-17] LLM 출력 검증기 (WikiPageValidator) & 재시도 파이프라인 구축 [완료됨]
- **Goal:** LLM 응답 수신과 DB/파일시스템 영속화 사이에 독립적인 검증 계층을 구축하여 지식 그래프 무결성을 보장하고, 3회 재시도(Retry with Feedback) 루프를 통해 불량 데이터 저장을 원천 차단.
- **Key Policies & Implementation:**
  1. **순수 검증 계층 (`WikiPageValidator`)**:
     - LLM 호출 및 DB 저장을 일절 배제하고 `StructuredWikiPage`에 대한 순수 판단 및 정제만 담당.
     - **ID 규격**: `^[a-z0-9]+(?:-[a-z0-9]+)*$` 영문 소문자-하이픈 규격 엄격 검증.
     - **Summary 길이 계약**: 공백 제거 기준 `30 <= summary.trim().length() <= 80` 엄격 검증.
     - **5대 필수 섹션**: `## 정의`, `## 핵심 구성 요소`, `## 동작 원리`, `## 장점과 트레이드오프`, `## 실전 적용 사례` 존재 및 본문 20자 이상(빈 섹션 방지) 검증.
     - **Placeholder 감지**: `...`, `(내용 없음)`, `TODO`, `TBD`, `[작성 예정]` 차단.
     - **Cross-Reference 무결성 (dangling link)**: `availableConceptIds`에 없는 ID는 절대 텍스트(`**...**`)로 숨기지 않고 `UNKNOWN_REFERENCE` Hard Fail 및 Retry 요구.
     - **Auto-Sanitize 범위 제한**: 오직 자기참조(`[[current-id]]` -> 볼드 텍스트)만 안전하게 치환.
     - **Frontmatter 검증**: 필수 메타데이터(`title`, `type`, `created_at`, `source_supported`, `general_knowledge_supplemented`) 파싱 검증.
  2. **재시도 및 격리 파이프라인 (`LlmService`)**:
     - 최초 1회 + 최대 2회 재시도(총 최대 3회 호출).
     - 검증 실패 시 발생한 구체적 에러 목록만 LLM 피드백 프롬프트로 주입하여 정밀 교정 유도.
     - 3회 모두 실패 시 파일 미생성 및 Neo4j 저장 생략(Reject)하여 데이터 오염 원천 차단.
  3. **TDD 및 통합 테스트**:
     - 단위 및 경계값 테스트 (`WikiPageValidatorTest`): 29자/30자/80자/81자, 19자/20자, dangling link, self-reference, invalid slug 등.
     - 파이프라인 통합 테스트 (`WikiGenerationPipelineTest`):
       - Case 1 (1차 실패 -> 2차 성공, 총 2회 호출)
       - Case 2 (1차/2차 실패 -> 3차 성공, 총 3회 호출)
       - Case 3 (3회 모두 실패 -> Reject 미생성, 총 3회 호출)
       - Case 4 (자기참조 Auto-Sanitize -> 1회 호출 즉시 저장)
- **New Files:**
  - `aims-backend/src/main/java/com/aimsgraph/ingest/validator/ErrorType.java`
  - `aims-backend/src/main/java/com/aimsgraph/ingest/validator/ValidationError.java`
  - `aims-backend/src/main/java/com/aimsgraph/ingest/validator/ValidationResult.java`
  - `aims-backend/src/main/java/com/aimsgraph/ingest/validator/WikiPageValidator.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/validator/WikiPageValidatorTest.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/WikiGenerationPipelineTest.java`
- **Modified Files:**
  - `aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/LlmServiceTest.java`
  - `aims-backend/src/test/java/com/aimsgraph/ingest/LlmExtractionQualityTest.java`

## [2026-09-17] 지식 컴파일러 프롬프트 2차 정밀 고도화 (자기참조 차단 & 시간축 메타데이터 주입) [완료됨]
- **Goal:** ChatGPT 2차 정밀 피드백을 반영하여 지식 추출, 위키 본문 생성, 지능형 병합 전반의 경계 조건과 파이프라인 검증 구조 고도화.
- **Key Improvements:**
  1. **개념 추출 (1단계)**:
     - Quota 상충 해소: `There is NO required target range or minimum number of concepts.` 및 정보 밀도에 따른 가이드라인 명시 (`return approximately 3-15 concepts when the source contains enough information density, but return fewer when fewer concepts are genuinely supported`).
  2. **위키 본문 생성 (2단계)**:
     - 기술 언급 규칙 합리화: 무조건적 차단 대신 무관한 기술 유입을 차단하고 보편 지식 설명용 기술 언급은 허용하되 사용자 프로젝트의 것으로 날조하는 것만 엄격히 금지.
     - 자기 참조(Self-Reference) 원천 차단: 프롬프트 규칙에 금지 조항 추가 및 코드 레벨에서 생성 대상 노드 ID를 `AVAILABLE CONCEPT IDS` 목록에서 동적으로 배제하여 `[[자기자신]]` 링크 생성 방지.
     - Frontmatter 출처 수준(Provenance) 명시: `source_supported: true`, `general_knowledge_supplemented: true` 메타데이터 자동 주입.
  3. **지능형 병합 (3단계)**:
     - "authoritative" 지시 완화: `ONE cohesive, consolidated wiki document`로 수정하여 무분별한 덮어쓰기 방지.
     - 시간축(Temporal change) 메타데이터 주입: 파일의 최종 수정 일시(Existing)와 현재 일시(New)를 추출하여 프롬프트 헤더(`=== EXISTING VERSION (Last Modified: ...) ===`, `=== NEW VERSION (Generated: ...) ===`)에 주입함으로써 시간 흐름에 따른 진화/상태 갱신을 정확히 판별하도록 개선.
- **Modified Files:**
  - `aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java`

## [2026-09-17] 지식 컴파일러 프롬프트 전면 개편 (신뢰성 & 지능형 병합 고도화) [완료됨]
- **Goal:** ChatGPT 피드백을 기반으로 단순 요약이 아닌 신뢰할 수 있는 Zettelkasten Second Brain 구축을 위해 프롬프트 전면 개편.
- **Key Improvements:**
  1. **개념 추출 (ConceptExtractor / UnifiedGraphExtractor)**:
     - 억지 개념 생성을 유발하던 하한선(quota) 제거: 최소 개수 제한 없이 원문 신뢰도 기반 추출 (`No minimum number of concepts`, 정보 밀도에 따라 3~15개 유동적 추출).
     - 고유명사 필터링 정교화: 단순 1회성 언급은 제외하되, 원문에서 역할/아키텍처/중요성이 유의미하게 설명된 고유명사는 보존.
  2. **위키 본문 생성 (Wiki Generation)**:
     - KNOWLEDGE BOUNDARY 규칙 도입: 원문 기반 사실과 보편 엔지니어링 지식의 경계를 명확히 분리하여, 원문에 없는 가짜 프로젝트 아키텍처/코드/벤치마크 날조(Hallucination) 원천 차단.
     - AVAILABLE CONCEPT IDS 주입: 생성 프롬프트에 실제 추출된 노드 ID 목록을 주입하여 가짜 크로스 레퍼런스 ID 생성 방지.
  3. **지능형 병합 (Merge & Refine)**:
     - `NEWER information wins` 규칙 폐기: 최신 정보가 무조건 옳다고 가정하지 않고, 모순 발생 시 시간적/맥락적 차이를 명시적으로 보존.
     - 문서 비대화(Bloat) 방지: 단순 덧붙이기(concat)를 금지하고, 중복 표현 제거 및 핵심 지식 위주의 간결한 합성(Concise Synthesis) 강제.
- **Modified Files:**
  - `aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java`

## [2026-09-17] 프론트엔드 워크스페이스 Zip 내보내기(Export) UI 연동 및 403 강제 로그아웃 버그 수정 [완료됨]
- **Goal:** 백엔드에 구축되어 있던 워크스페이스 Zip 내보내기 API(`GET /api/v1/workspaces/{workspace_id}/export`)를 프론트엔드 사이드바 헤더에 연결하고, 내보내기 실패 시 로그인 창으로 강제 리다이렉트되던 안티패턴을 수정.
- **Bug Fix & Improvement:**
  - `frontend/src/services/api.ts`:
    - `customFetch`의 자동 로그아웃 조건을 `401 || 403`에서 오직 `401`(세션 만료)로 한정하여, 단순 권한 불일치(403) 시 세션이 파기되지 않도록 분리.
    - `exportWorkspace(workspaceId)` 함수에 백엔드 테넌트 검증에 필요한 `X-Workspace-ID` 헤더 및 `?workspaceId=` 쿼리 파라미터를 추가하여 `403 Workspace mismatch` 원천 방어.
  - `frontend/src/components/layout/Sidebar.tsx`:
    - 사이드바 헤더에 `Download` 및 `Loader2` 버튼 추가, 다운로드 중 비동기 로딩 스피너 및 에러 메시지 얼럿 연동.

## [2026-09-17] LLM API 실시간 연결 진단 엔드포인트 및 라이브 테스트 구축 [완료됨]
- **Goal:** 시스템에 주입된 LLM API 키(OpenAI 등)의 실제 동작 및 응답 수신 여부를 검증하기 위해, JUnit 라이브 연결 테스트와 인증 불필요 실시간 헬스체크 API 엔드포인트(`GET /api/v1/health/llm`)를 구현.
- **New Files:**
  - `LlmLiveConnectionTest.java`: 실제 OpenAI API와 핑퐁 통신을 수행하여 응답 소요 시간(Latency) 및 PING_OK 수신을 검증하는 라이브 테스트.
  - `LlmHealthController.java`: 실시간 헬스체크 REST API (`/api/v1/health/llm`). 마스킹된 키 정보, 레이턴시, 모델 응답 반환.
  - `LlmHealthControllerTest.java`: 컨트롤러 단위 테스트.
- **Modified Files:**
  - `SecurityConfig.java`: `/api/v1/health/**` 경로를 permitAll()에 등록하여 토큰 없이 즉시 진단 가능하도록 허용.
  - `API_SPEC.md`: Health Check API 명세 추가.

## [2026-07-18] AI 규칙 세분화(Rules Partitioning) 및 스마트 AI 리뷰어 구현 [완료됨]
- **Goal:** 프로젝트의 복잡성 증가에 대비하여 규칙 파일을 공통, 백엔드, 프론트엔드로 세분화하고, AI 리뷰어가 PR 변경 영역을 자동 감지해 해당 규칙 파일만 동적 주입하도록 튜닝하여 토큰 절약 및 검사 정확도 제고.
- **Affected Files:**
  - [project-rules.md](file:///c:/second%20brain/rules/common/project-rules.md)
  - [backend-rules.md](file:///c:/second%20brain/rules/backend/backend-rules.md)
  - [frontend-rules.md](file:///c:/second%20brain/rules/frontend/frontend-rules.md)
  - [run-ai-review.js](file:///c:/second%20brain/.github/scripts/run-ai-review.js)
  - [GEMINI.md](file:///c:/second%20brain/GEMINI.md)

## [2026-07-18] AI 코드 리뷰 및 테스트 자동화 파이프라인 구축 가이드 추가 [완료됨]
- **Goal:** 프로젝트 헌법 및 가드레일을 준수하기 위한 AI 자동화 코드 리뷰 규칙(/review 스킬 연동) 및 Testcontainers 기반의 CI/CD 파이프라인 구축 가이드를 생성함.
- **Affected Files:**
  - [PIPELINE_GUIDE.md](file:///c:/second%20brain/docs/PIPELINE_GUIDE.md)

## [2026-07-16] @ControllerAdvice 기반 글로벌 예외 핸들러 도입 [완료됨]
- **Goal:** 컨트롤러마다 제각각이던 try-catch 예외 처리를 `@RestControllerAdvice` 기반의 글로벌 핸들러로 통합하여, 모든 API의 에러 응답을 `ErrorResponse(error, message, timestamp)` 포맷으로 표준화.
- **원칙:** 즉시 재시도 또는 로컬 폴백이 필요한 경우에만 컨트롤러 내부 try-catch를 유지하고, 나머지는 글로벌 핸들러로 위임.
- **New Files:**
  - `GlobalExceptionHandler.java`: AccessDenied, 요청 파싱, Validation, 파일 업로드 크기 초과, IOException, 최종 안전망(Exception) 등 계층적 핸들러
  - `ErrorResponse.java`: 통일된 에러 응답 record (error, message, timestamp)
- **Modified Files:**
  - `AnalyzeController.java`: analyzeFiles(), analyzeNotion() try-catch 제거 → throws Exception
  - `WorkspaceController.java`: createWorkspace(), getWorkspaceGraph(), deleteWorkspaceData() try-catch 제거 → throws IOException
  - `AuthController.java`: deleteAccount() try-catch 및 e.printStackTrace() 제거 → throws IOException
  - `WikiController.java`: getWikiContent() try-catch 제거 → throws IOException
- **유지된 try-catch (로컬 폴백 필요):**
  - `WorkspaceController.listWorkspaces()`: IOException 시 빈 리스트 반환 (graceful degradation)
  - `WikiController.resolveSlug()`: Exception 시 null 반환 (직접 파일 조회 폴백)
  - `NotificationController`: SSE emitter 실패 시 즉시 리소스 정리

## [2026-06-09] Chat-Notion MCP 연동 기능 추가 (Micro-Task)
- **Goal:** 채팅(Query) API 호출 시 명시적인 파라미터(useNotion=true)가 주어질 때만 Notion 문서를 검색하여 LLM에 프롬프트로 주입.
- **Affected Files:**
  - QueryController.java: 선택적 파라미터 useNotion, 
otionPageId 추가.
  - LlmService.java: query 메서드에서 useNotion이 true일 경우 NotionIngestService 호출 및 프롬프트 병합.
  - NotionIngestService.java: 검색 또는 페이지 조회 보조 메서드 추가.
  - API_SPEC.md: API 명세 갱신.
- **Constraints:**
  - 기존 로컬 마크다운 검색 로직 훼손 금지.
  - 매번 Notion을 호출하여 레이턴시를 낭비하지 않도록 파라미터로 엄격히 분리할 것.
## [2026-06-09] GraphCanvas MCP 모달 토글 버그 수정 (Micro-Task)
- **Goal:** 프론트엔드의 MCP 연결 플로팅 버튼이 누를 때마다 켜졌다 꺼졌다(토글) 되도록 수정.
- **Affected Files:**
  - c:\second brain\frontend\src\components\graph\GraphCanvas.tsx
- **Details:** 
  - onClick={() => setShowMcpModal(true)} 로 하드코딩된 부분을 onClick={() => setShowMcpModal(!showMcpModal)} 또는 (prev => !prev)로 변경.
## [2026-06-09] ChatPanel 노션 토글 버튼 UI 개선 (Micro-Task) [완료됨]
- **Goal:** 프론트엔드 채팅 패널의 노션 검색 토글 버튼에서 번잡한 텍스트를 제거하고 아이콘만 미니멀하게 렌더링.
- **Affected Files:**
  - c:\second brain\frontend\src\components\chat\ChatPanel.tsx
- **Details:** 
  - {useNotion ? "Notion Search: ON" : "Notion Search: OFF"} 텍스트 출력부 제거.
  - 아이콘만 남기고 버튼 패딩 등(px-2.5 -> p-2 등)을 아이콘 전용 버튼에 맞게 최적화.
## [2026-06-09] LlmService 챗봇 인지능력(Awareness) 프롬프트 개선 (Micro-Task) [완료됨]
- **Goal:** 채팅봇이 "노션 접근 권한이 없다"고 헛소리를 하는 문제(System Prompt 부재) 해결.
- **Affected Files:**
  - c:\second brain\aims-backend\src\main\java\com\aimsgraph\ingest\LlmService.java
- **Details:** 
  - query 메서드 내부의 enrichedQuery 프롬프트와 queryDirect 호출 시 사용되는 베이스 프롬프트를 강화.
  - "You are a helpful assistant integrated with Notion MCP and Second Brain Wiki. If the user asks if you have access to Notion, confidently reply YES, because the backend system dynamically injects Notion context when the user toggles the Notion button in the UI." 라는 식의 권한 인지 문구를 하드코딩 주입.
## [2026-06-09] 노션 전역 검색 기반 Agentic RAG 구현 (Feature)
- **Goal:** 사용자가 노션 페이지 ID를 명시하지 않더라도, AI가 질문 키워드를 바탕으로 노션 Search API를 호출하여 동적으로 가장 관련 있는 문서를 찾아오도록 구현.
- **Affected Files:**
  - c:\second brain\aims-backend\src\main\java\com\aimsgraph\ingest\NotionIngestService.java (searchNotionPageId 추가)
  - c:\second brain\aims-backend\src\main\java\com\aimsgraph\ingest\LlmService.java (query 메서드에서 fallback 검색 호출)

## 2026-06-10: Graph-based Agentic Traversal Implementation [완료됨]
- Issue: The current context retrieval passes all filenames to the LLM instead of traversing the Neo4j graph.
- Goal: Implement LangChain4j AiServices and Tools for true Agentic Graph Traversal.
- Plan: Create GraphTools with searchGraph, getNodeContext, readWikiPage, and readNotionPage. Modify LlmService.java to use AiServices.builder() for generating agentic responses.

## 2026-06-10: Remove Notion Page ID Manual Input
- Issue: The user wants to remove the manual Notion Page ID input field since the AI can now autonomously search Notion via Agentic Workflow.
- Goal: Remove Notion Page ID UI from frontend and simplify backend API to omit this field.

## 2026-06-10: Chat-to-Build Delegation (Save Insight Tool)
- Issue: The user wants to build the second brain via the chat interface without losing Zettelkasten quality.
- Goal: Add saveToSecondBrain tool to GraphTools so the Chat Agent can delegate knowledge ingestion to the dedicated wiki pipeline.
- Plan: Update GraphTools to support saving, pass model credentials, and enforce detailed summary extraction in the Chat Agent's prompt.

## 2026-06-10: RightPanel Resize Feature
- Issue: The user wants to resize the right panel (chat/viewer) by dragging its left border.
- Goal: Implement drag-to-resize functionality in RightPanel.tsx using React state and mouse events.

## 2026-06-10: GitHub MCP Connection Implementation [완료됨]
- Goal: Implement GitHub MCP connection UI and token verification in GraphCanvas, api.ts, and useAppStore.ts.
- Status: Done

## 2026-06-10: BYOK DB Encryption Migration [완료됨]
- Goal: Migrate API tokens from frontend localStorage to backend AES-encrypted DB storage.
- Status: Done

## 2026-06-10: B2B Login 초 & JWT Auth [완료됨]
- Goal: Implement JWT-based login page and Spring Security on backend for multi-user support.
- Status: Done

## 2026-06-11: 프론트엔드/백엔드 완전 로그아웃 (Redis Blacklisting) 구현 [완료됨]
- Goal: 프론트엔드 글로벌 로그아웃 버튼(Zustand 상태 초기화, /login 리다이렉트) 구현 및 백엔드 POST /v1/auth/logout API를 통한 Redis JWT Blacklisting 연동 (안 2 채택).

## 2026-06-11: 로그아웃 버튼 위치 변경 (UI)
- Goal: 사용성 개선을 위해 Sidebar.tsx 우측 상단의 로그아웃 버튼을 좌측 사이드바 하단(Footer) 영역으로 크게 이동.

## 2026-06-11: 사이드바 하단 사용자 계정 표시 (UI)
- Goal: 좌측 사이드바 하단의 로그아웃 버튼 옆에 현재 로그인한 사용자 아이디(`currentUser`)가 표시되도록 UI 레이아웃 개선.

## 2026-06-11: 로그인 직후 빈 화면(Empty State) 버그 수정
- Goal: 로그인 완료 직후 페이지 진입 시 새로고침 전까지 그래프가 보이지 않던 문제를 해결하기 위해, `page.tsx`의 렌더링 의존성(dependency array)에 `isLoggedIn` 상태를 추가하여 즉시 백엔드에서 데이터를 불러오도록 수정.

## 2026-06-11: B2B 계정 영구 탈퇴 기능 구현 [완료됨]
- Goal: B2B 보안 정책 및 데이터 격리 파기 요구사항에 맞추어, 프론트엔드 탈퇴 확인 경고 모달 UI 및 백엔드 DELETE /api/v1/auth/account API(Neo4j 노드 영구 삭제, 물리 마크다운 디렉토리 재귀 완전 삭제, RDBMS User 정보 삭제 일괄 트랜잭션 처리) 구현.

## 2026-06-23: AI 모델 선택 개선 및 가상 스레드 위키 쓰기 동시성 제어 [완료됨]
- **Goal:** 위키 페이지 생성 시 사용자가 선택한 AI 모델(DeepSeek v4 등)이 반영되지 않던 버그를 수정하고, 가상 스레드 환경에서 동일 위키 파일 동시 쓰기로 인한 Race Condition을 방지하도록 ReentrantLock 기반의 파일 락 장치 도입.
- **Affected Files:**
  - c:\second brain\aims-backend\src\main\java\com\aimsgraph\ingest\LlmService.java
  - c:\second brain\aims-backend\src\test\java\com\aimsgraph\ingest\LlmServiceTest.java

## 2026-06-23: LangChain4j 1.15.1 업그레이드 및 ChatModel 마이그레이션 [완료됨]
- **Goal:** 에이전트의 10회 도구 호출 제한 예외(Fallback 버그)를 근본적으로 차단하기 위해 LangChain4j 1.x 대 메이저 업그레이드를 수행하고, 이에 따른 ChatLanguageModel -> ChatModel API 마이그레이션을 적용합니다.
- **Affected Files:**
  - [build.gradle](file:///c:/second%20brain/aims-backend/build.gradle): langchain4j, langchain4j-open-ai 버전을 1.15.1로 상향 조정하고 langchain4j-core 의존성 추가
  - [LlmService.java](file:///c:/second%20brain/aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java): ChatLanguageModel을 ChatModel로 변경, generate()를 chat()으로 교체, AiServices 빌더에 maxToolCallingRoundTrips(50) 적용
  - [LlmServiceTest.java](file:///c:/second%20brain/aims-backend/src/test/java/com/aimsgraph/ingest/LlmServiceTest.java): ChatModel Mocking 및 ChatResponse 빌더 구조 테스트 적응
- **Details:**
  - 마이그레이션 완료 후 `./gradlew test` 검증 수행 완료.
  - 로컬 환경 로그인 토큰을 사용하여 `/api/v1/query` API를 연동 테스트하여 `searchGraph` -> `getNodeContext` -> `readWikiPage` 에 이르는 에이전트 sequential 툴 트래버설 흐름이 예외 없이 완벽하게 정상 동작함을 최종 검증 완료.

## 2026-06-30: AI Wiki Ingestion Robustness (Structured Outputs Integration) [완료됨]
- **Goal:** 프롬프트 기반 지식 추출에서 발생할 수 있는 JSON 파싱 에러를 근본 차단하기 위해 `extractKnowledge` 및 `callResponsesAPI` 메서드를 API 레벨의 Structured Outputs 규격 강제 방식으로 전환합니다.
- **Affected Files:**
  - [LlmService.java](file:///c:/second%20brain/aims-backend/src/main/java/com/aimsgraph/ingest/LlmService.java)
  - [LlmServiceTest.java](file:///c:/second%20brain/aims-backend/src/test/java/com/aimsgraph/ingest/LlmServiceTest.java)
  - [LlmExtractionQualityTest.java](file:///c:/second%20brain/aims-backend/src/test/java/com/aimsgraph/ingest/LlmExtractionQualityTest.java)
- **Plan:**
  - `extractKnowledge`와 `callResponsesAPI` 호출 시 `gpt-4o-mini` 모델 호출부에 `json_schema` 스펙을 주입하거나 LangChain4j `AiServices` 인터페이스를 통해 Structured Output을 보장합니다.
  - 기존의 수동 문자열 정규식 가공 구문(`replaceAll`)을 제거하고 JSON 파이프라인의 구조적 안전성을 검증합니다.

## 2026-07-16: MSSQL WikiPage 본문 캐싱 하이브리드 동기화 도입 [완료]
- **Goal:** 물리 마크다운 파일 실시간 디스크 I/O 조회로 인한 병목(특히 다중 에이전트 그래프 트래버설 시 누적 레이턴시)을 제거하기 위해, RDBMS(MSSQL)의 `WikiPage` 테이블을 캐시 저장소로 활용하는 하이브리드 아키텍처 도입.
- **DDL:**
  - `WikiPage` 테이블에 본문 저장을 위한 `content NVARCHAR(MAX)` 컬럼 추가.
- **MyBatis 매퍼:**
  - `WikiPageMapper.java` 및 `WikiPageMapper.xml`를 생성하여 SELECT, UPDATE, MERGE(Upsert) 구문 지원.
- **비즈니스 로직 (WikiService):**
  - Cache-first 조회 제공 (`getWikiContent`). Cache Miss 시에만 디스크에서 Lazy Loading 후 DB 캐시 Upsert.
  - 양방향 동기화:
    - 웹 UI 수정 시 (`saveOrUpdateWiki`): Redisson 락을 사용한 분산 동기화 제어(가상 스레드 친화적), DB 캐시 및 물리 파일 덮어쓰기, Neo4j 노드 업데이트.
    - 외부 도구 로컬 수정 시 (`syncFileToDb`): 콘텐츠 감지 후 DB 캐시의 content 및 content_hash 최신화.
- **컨트롤러 리팩토링:**
  - `WikiController.java`에서 직접 디스크 I/O와 Neo4j 조회를 제어하던 로직을 `WikiService`로 위임하여 캡슐화.
- **CORS Preflight OPTIONS 대응 패치:**
  - 스프링 시큐리티 필터 체인 통과 시 브라우저의 OPTIONS Preflight 차단으로 인한 `Failed to fetch` 오류 해결을 위해, `SecurityConfig.java`에 OPTIONS 전역 허용 매처(`HttpMethod.OPTIONS, "/**"`) 설정 보강 완료.
