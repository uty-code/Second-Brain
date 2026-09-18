# Redisson을 활용한 분산 락(Distributed Lock) 구현

다중 서버 환경에서 단일 JVM의 `synchronized`나 `ReentrantLock`은 동시성을 방어하지 못합니다. Redisson 라이브러리는 Redis를 기반으로 분산 락을 손쉽게 구현할 수 있도록 지원합니다.

```java
package com.example.lock;

import java.util.concurrent.TimeUnit;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

  private final RedissonClient redissonClient;

  public TicketService(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  public void issueTicket(String ticketId) {
    RLock lock = redissonClient.getLock("lock:ticket:" + ticketId);
    try {
      // 락 획득 시도: 최대 5초 대기, 획득 후 3초 경과 시 자동 해제
      boolean isLocked = lock.tryLock(5, 3, TimeUnit.SECONDS);
      if (isLocked) {
        try {
          // 비즈니스 로직 수행 (티켓 발급 및 재고 차감)
        } finally {
          lock.unlock();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
```

Redisson은 내부적으로 Watchdog 메커니즘을 통해 락의 타임아웃을 안전하게 연장하며, Spin Lock 대신 Pub/Sub 기반으로 동작하여 Redis 서버에 과도한 부하를 주지 않습니다.
