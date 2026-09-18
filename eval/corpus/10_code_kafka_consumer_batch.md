# Spring Kafka 배치 리스너(Batch Listener) 설정 및 처리

Kafka 메시지를 건별로 처리하지 않고 대량(Batch)으로 묶어서 한 번에 소비하면 네트워크 오버헤드와 DB 쓰기 I/O를 크게 절약할 수 있습니다.

```java
package com.example.kafka;

import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderBatchConsumer {

  @KafkaListener(
      topics = "order-events",
      groupId = "order-group",
      containerFactory = "batchKafkaListenerContainerFactory")
  public void consumeBatch(
      List<ConsumerRecord<String, String>> records,
      Acknowledgment ack) {
    try {
      for (ConsumerRecord<String, String> record : records) {
        processOrder(record.key(), record.value());
      }
      // 배치 내의 모든 메시지 처리가 완료된 후 수동 커밋
      ack.acknowledge();
    } catch (Exception e) {
      // 오류 발생 시 재시도 또는 DLQ 전송 로직 수행
    }
  }

  private void processOrder(String key, String payload) {
    // 비즈니스 로직 처리
  }
}
```

- `containerFactory`를 배치 전용 팩토리로 지정하여 여러 개의 `ConsumerRecord`를 `List` 형태로 수신합니다.
- `Acknowledgment.acknowledge()`를 호출하여 수동 커밋(Manual Commit) 모드로 안전하게 오프셋을 전진시킵니다.
