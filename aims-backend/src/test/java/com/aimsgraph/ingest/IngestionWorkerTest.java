package com.aimsgraph.ingest;

import com.aimsgraph.outbox.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionWorkerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Neo4jClient neo4jClient;

    @Mock
    private RLock indexLock;

    @Mock
    private RLock logLock;

    @Mock
    private LlmService llmService;

    private IngestionWorker ingestionWorker;

    @BeforeEach
    void setUp() {
        ingestionWorker = new IngestionWorker(objectMapper, redissonClient, neo4jClient, llmService);
    }

    @Test
    void testProcessEvent() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID().toString());
        event.setWorkspaceId("tenant-1");
        event.setEventType("RAW_SOURCE_ADDED");
        event.setPayload("{\"content\": \"test data\"}");
        event.setCreatedAt(LocalDateTime.now());

        String message = "{\"id\":\"" + event.getId() + "\"}";

        when(objectMapper.readValue(message, OutboxEvent.class)).thenReturn(event);
        
        when(redissonClient.getLock("wiki:tenant-1:index.md")).thenReturn(indexLock);
        when(redissonClient.getLock("wiki:tenant-1:log.md")).thenReturn(logLock);
        
        when(indexLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(logLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        ExtractedConcept mockConcept = new ExtractedConcept();
        mockConcept.setName("mock-concept-1");
        mockConcept.setTitle("Mock Concept");
        mockConcept.setType("concept");
        mockConcept.setSummary("This is a mock summary");
        mockConcept.setContent("This is mock content referencing [[mock-concept-2]]");
        LinkedConcept lc = new LinkedConcept();
        lc.setName("mock-concept-2");
        lc.setType("RELATES_TO");
        mockConcept.setLinkedConcepts(List.of(lc));

        when(llmService.extractKnowledge(anyString(), anyString(), anyString())).thenReturn(List.of(mockConcept));

        ingestionWorker.processEvent(message);

        verify(objectMapper, times(1)).readValue(message, OutboxEvent.class);
        verify(indexLock, times(1)).tryLock(10, 10, TimeUnit.SECONDS);
        verify(logLock, times(1)).tryLock(10, 10, TimeUnit.SECONDS);
        verify(neo4jClient, atLeastOnce()).query(anyString());
    }
}
