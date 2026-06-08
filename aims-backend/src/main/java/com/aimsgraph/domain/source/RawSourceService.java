package com.aimsgraph.domain.source;

import com.aimsgraph.outbox.OutboxEvent;
import com.aimsgraph.outbox.OutboxEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RawSourceService {
    
    private final RawSourceMapper rawSourceMapper;
    private final OutboxEventMapper outboxEventMapper;

    @Transactional
    public void ingestSource(RawSource source) {
        // 1. Save RawSource
        rawSourceMapper.insert(source);
        
        // 2. Save OutboxEvent
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID().toString());
        event.setWorkspaceId(source.getWorkspaceId());
        event.setAggregateType("DOCUMENT");
        event.setAggregateId(String.valueOf(source.getId()));
        event.setEventType("CREATED");
        event.setPayload(String.format("{\"sourceUri\": \"%s\", \"title\": \"%s\"}", source.getSourceUri(), source.getTitle()));
        event.setStatus("PENDING");
        
        outboxEventMapper.insert(event);
    }
}
