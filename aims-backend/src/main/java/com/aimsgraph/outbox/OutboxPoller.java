package com.aimsgraph.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventMapper outboxEventMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void pollOutboxEvents() {
        List<OutboxEvent> events = outboxEventMapper.findPendingEvents();
        for (OutboxEvent event : events) {
            try {
                String message = objectMapper.writeValueAsString(event);
                kafkaTemplate.send("aims.outbox.events", event.getWorkspaceId(), message)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                outboxEventMapper.updateStatus(event.getId(), "PROCESSED");
                            } else {
                                log.error("Failed to send event {}", event.getId(), ex);
                                outboxEventMapper.updateStatus(event.getId(), "FAILED");
                            }
                        });
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize event {}", event.getId(), e);
                outboxEventMapper.updateStatus(event.getId(), "FAILED");
            }
        }
    }
}
