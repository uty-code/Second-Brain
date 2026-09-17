package com.aimsgraph.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimsgraph.testcontainers.AbstractContainerBaseTest;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OutboxPollerTest extends AbstractContainerBaseTest {

  @Autowired private OutboxPoller outboxPoller;

  @Autowired private OutboxEventMapper outboxEventMapper;

  @Test
  void shouldPublishPendingEventsAndMarkAsProcessed() throws Exception {
    // given
    OutboxEvent event = new OutboxEvent();
    event.setId(UUID.randomUUID().toString());
    event.setWorkspaceId("ws-1");
    event.setAggregateType("DOCUMENT");
    event.setAggregateId("100");
    event.setEventType("CREATED");
    event.setPayload("{\"test\":\"data\"}");
    event.setStatus("PENDING");
    outboxEventMapper.insert(event);

    // when
    outboxPoller.pollOutboxEvents();

    // then
    var events = outboxEventMapper.findByWorkspaceId("ws-1");
    assertThat(events.get(0).getStatus()).isEqualTo("PROCESSED");

    // Verify Kafka message
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
      consumer.subscribe(Collections.singletonList("aims.outbox.events"));
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
      assertThat(records.count()).isGreaterThanOrEqualTo(1);

      boolean found = false;
      for (ConsumerRecord<String, String> record : records) {
        if (record.key().equals("ws-1")) {
          found = true;
          assertThat(record.value()).contains("DOCUMENT");
        }
      }
      assertThat(found).isTrue();
    }
  }
}
