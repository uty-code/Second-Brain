# Product Requirements Document (PRD): AIMS-Graph

## 1. 프로젝트 개요
**AIMS-Graph (Agentic Information Management System)**는 기존 단순 RAG(검색 증강 생성)의 환각 및 맥락 한계를 극복하기 위해 설계된 **LLM 위키(LLM Wiki - Second Brain)** 철학의 기업용 지식 관리 솔루션입니다.
질문을 받을 때마다 원본 문서들로부터 파편화된 정보를 단발적으로 조립하는 일반 RAG와 달리, AIMS-Graph는 **'지속적으로 정제하고 누적하여 복리로 증식하는 위키(Persistent, Compounding Knowledge Base)'**를 핵심 가치로 둡니다.
사용자는 지식 수집과 질문에 집중하고, AI 에이전트는 문서 요약, 교차 참조 링크 생성, 카탈로그 및 연대기 정리 등의 모든 무거운 유지보수 작업을 백그라운드에서 자동으로 수행합니다.

## 2. 주요 차별점 (Core Value Proposition)
- **지속성 지식 컴파일 (Persistent Knowledge Compilation)**: 매번 원본 지식을 일회성으로 재탐색하지 않고, 신규 소스 인입 시 기존 지식 그래프와 마크다운 위키 파일망에 이를 영속적으로 병합 업데이트합니다.
- **역할 분리 (Source vs Compiled Wiki)**: 인간이 제어하는 원본 데이터 소스 영역과 AI가 100% 설계하고 큐레이션하는 컴파일된 지식 위키 영역을 완전히 격리하여 데이터 무결성을 보장합니다.
- **에이전틱 그래프 순회 (Agentic Graph Traversal - NO RAG)**: 유사도 기반 벡터 검색(Vector Search)의 사용을 전면 배제합니다. 대신, LangChain4j AiServices 및 Tool Calling 기술을 통해 AI 에이전트가 위키 및 Neo4j 지식 그래프의 논리적 엣지(Edge - EXTENDS, DEPENDS_ON 등)를 직접 종단/횡단하면서 사람과 유사한 인과 추론 답변을 도출합니다.
- **실시간 탐색 시각화 (AI Gaze Tracking)**: AI가 그래프를 자율 탐색하는 과정을 브라우저 상에 에메랄드 그린 컬러 및 글로우 링 형태로 실시간 시각화하여 지식 검색 및 추론 과정의 투명성을 극대화합니다.
- **이중 저장소 동기화 (Dual-Storage Consistency)**: 영속적인 마크다운 위키 파일들과 고속 관계 탐색용 지식 그래프(Neo4j) 간의 완벽한 델타 동기화를 보장합니다.
- **사용자별 테넌트 격리 및 삭제 보장**: B2B 환경에서 사용자별 워크스페이스(Vault) 디렉토리 및 DB 자격증명을 완전 격리하며, 계정 탈퇴 시 잔여 데이터 찌꺼기를 포함한 논리적/물리적 전체 리소스를 영구 파괴적으로 소멸시키는 완전 청소를 보장합니다.

## 3. SaaS 연동 비전 및 사용자 시나리오 (B2B/B2C)
본 시스템은 다양한 외부 클라이언트 및 플랫폼과 유기적으로 작동하도록 REST API 및 Model Context Protocol(MCP)을 제공합니다.
- **외부 AI 모델과의 직접 연동**:
  - **Claude Desktop / Cursor**: Spring AI 기반의 공식 MCP(Model Context Protocol) SSE 브릿지를 통해 외부 개발 도구 및 데스크톱 에이전트가 사용자의 위키 지식을 탐색할 수 있도록 지원합니다.
  - **SaaS 플랫폼 Actions 연동**: REST API 규약을 통해 타 서비스와 손쉽게 지식을 수집하고 검색할 수 있습니다.
- **'금고(Vault)' 기반 멀티테넌시 및 접근 제어**:
  - 사용자는 목적에 맞게 여러 개의 지식창고(Vault = Workspace)를 개설할 수 있습니다.
  - 사용자 계정별로 생성된 세션 토큰(JWT)과 사용자 정보에 따라 격리된 전용 디렉토리(`ws-[username]`, `[username]_[suffix]`) 내에서만 작업이 처리되어 완벽한 B2B 격리 수준을 제공합니다.
- **BYOK (Bring Your Own Key) 보안 고도화**:
  - 외부 연동(Notion, GitHub 등)에 필요한 토큰 정보를 로컬 브라우저에 임시 보관하지 않고, 백엔드 데이터베이스에 **AES-256 알고리즘으로 암호화**하여 영속화합니다.
  - 필요할 때만 복호화하여 API 호출에 사용하므로 사용자의 개인 키 유출 리스크를 근본적으로 차단합니다.

## 4. 로드맵 및 개발 페이즈 (Phased Roadmap)
- **Phase 1 (기반 구축 - MVP)**: 단일 사용자 기준의 마크다운 수집 및 Neo4j 기본 그래프 적재 기능 구현.
- **Phase 2 (LLM 위키 설계 및 RAG 폐기)**: 사후 벡터 검색(RAG) 기술을 전면 폐기하고, 마크다운 위키 델타 컴파일 및 Neo4j 엣지 튜닝 체계 도입.
- **Phase 3 (분산 일관성 및 MCP)**: Kafka 아웃박스 패턴 적용, Redis 분산 락, 공식 Spring AI MCP 서버 마이그레이션.
- **Phase 4 (B2B 고도화 및 보안 - 현 단계)**: JWT 기반 로그인/로그아웃 블랙리스팅, 계정 영구 탈퇴에 따른 논리적/물리적 리소스 완전 소거 정책 구현, 워크스페이스 멀티테넌트 격리 강화, 실시간 SSE 기반 AI Gaze Tracking 하이라이팅 구현 완료.

## 5. 제품 스코프 내 완료 기능 및 제외 사항
- **포함 사항 (완료된 코어 기능)**:
  - JWT 및 Spring Security를 포함하는 B2B 멀티유저 로그인 가드.
  - Redis 블랙리스트를 통한 로그아웃 토큰 완전 무효화.
  - 회원 영구 탈퇴 시 Neo4j, 물리 파일 폴더, DB 계정 정보를 일괄 영구 소멸시키는 동기화 청소 프로세스.
  - LangChain4j 에이전틱 그래프 Traversal 및 자율 지식 수집 도구.
  - 실시간 SSE 알림 기반 그래프 노드 하이라이팅(AI Gaze Tracking) 및 로딩 스피너 UI.
  - AES-256 DB 암호화 기반 BYOK 자격증명 관리 시스템.
  - REST API 및 공식 Spring AI MCP SSE 서버 연동 규약 준수.
- **제외 사항 (Out of Scope)**:
  - 텍스트/마크다운 이외의 동영상, 이미지 등의 대용량 미디어에 대한 직접 지식 마이닝 및 변환.
  - 멀티 언어(영어/한국어 이외의 제3국어) 간의 동적 번역 온톨로지 주입.
