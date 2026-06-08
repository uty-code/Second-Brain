package com.aimsgraph.outbox;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class OutboxPollerTest {

    @Container
    static MSSQLServerContainer<?> mssql = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
            .acceptLicense();

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mssql::getJdbcUrl);
        registry.add("spring.datasource.username", mssql::getUsername);
        registry.add("spring.datasource.password", mssql::getPassword);
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private OutboxEventMapper outboxEventMapper;

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
