# Phase 11: LLM 쿼리 컨텍스트 자가 위키 주입 (RAG 지양, Local Wiki Markdown 기반 Context Retrieval 시스템 구축)

> **Status: [완료됨]**
>
> - **히스토리**:
>   - RAG와 Vector DB를 지양하는 Andrej Karpathy's LLM Wiki Pattern의 사상을 준수하기 위해 로컬 지식 위키 디렉토리를 탐색하고 gpt-4o-mini를 이용하여 질문에 연관된 3~5개의 위키 마크다운 문서를 1차적으로 선정, 이를 최종 LLM 프롬프트의 컨텍스트로 결합하여 답변하도록 LlmService.java의 query 메서드를 업데이트함.
>   - LlmServiceTest.java에 단위 테스트를 성공적으로 추가하여 기능을 검증함.

## 구현 내용
- `LlmService.java`:
  - `collectMarkdownFiles` 헬퍼 메서드를 통해 `concepts`, `entities`, `insights` 경로의 모든 `.md` 파일을 수집.
  - 1차 LLM 호출(`gpt-4o-mini`)을 통해 사용자의 질문과 가장 잘 매핑되는 파일 3~5개를 선정.
  - 선택된 마크다운 문서 내용을 읽어와 `enrichedQuery`에 주입.
  - 예외 발생 시나 디렉토리가 없을 시 오리지널 직접 쿼리(`queryDirect`)로 안전하게 fallback 하도록 가드레일 설계.
- `LlmServiceTest.java`:
  - 임시 디렉토리를 활용한 `query_shouldRetrieveWikiFilesAndEnrichQuery` 단위 테스트 추가.
  - `queryDirect`를 Spy를 사용하여 Mocking 및 캡쳐함으로써 온전히 검증 성공.
