---
id: http2-protocol
title: HTTP/2 프로토콜
summary: HTTP/2는 단일 TCP 연결에서 멀티플렉싱과 HPACK 헤더 압축을 제공하는 표준 통신 규약입니다.
type: concept
tags: [network, http2, protocol]
aliases: [HTTP2]
provenance: [rfc-7540-notes]
---

## 정의
HTTP/2는 기존 HTTP/1.1의 성능 한계를 극복하기 위해 설계된 바이너리 프레이밍 기반 전송 프로토콜입니다.

## 핵심 구성 요소
- 멀티플렉싱(Multiplexing): 하나의 TCP 연결 내에서 여러 요청과 응답 프레임을 동시에 인터리빙(interleaving)하여 전송합니다.
- HPACK 헤더 압축: 중복되는 HTTP 헤더 정보를 정적/동적 허프만 코딩 테이블로 압축하여 오버헤드를 대폭 절감합니다.

## 동작 원리
모든 메시지는 바이너리 형태의 프레임으로 쪼개지며, 각 프레임에는 스트림 ID가 부여되어 단일 소켓에서 혼선 없이 재조립됩니다.

## 장점과 트레이드오프
- 단일 연결 재사용으로 TCP 핸드셰이크 지연을 최소화합니다.
- TCP 계층 자체의 패킷 유실 시 전체 스트림이 지연되는 TCP HOL 블로킹 한계는 여전히 존재합니다.

## 실전 적용 사례
현대 웹 브라우저와 리버스 프록시 간의 기본 통신 규격으로 채택되어 있습니다.

## 관련 개념들
- [[network-optimization]]
