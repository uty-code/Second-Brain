package com.aimsgraph.outbox;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxEventMapper {
  void insert(OutboxEvent outboxEvent);

  List<OutboxEvent> findByWorkspaceId(String workspaceId);

  List<OutboxEvent> findPendingEvents();

  void updateStatus(String id, String status);
}
