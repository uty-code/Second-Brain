# API 통신 패러다임 비교: REST, gRPC, GraphQL

서로 다른 네트워크 환경과 요구사항에 따라 최적의 API 기술이 달라집니다.
- **REST (Representational State Transfer)**: HTTP 표준 메서드(GET, POST, PUT, DELETE)와 URL 리소스를 매핑하여 클라이언트와 통신합니다. 직관적이고 전 세계 브라우저 생태계에서 가장 폭넓게 호환됩니다.
- **gRPC**: HTTP/2 기반 위에서 프로토콜 버퍼(Protocol Buffers)를 사용하는 바이너리 직렬화 RPC 프레임워크입니다. 극도로 높은 전송 속도와 저지연(Low Latency)을 제공하여 내부 마이크로서비스 간 통신에 최적화되어 있습니다.
- **GraphQL**: 클라이언트가 단일 엔드포인트에 쿼리를 보내 자신이 필요한 정확한 필드만 요청할 수 있는 질의 언어입니다. REST의 고질적인 오버페칭(Over-fetching)과 언더페칭(Under-fetching) 문제를 해결하여 복잡한 프론트엔드 모바일 앱에 주로 채택됩니다.
