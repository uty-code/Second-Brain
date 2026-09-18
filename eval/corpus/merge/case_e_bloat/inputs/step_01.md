## 메모: Redisson 라이브러리 적용
Redis 분산 락 구현 시 직접 SETNX를 폴링(스핀락)하면 Redis 부하가 가중됩니다. 대신 pub/sub 기반으로 락 획득 대기를 처리하는 Redisson 클라이언트를 사용하는 것이 권장됩니다.
