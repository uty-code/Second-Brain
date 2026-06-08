# Product Requirements Document (PRD): AIMS-Graph

## 1. 프로젝트 개요
**AIMS-Graph (Agentic Information Management System)**는 단순한 RAG(검색 증강 생성)의 한계를 극복하기 위해 제안된 **LLM Wiki (Second Brain)** 철학을 엔터프라이즈 환경으로 확장한 백엔드 아키텍처입니다.
질문할 때마다 원본 문서에서 파편화된 정보를 새로 검색하고 조립하는 기존 RAG와 달리, AIMS-Graph는 **'지속적으로 축적되고 갱신되는 위키(Persistent, Compounding Artifact)'**를 구축합니다. 
인간은 데이터 소싱과 질문에 집중하고, 에이전트(LLM)는 요약, 교차 참조, 목차 정리 등 모든 유지보수(Bookkeeping) 작업을 백그라운드에서 수행하여 지식 베이스를 최신 상태로 유지합니다.

## 2. Value Proposition (핵심 기술적 극복 과제)
- **축적형 지식 컴파일 (Persistent Knowledge Compilation)**: RAG처럼 매번 지식을 재발견(Rediscovering)하는 대신, 새 소스가 추가될 때마다 기존 지식 그래프와 마크다운 위키에 통합하여 한 번만 컴파일하고 지속 갱신합니다.
- **역할의 분리 (Human vs LLM)**: 인간은 불변의 원본 소스(Raw sources)를 큐레이션하고 질문을 던지며, LLM은 위키(The wiki) 레이어를 전적으로 소유하고 관리합니다.
- **순수 에이전틱 그래프 탐색 (Pure Agentic Graph Traversal)**: 파편화된 원문 조각을 수학적으로 찾는 일반 RAG(Vector Search) 기술을 **전면 폐기**했습니다. 대신, 에이전트(LLM)가 자신이 직접 집필한 위키 페이지(개념 노드)에서 출발하여, 다변화된 논리적 간선(EXTENDS, CONTRADICTS 등)을 표지판 삼아 이웃 노드로 점프하며(Traverse) 추론을 이어가는 인간 지능과 가장 유사한 방식을 사용합니다.
- **분산 트랜잭션과 데이터 정합성 (Dual-Storage Consistency)**: 영속성 마크다운 위키 저장소와 초고속 조회를 위한 온톨로지 그래프 DB(Neo4j) 간의 완벽한 동기화를 보장합니다.
- **엔터프라이즈급 권한 제어 및 이벤트 주도 처리**: 노드 레벨 RBAC를 통한 동적 보안 필터링과 Kafka 기반 메시지 큐를 활용한 무거운 에이전트 태스크 격리.

## 3. SaaS 연동 비전 및 사용 시나리오 (B2C/B2B)
이 백엔드는 일반 소비자들이 기존 구독형 AI와 연동하여 사용하는 **SaaS 서비스의 코어 엔진**을 지향합니다.
- **구독형 AI 모델 완벽 지원**:
  - **ChatGPT Plus**: "Custom GPTs (Actions)" 기능을 통해 OpenAPI(REST) 기반으로 완벽 연동.
  - **Claude Pro**: "Claude Desktop 앱"의 설정 파일에 MCP 서버로 등록하여 연동 (원격 접속을 위한 MCP Remote Bridge 사용).
  - *(참고: Gemini Advanced는 웹 버전에서 사용자 정의 외부 API 연동 기능이 막혀 있어 제외)*
- **'금고(Vault)' 기반 멀티 테넌시 접근 제어**:
  - 사용자 한 명이 용도별(업무용, 개인용 등)로 여러 개의 세컨드 브레인(Vault = `workspace_id`)을 생성할 수 있습니다.
  - **1 계정 = 1 통합 API 키(MCP 연결)** 구조를 채택하되, 웹 대시보드에서 각 API 키가 접근할 수 있는 Vault 목록을 다중 매핑(RBAC)하여 유연하고 안전하게 데이터를 격리합니다.
- **BYOK (Bring Your Own Key) 기반 Ingestion 비용 구조**:
  - 가장 비용이 많이 드는 '원본 문서 분석 및 위키 컴파일(Ingestion)' 작업은 **사용자가 웹 대시보드에 직접 등록한 본인의 LLM API 키(OpenAI/Anthropic)**를 사용하여 백엔드가 대신 처리합니다.
  - 이를 통해 서버 운영자는 막대한 LLM 토큰 비용 부담 없이 서비스를 확장할 수 있으며, 추후 프리미엄 구독 모델로 손쉽게 전환/병행이 가능합니다.

## 4. 포트폴리오 스토리라인 전략 (Phased Development)
- **Phase 1 (MVP 개발)**: 단일 서버 내 로컬 마크다운 파일을 수집하여 LangChain 기반 임베딩 후 pgvector에 넣는 전형적인 RAG 베이스라인 시스템 구축.
- **Phase 2 (아키텍처 전환 - LLM Wiki 도입)**: 질문할 때마다 처음부터 문맥을 조립해야 하는 RAG의 한계를 체감. Karpathy의 LLM Wiki 패턴을 전격 도입하여 **Vector DB(RAG)를 프로젝트에서 완전히 도려내고**, 엮이고 축적되는 영구적인 마크다운 위키 + 다변화된 온톨로지를 가진 Neo4j 구조로 전환.
- **Phase 3 (엔터프라이즈 최적화 및 분산 처리)**: Ingest, Query, Lint 오퍼레이션에서 발생하는 동시성 이슈를 Redis 분산 락 및 아웃박스 패턴으로 해결. MCP 스펙 연동으로 탐색 과정 자체를 위키에 재기록(File back)하는 선순환 구조 완성.

## 5. MVP 스코프 및 제외 사항
- **포함 사항**: 텍스트/마크다운 기반의 Raw Source 수집, 에이전트 기반 지식 추출 및 위키 컴파일, 트랜잭셔널 아웃박스를 통한 Neo4j 동기화, MCP 표준 서빙. Next.js 프론트엔드는 브라우저 기반 MCP 클라이언트로 직접 동작하여 SSE 통신을 통해 그래프와 문서를 렌더링합니다.
- **제외 사항 (Out of Scope)**: 복잡한 사용자 인증/로그인 시스템. (향후 JWT+OAuth2로 확장 예정이나, MVP에서는 프론트엔드가 `.env`에 하드코딩된 더미 JWT 토큰을 삽입하여 백엔드의 단일 Vault에만 접근하도록 단순화합니다), 다중 멀티미디어(영상/음성) 프로세싱.
