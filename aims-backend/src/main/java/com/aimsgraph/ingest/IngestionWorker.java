package com.aimsgraph.ingest;

import com.aimsgraph.outbox.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.aimsgraph.api.NotificationController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionWorker {

    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    private final Neo4jClient neo4jClient;
    private final LlmService llmService;

    @KafkaListener(topics = "aims.outbox.events", groupId = "aims-group")
    public void processEvent(String message) {
        log.info("Received message: {}", message);
        try {
            OutboxEvent event = objectMapper.readValue(message, OutboxEvent.class);
            
            String tenantId = event.getWorkspaceId();
            log.info("Calling LLM Service for tenant: {}", tenantId);
            
            String contentToProcess = event.getPayload() != null ? event.getPayload().toString() : "Sample document content for event " + event.getId();
            List<ExtractedConcept> concepts = llmService.extractKnowledge(event.getId() != null ? event.getId().toString() : "", contentToProcess, tenantId);
            
            // 1. 병렬 위키 생성 호출 (여기서 블로킹되어 모든 .md 파일이 생성될 때까지 대기)
            java.util.List<java.util.Map<String, Object>> nodes = new java.util.ArrayList<>();
            for (ExtractedConcept c : concepts) {
                if (c.getName() != null && !c.getName().isBlank()) {
                    nodes.add(java.util.Map.of(
                        "id", c.getName(),
                        "name", c.getTitle() != null ? c.getTitle() : c.getName(),
                        "type", c.getType() != null ? c.getType() : "concept",
                        "summary", c.getSummary() != null ? c.getSummary() : ""
                    ));
                }
            }
            if (!nodes.isEmpty()) {
                llmService.generateWikiPages(java.util.Map.of("nodes", nodes), contentToProcess, tenantId, "gpt-4o-mini");
            }
            
            RLock indexLock = redissonClient.getLock("wiki:" + tenantId + ":index.md");
            RLock logLock = redissonClient.getLock("wiki:" + tenantId + ":log.md");
            
            boolean indexLocked = false;
            boolean logLocked = false;
            
            try {
                indexLocked = indexLock.tryLock(10, 10, TimeUnit.SECONDS);
                logLocked = logLock.tryLock(10, 10, TimeUnit.SECONDS);
                
                if (!indexLocked || !logLocked) {
                    throw new IllegalStateException("Failed to acquire locks for wiki files. Deferring processing.");
                }
                updateWikiIndices(event, concepts, tenantId);
            } finally {
                if (indexLocked) indexLock.unlock();
                if (logLocked) logLock.unlock();
            }

            updateNeo4j(concepts, tenantId);
            
            // Broadcast completion
            NotificationController.broadcastNotification(tenantId, "ingest_completed", "Ingest completed successfully.");

        } catch (Exception e) {
            log.error("Error processing event", e);
            throw new RuntimeException("Failed to process event", e);
        }
    }

    private void updateWikiIndices(OutboxEvent event, List<ExtractedConcept> concepts, String tenantId) throws IOException {
        Path wikiDir = Paths.get("workspaces", tenantId, "wiki");
        if (!Files.exists(wikiDir)) {
            Files.createDirectories(wikiDir);
            Files.createDirectories(wikiDir.resolve("concepts"));
            Files.createDirectories(wikiDir.resolve("entities"));
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        StringBuilder logEntries = new StringBuilder();
        StringBuilder indexEntries = new StringBuilder();

        for (ExtractedConcept concept : concepts) {
            String slug = concept.getName();
            if (slug == null || slug.isBlank()) continue;
            
            logEntries.append(String.format("- [%s] [INGEST] [[%s]]: %s\n", timestamp, slug, concept.getSummary()));
            indexEntries.append(String.format("- [[%s]]: %s\n", slug, concept.getSummary()));
        }

        Path logFile = wikiDir.resolve("log.md");
        if (!Files.exists(logFile)) {
            Files.writeString(logFile, logEntries.toString(), StandardOpenOption.CREATE);
        } else {
            Path tempFile = Files.createTempFile("log-temp", ".md");
            Files.writeString(tempFile, logEntries.toString(), StandardOpenOption.CREATE);
            try (java.io.InputStream in = Files.newInputStream(logFile);
                 java.io.OutputStream out = Files.newOutputStream(tempFile, StandardOpenOption.APPEND)) {
                in.transferTo(out);
            }
            Files.move(tempFile, logFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        Path indexFile = wikiDir.resolve("index.md");
        Files.writeString(indexFile, indexEntries.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private void updateNeo4j(List<ExtractedConcept> concepts, String tenantId) {
        for (ExtractedConcept concept : concepts) {
            if (concept.getName() == null || concept.getName().isBlank()) continue;
            
            String createNodeCypher = "MERGE (c:Concept {name: $name, workspaceId: $workspaceId}) " +
                    "ON CREATE SET c.title = $title, c.createdAt = datetime() " +
                    "ON MATCH SET c.title = $title, c.updatedAt = datetime()";
            neo4jClient.query(createNodeCypher)
                       .bind(concept.getName()).to("name")
                       .bind(tenantId).to("workspaceId")
                       .bind(concept.getTitle()).to("title")
                       .run();

            if (concept.getLinkedConcepts() != null) {
                for (LinkedConcept link : concept.getLinkedConcepts()) {
                    if (link == null || link.getName() == null || link.getName().isBlank()) continue;
                    
                    String relType = link.getType() != null ? link.getType().toUpperCase() : "RELATES_TO";
                    // Sanitize to prevent Cypher injection
                    relType = relType.replaceAll("[^A-Z_]", "");
                    if (relType.isEmpty()) relType = "RELATES_TO";
                    
                    String linkCypher = "MERGE (c:Concept {name: $name, workspaceId: $workspaceId}) " +
                                        "MERGE (l:Concept {name: $link, workspaceId: $workspaceId}) " +
                                        "MERGE (c)-[:" + relType + "]->(l)";
                    neo4jClient.query(linkCypher)
                               .bind(concept.getName()).to("name")
                               .bind(link.getName()).to("link")
                               .bind(tenantId).to("workspaceId")
                               .run();
                }
            }
        }
    }
}
