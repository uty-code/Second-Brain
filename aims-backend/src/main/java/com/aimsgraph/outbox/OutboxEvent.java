package com.aimsgraph.outbox;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OutboxEvent {
    private String id;
    private String workspaceId;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String payload;
    private String status;
    private LocalDateTime createdAt;
}
