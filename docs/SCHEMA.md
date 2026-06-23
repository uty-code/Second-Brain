# Database & Graph Schema Specification: AIMS-Graph

## 1. 관계형 데이터베이스 스키마 (MSSQL)

AIMS-Graph는 RDBMS로서 MSSQL을 사용하며, 아래 테이블 구조를 통해 사용자 계정, 격리된 워크스페이스 메타데이터, 파일 변동 사항(Outbox)을 관리합니다.

### 1.1 `Users` 테이블 (B2B 사용자 계정)
- **`id`** (`BIGINT`, `IDENTITY(1,1)`, `PRIMARY KEY`): 고유 식별자
- **`username`** (`VARCHAR(100)`, `UNIQUE`, `NOT_NULL`): 사용자 로그인 ID
- **`password_hash`** (`VARCHAR(255)`, `NOT_NULL`): BCrypt 해싱된 패스워드
- **`default_workspace_id`** (`VARCHAR(50)`): 가입 시 자동 생성되는 디폴트 워크스페이스 ID (`ws-[username]`)
- **`created_at`** (`DATETIME2`, `DEFAULT GETUTCDATE()`): 가입 일시

### 1.2 `Workspace` 테이블 (워크스페이스 설정)
- **`id`** (`VARCHAR(50)`, `PRIMARY KEY`): 워크스페이스 고유 ID (`ws-[username]` 또는 `[username]_[suffix]`)
- **`name`** (`NVARCHAR(100)`): 워크스페이스(금고) 한글/영문 표시 명칭
- **`created_at`** (`DATETIME2`, `DEFAULT GETUTCDATE()`): 생성 일시

### 1.3 `WorkspaceCredentials` 테이블 (BYOK 암호화 API 자격증명)
- **`workspace_id`** (`VARCHAR(50)`, `PRIMARY KEY`): 연동 대상 워크스페이스 ID
- **`notion_api_key`** (`VARCHAR(MAX)`): AES-256 암호화된 Notion Integration 토큰
- **`github_api_key`** (`VARCHAR(MAX)`): AES-256 암호화된 GitHub API 토큰
- **`deepseek_api_key`** (`VARCHAR(MAX)`): AES-256 암호화된 DeepSeek API 토큰
- **`updated_at`** (`DATETIME2`, `DEFAULT GETUTCDATE()`): 최종 갱신 일시

### 1.4 `RawSource` 테이블 (수집된 원본 소스)
- **`id`** (`BIGINT`, `IDENTITY(1,1)`, `PRIMARY KEY`): 고유 식별자
- **`workspace_id`** (`VARCHAR(50)`, `INDEX`): 소속 워크스페이스 ID
- **`source_uri`** (`VARCHAR(500)`): 파일/서비스 실제 위치 정보
- **`title`** (`VARCHAR(500)`): 원본 문서 타이틀
- **`content_hash`** (`VARCHAR(64)`): 본문 내용의 SHA-256 해시값 (중복 수집 방지)
- **`source_type`** (`VARCHAR(20)`): 소스 출처 유형 (`MARKDOWN`, `NOTION`, `SLACK` 등)
- **`status`** (`VARCHAR(20)`): 파이프라인 처리 상태 (`RECEIVED`, `INGESTED`, `FAILED`)
- **`created_at`** (`DATETIME2`, `DEFAULT GETUTCDATE()`): 최초 수집 시간
- **`updated_at`** (`DATETIME2`): 처리 완료 및 갱신 일시

### 1.5 `WikiPage` 테이블 (마크다운 위키 페이지 메타데이터)
- **`id`** (`BIGINT`, `IDENTITY(1,1)`, `PRIMARY KEY`): 고유 식별자
- **`workspace_id`** (`VARCHAR(50)`, `INDEX`): 소속 워크스페이스 ID
- **`page_path`** (`VARCHAR(500)`): 파일 시스템 내의 마크다운 상대 경로 (예: `wiki/concepts/harness-framework.md`)
- **`title`** (`NVARCHAR(200)`): 위키 페이지 한글/영문 제목
- **`page_type`** (`VARCHAR(20)`): 페이지 성격 (`CONCEPT`, `ENTITY`, `INSIGHT`, `INDEX`, `LOG`)
- **`content_hash`** (`VARCHAR(64)`): 위키 파일의 해시값 (정합성 린팅용)
- **`created_at`** (`DATETIME2`, `DEFAULT GETUTCDATE()`): 최초 생성 일시
- **`updated_at`** (`DATETIME2`): 최종 수정 일시
- **`CONSTRAINT UQ_WikiPage_Workspace_Path`** (`UNIQUE`): `workspace_id` + `page_path` 복합 유니크 제약

