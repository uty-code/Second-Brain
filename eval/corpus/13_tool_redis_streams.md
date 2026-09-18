# Redis Streams 자료구조 소개

Redis 5.0에서 도입된 Redis Streams는 추가 전용 로그(Append-Only Log) 형태의 고성능 메시징 자료구조입니다.
Kafka의 핵심 개념인 Consumer Group, 메시지 ID 기반의 오프셋 관리, 재처리(PEL, Pending Entries List) 메커니즘을 인메모리 환경에서 경량으로 제공합니다.
`XADD` 명령어로 메시지를 스트림에 추가하고, `XREADGROUP`으로 여러 컨슈머가 메시지를 분산 처리하며, 처리가 끝나면 `XACK` 명령어로 명시적 수신을 확인합니다.
가벼운 이벤트 스트리밍이나 작업 큐(Job Queue)가 필요할 때 별도의 카프카 클러스터 없이도 강력한 대안이 됩니다.
