# 에이전트 전용 위키 작성 규칙 (WIKI_SCHEMA.md)

이 문서는 LLM 에이전트가 마크다운 위키(The Wiki)를 생성하거나 갱신할 때 반드시 지켜야 하는 **'The Schema'** 규칙입니다.

## 1. 위키 디렉토리 구조
```text
wiki/
├── index.md          # 전체 카탈로그 (자동 갱신)
├── log.md            # 연대기적 변경 기록 (자동 갱신)
├── concepts/         # 추상적 개념/토픽 문서
├── entities/         # 구체적 인물/조직/기술 문서
└── insights/         # Query 오퍼레이션에서 Filed back된 분석 문서
```

## 2. 파일 네이밍 규칙
- 영문 소문자 + 하이픈(`-`) 조합만 사용. (ex. `llm-wiki.md`)
- 한글 제목은 Frontmatter의 `title` 필드에 기록하고, 파일명은 영문 slug를 사용.

## 3. YAML Frontmatter 명세
```yaml
---
title: "LLM Wiki"              # [필수] 문서 제목 (한글 가능)
aliases: ["Second Brain"]       # [선택] 동의어 (교차 참조 시 매칭용)
type: concept                   # [필수] concept | entity | insight
tags: ["AI", "Architecture"]   # [선택] 분류 태그
source_refs: ["ext-12345"]      # [선택] 원본 RawSource ID 역참조
created_at: 2026-06-02          # [필수] 생성일
updated_at: 2026-06-02          # [필수] 최종 갱신일
---
```

## 4. 교차 참조 (Cross-Referencing) 규칙
- 다른 위키 문서를 언급할 때는 반드시 `[[Concept Name]]` 포맷을 사용.
- LLM 에이전트가 교차 참조를 생성할 때는 단순 연결을 넘어서 **논리적 관계성(Type)**을 반드시 지정해야 함.
- 후처리 파이프라인(IngestionWorker)에서 `EXTENDS`, `CONTRADICTS`, `DEPENDS_ON`, `EXPLAINS`, `RELATED_TO` 등 지정된 타입의 온톨로지 엣지(Edge)로 Neo4j에 자동 반영됨.

## 5. 중앙 카탈로그 (`index.md`) 업데이트 규칙
- 새 문서 생성 시, `index.md`의 카테고리(`## Concepts`, `## Entities`, `## Insights`) 하위에 링크 추가.
- Redis 분산 락(`wiki:index.md`) 획득 후 수정.
- 포맷: `- [[concept-name]]: 한 줄 요약`

## 6. 연대기적 기록 (`log.md`) 업데이트 규칙
- 모든 변경 시 `log.md` **최상단**에 로그 추가.
- Redis 분산 락(`wiki:log.md`) 획득 후 수정.
- 포맷: `- [YYYY-MM-DD HH:MM] [INGEST|UPDATE|LINT|FILE_BACK] [[page-name]]: 변경 요약`

## 7. Lint 데몬 판별 규칙
| 이슈 코드 | 판별 기준 | 자동 교정 |
|---|---|---|
| `ORPHAN_PAGE` | `index.md`에 링크 없는 위키 페이지 | ✅ |
| `BROKEN_LINK` | `[[링크]]` 대상 파일 미존재 | ❌ |
| `STALE_DATA` | `updated_at`이 90일 이상 경과 | ❌ |
| `CONTRADICTION` | Neo4j `CONTRADICTS` 엣지 존재 | ❌ |
| `MISSING_FRONTMATTER` | 필수 필드 누락 | ✅ |