### 1.6 `OutboxEvent` 테이블 (Transactional Outbox)
- **`id`** (`UNIQUEIDENTIFIER`, `PRIMARY KEY`): 이벤트 고유 UUID
- **`workspace_id`** (`VARCHAR(50)`, `INDEX`): 이벤트 발행처 워크스페이스 ID
- **`aggregate_type`** (`VARCHAR(50)`): 이벤트 대상 데이터 유형 (예: `DOCUMENT`, `CONCEPT`)
- **`aggregate_id`** (`VARCHAR(255)`): 대상 ID
- **`event_type`** (`VARCHAR(50)`): 이벤트 유형 (예: `CREATED`, `UPDATED`)
- **`payload`** (`NVARCHAR(MAX)`): JSON 형식의 상세 페이로드 데이터
- **`status`** (`VARCHAR(20)`): Kafka 메시지 발행 상태 (`PENDING`, `PROCESSED`, `FAILED`)
- **`created_at`** (`DATETIME2`, `DEFAULT GETUTCDATE()`): 이벤트 발생 시간

---

## 2. 지식 그래프 온톨로지 스키마 (Neo4j)

AIMS-Graph는 순수 그래프 탐색(Pure Agentic Graph Traversal) 아키텍처를 위해 RAG(벡터 임베딩/유사도 색인) 관련 데이터를 완전히 배제하며, 오직 명시적인 노드와 논리 관계망만으로 설계됩니다.

### 2.1 노드 레이블 및 속성 (Node Labels & Properties)

| 레이블 | 설명 | 주요 속성 |
|---|---|---|
| `:Source` | 수집된 원본 소스 정보 (MSSQL RawSource 데이터와 동기화) | `workspaceId` (String), `source_id` (Long), `title` (String), `uri` (String), `ingested_at` (DateTime) |
| `:Concept` | 위키 내 핵심 지식 개념/토픽 | `workspaceId` (String), `name` (String), `summary` (String), `page_path` (String) |
| `:Entity` | 구체적인 실체, 인물, 기관, 기술 스택명 | `workspaceId` (String), `name` (String), `type` (String: `PERSON`/`ORG`/`TECH`), `page_path` (String) |

*(참고: RAG 전면 금지 원칙에 맞추어 기존에 과도한 설계로서 존재하던 `embedding` 속성은 노드 속성 및 스키마 명세에서 완전히 삭제되었습니다.)*

### 2.2 관계 유형 및 속성 (Relationships & Properties)

| 관계 유형 | 방향 | 설명 | 속성 |
|---|---|---|---|
| `MENTIONS` | `(Source)->(Concept)` | 원본 문서가 특정 지식 개념을 직접 언급함 | `weight` (Float, 빈도 가중치) |
| `RELATES_TO` | `(Concept)->(Concept)` | 두 개념 간의 보편적인 연관 관계 | `context` (String, 관계 사유/맥락) |
| `EXTENDS` | `(Concept)->(Concept)` | 특정 개념이 다른 개념의 지식을 확장/발전시킴 | `context` (String) |
| `CONTRADICTS` | `(Concept)->(Concept)` | 두 개념의 주장이 서로 대치되거나 모순됨 | `reason` (String) |
| `DEPENDS_ON` | `(Concept)->(Concept)` | 해당 지식을 이해하기 위해 선행 개념의 습득이 요구됨 | `context` (String) |
| `EXPLAINS` | `(Concept)->(Concept)` | 한 개념이 다른 개념의 원리나 배경을 구체적으로 설명함 | `context` (String) |
| `DERIVED_FROM` | `(Concept)->(Source)` | 개념이 최초 추출된 원본 문서 소스 매핑 | `extracted_at` (DateTime) |

### 2.3 제약 조건 및 인덱스 (Constraints & Indexes)
```cypher
// 멀티테넌시 데이터 격리를 위한 복합 고유 제약 조건 (workspaceId + name)
CREATE CONSTRAINT concept_name_unique FOR (c:Concept) REQUIRE (c.workspaceId, c.name) IS UNIQUE;
CREATE CONSTRAINT entity_name_unique FOR (e:Entity) REQUIRE (e.workspaceId, e.name) IS UNIQUE;

// 고속 검색 조회를 위한 성능 인덱스
CREATE INDEX source_workspace_idx FOR (s:Source) ON (s.workspaceId, s.source_id);
CREATE INDEX concept_path_workspace_idx FOR (c:Concept) ON (c.workspaceId, c.page_path);
```

### 2.4 그래프 갱신 예제 Cypher 쿼리 (MyBatis/Java 연동)
```cypher
// 신규 개념(Concept) 노드를 안전하게 생성하고 멘션 관계를 연결하는 쿼리 (멀티테넌시 workspaceId 파라미터 적용)
MERGE (c:Concept {workspaceId: $workspaceId, name: $conceptName})
ON CREATE SET c.summary = $summary, c.page_path = $pagePath
WITH c
MATCH (s:Source {workspaceId: $workspaceId, source_id: $sourceId})
MERGE (s)-[:MENTIONS {weight: $weight}]->(c);
```
