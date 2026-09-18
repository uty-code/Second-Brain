---
id: http2-protocol
title: HTTP/2 프로토콜
summary: HTTP/2는 서버 푸시와 스트림 우선순위 지정을 통해 웹 리소스 전달을 최적화합니다.
type: concept
tags: [http2, server-push, stream-priority]
aliases: [HTTP2 프로토콜]
provenance: [web-perf-deepdive-2025]
---

## 정의
HTTP/2는 클라이언트 요청 없이도 선제적으로 리소스를 전송하고 스트림 간의 우선순위를 제어하는 고급 전송 기능을 갖추고 있습니다.

## 핵심 구성 요소
- 서버 푸시(Server Push): 클라이언트가 HTML을 요청했을 때, 필요한 CSS/JS 리소스를 서버가 사전에 감지하여 푸시합니다.
- 스트림 우선순위 지정(Stream Prioritization): 렌더링에 핵심적인 리소스 스트림에 가중치(Weight)와 의존성을 부여하여 우선 전송합니다.
- HTTP 수준의 HOL(Head-of-Line) 블로킹 해결: 요청이 앞선 요청의 응답 완료를 기다릴 필요 없이 독립적으로 처리됩니다.

## 동작 원리
서버는 PUSH_PROMISE 프레임을 클라이언트에 전송하여 푸시할 리소스의 스트림을 예고한 후 본 데이터를 병렬로 전송합니다.

## 장점과 트레이드오프
- 초기 웹페이지 로딩 속도(FCP)가 크게 향상됩니다.
- 서버 푸시가 브라우저 캐시 상태를 모를 경우 불필요한 대역폭 낭비가 발생할 수 있습니다.

## 실전 적용 사례
정적 에셋이 많은 전자상거래 프론트엔드 최적화에 적용됩니다.

## 관련 개념들
- [[web-performance-tuning]]
