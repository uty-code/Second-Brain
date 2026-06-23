# Updates Log

## 2026-06-11: LLM Extraction Quality Validation (QA Task)
- **이슈:** `LlmService.java` 내의 지식 추출 방식을 OpenAI `/v1/files` 기반에서 프롬프트 텍스트 직접 인젝션 기반(`gpt-4o-mini` Chat Completions)으로 마이그레이션 함에 따라, 새로 적용된 방식의 실제 결과물 품질이 이전 방식보다 월등한지 철저한 점검(QA)이 필요함.
- **요구사항:**
  - 서브 에이전트는 샘플 텍스트 데이터를 주입하여 백엔드의 `analyzeTextWithOpenAI` 프로세스(또는 `callResponsesAPI`)를 테스트해야 한다.
  - 생성된 JSON 지식 그래프 구조(Nodes, Links)가 프롬프트 제약조건을 얼마나 엄격하게 준수하는지(Hallucination 없는지, JSON 파싱 에러 없는지) 평가한다.
  - 평가 결과를 사용자가 볼 수 있도록 상세하게 보고한다.

## 2026-06-11: 프론트엔드 창고 이름 한국어 허용 (버그 픽스)
- **이슈:** UI에서 한국어로 창고 이름(Vault Name)을 생성하려 할 때, 백그라운드 정규식 필터링 변수에 의해 버튼이 비활성화되는 버그가 발생함.
- **해결 방안:** Sidebar.tsx 내부의 불필요한 영문 전용 정규식을 완전히 삭제하고, 생성 버튼의 disabled 조건에서 제거.
- **결과:** 사용자가 한글을 포함해 어떤 문자로든 정상적으로 창고 이름 생성이 가능해짐.
