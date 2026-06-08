package com.aimsgraph.outbox;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.UUID;

@Mapper
public interface OutboxEventMapper {
    void insert(OutboxEvent outboxEvent);
    List<OutboxEvent> findByWorkspaceId(String workspaceId);
    List<OutboxEvent> findPendingEvents();
    void updateStatus(String id, String status);
}
