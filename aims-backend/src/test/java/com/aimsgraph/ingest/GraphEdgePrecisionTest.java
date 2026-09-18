package com.aimsgraph.ingest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class GraphEdgePrecisionTest {

  @Mock private Neo4jClient neo4jClient;
  @Mock private Neo4jClient.UnboundRunnableSpec unboundRunnableSpec;
  @Mock private Neo4jClient.RunnableSpec runnableSpec;
  @Mock private Neo4jClient.OngoingBindSpec ongoingBindSpec;

  @InjectMocks private LlmService llmService;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(llmService, "wikiBaseDir", tempDir.toString());

    lenient().when(neo4jClient.query(anyString())).thenReturn(unboundRunnableSpec);
    lenient().when(unboundRunnableSpec.bind(any())).thenReturn(ongoingBindSpec);
    lenient().when(runnableSpec.bind(any())).thenReturn(ongoingBindSpec);
    lenient().when(ongoingBindSpec.to(anyString())).thenReturn(runnableSpec);
  }

  @Test
  @DisplayName("Case 01 & 05: Isolated Nodes - 무관한 도메인이나 독립 개념은 links가 비어있어도 정상 처리된다")
  void testIsolatedNodes_AcceptsEmptyLinks() {
    // Given: kubernetes-pod-scheduling and payment-pg-fee-policy without relationships
    Map<String, Object> node1 =
        Map.of("id", "kubernetes-pod-scheduling", "name", "Kubernetes Pod Scheduling");
    Map<String, Object> node2 =
        Map.of("id", "payment-pg-fee-policy", "name", "Payment PG Fee Policy");
    Map<String, Object> graphData =
        Map.of(
            "nodes", List.of(node1, node2),
            "links", Collections.emptyList());

    // When
    assertDoesNotThrow(() -> llmService.saveGraphToNeo4j(graphData, "test-ws"));

    // Then: 2 nodes are merged, 0 edge queries are executed
    verify(neo4jClient, times(2)).query(contains("MERGE (c:Concept"));
    verify(neo4jClient, never()).query(contains("-[:REFERENCES]->"));
  }

  @Test
  @DisplayName("Case 02: Text-only Mention - 본문에 타 개념이 단순 언급(contains)되어도 Edge를 생성하지 않는다")
  void testTextOnlyMention_DoesNotCreateEdge() throws IOException {
    // Given: workspace with 2 concepts: redis-distributed-lock, kubernetes-pod-scheduling
    Path conceptsDir = tempDir.resolve("test-ws").resolve("wiki").resolve("concepts");
    Files.createDirectories(conceptsDir);

    // kubernetes-pod-scheduling.md
    Files.writeString(
        conceptsDir.resolve("kubernetes-pod-scheduling.md"),
        "# Kubernetes Pod Scheduling\nK8s scheduling details.");

    // redis-distributed-lock.md mentions kubernetes in passing plain text, but NOT with [[slug]]
    // wiki link
    String redisContent =
        "# Redis Distributed Lock\n"
            + "Redis 분산 락은 여러 환경에서 사용할 수 있다.\n"
            + "kubernetes-pod-scheduling 환경에서도 배포하여 사용할 수 있다.\n";
    Files.writeString(conceptsDir.resolve("redis-distributed-lock.md"), redisContent);

    // When: syncWikiLinksToNeo4j is triggered
    llmService.syncWikiLinksToNeo4j("test-ws");

    // Then: No edge should be created because there is NO [[kubernetes-pod-scheduling]] link
    verify(neo4jClient, never()).query(contains("-[:REFERENCES]->"));
  }

  @Test
  @DisplayName("Case 03: Explicit Wiki Link - [[slug]] 형식의 명시적 링크만 정상적으로 Edge로 변환된다")
  void testExplicitWikiLink_CreatesEdge() throws IOException {
    // Given: workspace with redis-distributed-lock and redisson
    Path conceptsDir = tempDir.resolve("test-ws").resolve("wiki").resolve("concepts");
    Files.createDirectories(conceptsDir);

    Files.writeString(conceptsDir.resolve("redisson.md"), "# Redisson\nJava Redis client.");

    String redisContent = "# Redis Distributed Lock\n" + "Redis 분산 락은 [[redisson]]을 통해 구현할 수 있다.\n";
    Files.writeString(conceptsDir.resolve("redis-distributed-lock.md"), redisContent);

    // When
    llmService.syncWikiLinksToNeo4j("test-ws");

    // Then: Edge query must be executed with EXPLICIT_WIKI_LINK provenance
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(neo4jClient, atLeastOnce()).query(queryCaptor.capture());

    boolean hasEdgeQuery =
        queryCaptor.getAllValues().stream()
            .anyMatch(q -> q.contains("REFERENCES") && q.contains("EXPLICIT_WIKI_LINK"));
    assertTrue(
        hasEdgeQuery, "Expected Cypher query creating REFERENCES edge with EXPLICIT_WIKI_LINK");
  }

  @Test
  @DisplayName("Case 04: UNKNOWN_REFERENCE 방어 - 존재하지 않는 타겟(Phantom Node)으로의 Edge 생성을 차단한다")
  void testUnknownReference_RejectsPhantomNode() throws IOException {
    // Given: workspace has only redis-distributed-lock, but body references
    // [[phantom-secret-service]]
    Path conceptsDir = tempDir.resolve("test-ws").resolve("wiki").resolve("concepts");
    Files.createDirectories(conceptsDir);

    String redisContent =
        "# Redis Distributed Lock\n"
            + "This references [[phantom-secret-service]] which does NOT exist.\n";
    Files.writeString(conceptsDir.resolve("redis-distributed-lock.md"), redisContent);

    // When
    llmService.syncWikiLinksToNeo4j("test-ws");

    // Then: Edge creation must be skipped, phantom node must NOT be created
    verify(neo4jClient, never()).query(contains("-[:REFERENCES]->"));
  }

  @Test
  @DisplayName("Self-Reference 차단 - 자기 자신을 가리키는 [[self]] 링크는 Edge 생성을 건너뛴다")
  void testSelfReference_IsSkipped() throws IOException {
    // Given: redis-distributed-lock references [[redis-distributed-lock]]
    Path conceptsDir = tempDir.resolve("test-ws").resolve("wiki").resolve("concepts");
    Files.createDirectories(conceptsDir);

    String redisContent =
        "# Redis Distributed Lock\n" + "자기 자신을 참조하는 [[redis-distributed-lock]] 링크.\n";
    Files.writeString(conceptsDir.resolve("redis-distributed-lock.md"), redisContent);

    // When
    llmService.syncWikiLinksToNeo4j("test-ws");

    // Then
    verify(neo4jClient, never()).query(contains("-[:REFERENCES]->"));
  }

  @Test
  @DisplayName("LLM links 검증 - LLM이 반환한 links 중 nodes에 없는 Phantom Node는 차단된다")
  void testLlmLinksValidation_RejectsPhantomNode() {
    Map<String, Object> node1 = Map.of("id", "spring-security", "name", "Spring Security");
    Map<String, Object> phantomLink =
        Map.of(
            "source", "spring-security",
            "target", "unknown-ghost-auth",
            "label", "DEPENDENCY");
    Map<String, Object> graphData =
        Map.of(
            "nodes", List.of(node1),
            "links", List.of(phantomLink));

    // When
    llmService.saveGraphToNeo4j(graphData, "test-ws");

    // Then: Only node1 query executed, phantom link rejected
    verify(neo4jClient, times(1)).query(contains("MERGE (c:Concept"));
    verify(neo4jClient, never()).query(contains("-[:REFERENCES]->"));
  }

  @Test
  @DisplayName("LLM links 정상 변환 - 유효한 두 노드 사이의 LLM 관계는 LLM_INFERRED provenance로 저장된다")
  void testLlmLinksValidation_CreatesValidEdge() {
    Map<String, Object> node1 = Map.of("id", "spring-security", "name", "Spring Security");
    Map<String, Object> node2 = Map.of("id", "jwt", "name", "JWT");
    Map<String, Object> validLink =
        Map.of(
            "source", "spring-security",
            "target", "jwt",
            "label", "DEPENDENCY");
    Map<String, Object> graphData =
        Map.of(
            "nodes", List.of(node1, node2),
            "links", List.of(validLink));

    // When
    llmService.saveGraphToNeo4j(graphData, "test-ws");

    // Then
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    verify(neo4jClient, atLeastOnce()).query(queryCaptor.capture());

    boolean hasEdgeQuery =
        queryCaptor.getAllValues().stream()
            .anyMatch(q -> q.contains("REFERENCES") && q.contains("LLM_INFERRED"));
    assertTrue(hasEdgeQuery, "Expected Cypher query creating REFERENCES edge with LLM_INFERRED");
  }
}
