# 데이터베이스 및 그래프 온톨로지 명세 (SCHEMA.md)

## 1. 관계형 데이터베이스 (MSSQL)

### 1.0 `Workspace` 테이블 (테넌트 메타데이터 설정)
다중 사용자의 세컨드 브레인(금고) 기본 설정을 관리합니다.
- `id` (VARCHAR(50), PK): 테넌트 고유 식별자 (`workspace_id`)
- `name` (NVARCHAR(100)): 금고 이름 (ex. '업무용 뇌')
- `created_at` (DATETIME2, DEFAULT GETUTCDATE()): 생성 시간

### 1.1 `RawSource` 테이블
원본 문서의 메타데이터와 실제 마크다운 파일(S3/Git)을 매핑합니다.
- `id` (BIGINT, PK): 고유 식별자
- `workspace_id` (VARCHAR(50), INDEX): 테넌트(사용자/워크스페이스) 식별자
- `source_uri` (VARCHAR): 원본 문서 위치 (ex. `s3://wiki/raw/2026/06/...`)
- `title` (VARCHAR): 문서 제목
- `content_hash` (VARCHAR(64)): 내용의 SHA-256 해시 (중복 방지, UNIQUE 제약)
- `source_type` (VARCHAR(20)): 소스 유형 (`MARKDOWN`, `NOTION`, `SLACK`)
- `status` (VARCHAR(20)): 처리 상태 (`RECEIVED`, `INGESTED`, `FAILED`)
- `created_at` (DATETIME2, DEFAULT GETUTCDATE()): 수집 시간
- `updated_at` (DATETIME2): 최종 갱신 시간

### 1.2 `WikiPage` 테이블
LLM이 생성/관리하는 위키 페이지의 메타데이터를 추적합니다.
- `id` (BIGINT, PK, IDENTITY): 고유 식별자
- `workspace_id` (VARCHAR(50), INDEX): 테넌트(사용자/워크스페이스) 식별자
- `page_path` (VARCHAR(500)): 마크다운 파일 경로 (※ workspace_id와 복합 UNIQUE)
- `title` (NVARCHAR(200)): 페이지 제목
- `page_type` (VARCHAR(20)): 종류 (`CONCEPT`, `ENTITY`, `INSIGHT`, `INDEX`, `LOG`)
- `content_hash` (VARCHAR(64)): 현재 내용의 SHA-256 해시
- `created_at` (DATETIME2, DEFAULT GETUTCDATE()): 생성 시간
- `updated_at` (DATETIME2): 최종 갱신 시간

### 1.3 `OutboxEvent` 테이블 (Transactional Outbox)
데이터의 최종 일관성을 보장하기 위해 Kafka로 발행될 이벤트를 임시 저장합니다.
- `id` (UNIQUEIDENTIFIER, PK): 이벤트 ID (UUID)
- `workspace_id` (VARCHAR(50), INDEX): 테넌트 식별자 (Kafka 파티셔닝 키로 활용 가능)
- `aggregate_type` (VARCHAR): 엔티티 종류 (ex. `DOCUMENT`, `CONCEPT`)
- `aggregate_id` (VARCHAR): 엔티티 식별자
- `event_type` (VARCHAR): 이벤트 종류 (ex. `CREATED`, `UPDATED`)
- `payload` (NVARCHAR(MAX)): JSON 형태의 실제 변경 데이터
- `status` (VARCHAR): 상태 (`PENDING`, `PROCESSED`, `FAILED`)
- `created_at` (DATETIME): 생성 시간

## 2. 그래프 데이터베이스 온톨로지 (Neo4j)

### 2.1 노드 라벨 및 필수 속성 (Node Labels & Properties)

| 라벨 | 설명 | 필수 속성 |
|---|---|---|
| `:Source` | 외부에서 수집된 원본 문서 (MSSQL RawSource와 1:1) | `workspaceId` (String), `source_id` (Long), `title` (String), `uri` (String), `ingested_at` (DateTime) |
| `:Concept` | 위키 내 핵심 개념/토픽 (ex. `LLM Wiki`) | `workspaceId` (String), `name` (String), `summary` (String), `page_path` (String), `embedding` (Float Array, 의미론적 검색용) |
| `:Entity` | 구체적 인물, 조직, 기술 스택 등 | `workspaceId` (String), `name` (String), `type` (String: `PERSON`/`ORG`/`TECH`), `page_path` (String) |

### 2.2 관계 및 속성 (Relationships & Properties)
| 관계 | 방향 | 설명 | 속성 |
|---|---|---|---|
| `MENTIONS` | `(Source)->(Concept)` | 원본 문서가 특정 개념을 언급 | `weight` (Float, 언급 빈도) |
| `RELATES_TO` | `(Concept)->(Concept)` | 개념 간 일반적 연관 | `context` (String, 관계 맥락 요약) |
| `SUPPORTS` | `(Concept)->(Concept)` | 한 개념이 다른 개념을 뒷받침 | `evidence` (String) |
| `CONTRADICTS` | `(Concept)->(Concept)` | 두 개념이 서로 모순 (Lint 대상) | `reason` (String) |
| `DERIVED_FROM` | `(Concept)->(Source)` | 개념이 어떤 원본으로부터 도출됨 | `extracted_at` (DateTime) |

### 2.3 인덱스 전략
```cypher
-- 고유성 보장 (멀티 테넌시 고려: workspaceId + name 복합 제약조건)
CREATE CONSTRAINT concept_name_unique FOR (c:Concept) REQUIRE (c.workspaceId, c.name) IS UNIQUE;
CREATE CONSTRAINT entity_name_unique FOR (e:Entity) REQUIRE (e.workspaceId, e.name) IS UNIQUE;
-- 검색 성능
CREATE INDEX source_workspace_idx FOR (s:Source) ON (s.workspaceId, s.source_id);
CREATE INDEX concept_path_workspace_idx FOR (c:Concept) ON (c.workspaceId, c.page_path);

-- 벡터 인덱스 (의미론적 검색 / 오타, 유의어 방어용)
CREATE VECTOR INDEX concept_embedding_idx IF NOT EXISTS FOR (c:Concept) ON (c.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}};
```

### 2.4 Cypher 예제 (Ingest 시 사용)
```cypher
// 새로운 Concept 노드 생성 및 Source와 연결 (멀티 테넌시 적용)
MERGE (c:Concept {workspaceId: $workspaceId, name: $conceptName})
ON CREATE SET c.summary = $summary, c.page_path = $pagePath
WITH c
MATCH (s:Source {workspaceId: $workspaceId, source_id: $sourceId})
MERGE (s)-[:MENTIONS {weight: $weight}]->(c);
```
