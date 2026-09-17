package com.aimsgraph.outbox;

import java.time.LocalDateTime;
import lombok.Data;

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
