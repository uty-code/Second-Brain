# 프로젝트: AIMS-Graph Backend

> 이 파일은 Antigravity가 세션 시작 시 **자동으로 컨텍스트 윈도우에 로드**하는 프로젝트 헌법입니다.
> 글로벌 규칙(`~/.gemini/GEMINI.md`)보다 우선 적용됩니다.

## 기술 스택
- Spring Boot 3.x (Java 21, Virtual Threads)
- MSSQL (MyBatis) — 원본 메타데이터 + 아웃박스
- Neo4j 5 — 지식 그래프
- Apache Kafka — 비동기 이벤트 스트림
- Redis (Redisson) — 분산 락 + 캐시

## CRITICAL: 아키텍처 규칙
- **절대 원칙 (Andrej Karpathy's LLM Wiki Pattern)**: RAG(문서 청킹, 벡터 임베딩, 벡터 DB) 기술은 어떠한 경우에도 **절대 사용을 금지**한다. 대신, 입력된 원본 소스(Raw sources)를 LLM 기반의 에이전틱 워크플로우(Agentic Workflow)를 통해 분석하고, 상호 링크가 촘촘하게 걸린 **정형화된 마크다운 위키(Processed Wiki) 파일과 인덱스(Index)** 형태로 변환 및 누적하는 아키텍처만을 설계하고 유지해야 한다.
- DB 동기화 시 반드시 '트랜잭셔널 아웃박스 패턴'을 엄수할 것.
- 모든 블로킹 I/O 작업은 Java 21의 가상 스레드(Virtual Threads)로 처리할 것.
- 인메모리 DB(H2 등) 사용 절대 금지. 테스트 시에도 Testcontainers를 사용할 것.
- Neo4j 스키마는 `docs/SCHEMA.md`의 온톨로지를 반드시 준수할 것.

## CRITICAL: 프론트엔드 규칙
- 프론트엔드 UI/UX 작업 시 **반드시 `docs/UI_GUIDE.md`를 먼저 읽고** 안티패턴(AI Slop)을 배제할 것.
- 컴포넌트 추가 전 Tailwind CSS 전역 변수 및 Shadcn UI 컴포넌트 구조를 준수할 것.

## CRITICAL: 개발 프로세스
- 새 기능 구현 시 반드시 실패하는 테스트를 먼저 작성하고 통과시킬 것 (TDD).
- API 키 등 민감 정보는 하드코딩하지 말고 환경변수로 주입받을 것.
- 커밋 메시지는 conventional commits 형식을 따를 것 (ex. `feat:`, `fix:`, `refactor:`).
- 작업(Phase) 완료 시 반드시 루트 폴더의 `tasks.md` 파일에 완료 상태(`[x]`)를 갱신하고, 개별 `phase-*.md` 문서 최상단에도 `Status: [완료됨]` 및 진행 히스토리를 남길 것.
- **[자동 문서화]** 아키텍처나 API 등 주요 로직 변경 시, 사용자가 별도로 지시하지 않더라도 `docs/` 폴더(`API_SPEC.md`, `ARCHITECTURE.md`, `ADR.md` 등)를 에이전트가 스스로 즉각 업데이트하여 항상 최신 상태를 유지할 것.

## CRITICAL: 하네스(Harness) 파이프라인 규칙
- 이 프로젝트에서 **하네스(Harness)는 일반적인 테스트 자동화(QA)가 아닌, '프로젝트 기획 분석 및 서브 에이전트 자율 구현 파이프라인 엔진'을 지칭**한다.
- 하네스 프레임워크 및 엔진에 관한 상세한 규칙과 가이드라인은 `.agents/skills/harness/SKILL.md`를 반드시 따를 것.
- 초기 구축 이후의 단순 개선/유지보수 작업은 절대 새로운 `phase-*.md`를 만들지 않고, 오직 `docs/updates.md` 단일 파일에 누적 기록하여 작업을 진행할 것.

## CRITICAL: 안전 가드레일
- `rm -rf`, `git reset --hard`, `git push --force` 등 파괴적 명령어 사용 금지.
- 동일한 테스트에서 3회 이상 연속 실패 시, 다른 접근 방식을 시도하거나 사용자에게 개입을 요청할 것.

## 명령어
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 실행: `./gradlew bootRun`
- 린트: `./gradlew spotlessCheck`

## 참조 문서
- 기획: `docs/PRD.md`
- 아키텍처: `docs/ARCHITECTURE.md`
- 결정 기록: `docs/ADR.md`
- DB 스키마: `docs/SCHEMA.md`
- API 명세: `docs/API_SPEC.md`
- UI/UX 가이드: `docs/UI_GUIDE.md` (프론트엔드 작업 시 필수)
- 위키 규칙: `docs/WIKI_SCHEMA.md`
- 환경 세팅: `docs/SETUP.md`
