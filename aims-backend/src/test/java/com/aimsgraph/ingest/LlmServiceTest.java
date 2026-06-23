package com.aimsgraph.ingest;

import com.aimsgraph.domain.workspace.WorkspaceService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LlmServiceTest {

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private com.aimsgraph.domain.workspace.WorkspaceCredentialsService credentialsService;

    @Mock
    private ChatModel chatModel;

    @Mock
    private Neo4jClient neo4jClient;

    @InjectMocks
    private LlmService llmService;

    @BeforeEach
    void setUp() {
        Map<String, ChatModel> mockCache = new ConcurrentHashMap<>();
        mockCache.put("tenant-1:gpt-4o-mini", chatModel);
        ReflectionTestUtils.setField(llmService, "modelCache", mockCache);
    }

    @Test
    void extractKnowledge_shouldParseJsonCorrectly() {
        String mockJsonResponse = "[\n" +
                "  {\n" +
                "    \"name\": \"test-concept\",\n" +
                "    \"title\": \"Test Concept\",\n" +
                "    \"type\": \"concept\",\n" +
                "    \"tags\": [\"test\"],\n" +
                "    \"aliases\": [\"alias1\"],\n" +
                "    \"summary\": \"Test summary\",\n" +
                "    \"content\": \"Test content\",\n" +
                "    \"linkedConcepts\": [{\"name\": \"test-concept-2\", \"type\": \"EXTENDS\"}]\n" +
                "  }\n" +
                "]";

        when(chatModel.chat(anyString())).thenReturn(mockJsonResponse);

        List<ExtractedConcept> results = llmService.extractKnowledge("event-1", "This is test content", "tenant-1");

        assertNotNull(results);
        assertEquals(1, results.size());
        ExtractedConcept concept = results.get(0);
        assertEquals("test-concept", concept.getName());
        assertEquals("Test Concept", concept.getTitle());
        assertEquals("concept", concept.getType());
        assertEquals("Test summary", concept.getSummary());
        assertEquals("Test content", concept.getContent());
        assertNotNull(concept.getTags());
        assertEquals(1, concept.getTags().size());
        assertEquals("test", concept.getTags().get(0));
        assertNotNull(concept.getLinkedConcepts());
        assertEquals(1, concept.getLinkedConcepts().size());
        assertEquals("test-concept-2", concept.getLinkedConcepts().get(0).getName());
        assertEquals("EXTENDS", concept.getLinkedConcepts().get(0).getType());
    }

    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path tempWikiDir;

    @Test
    void query_shouldUseAgenticGraphTraversal() throws Exception {
        // Given
        ReflectionTestUtils.setField(llmService, "wikiBaseDir", tempWikiDir.toAbsolutePath().toString());
        ReflectionTestUtils.setField(llmService, "defaultApiKey", "test-api-key");

        dev.langchain4j.data.message.AiMessage aiMessage = dev.langchain4j.data.message.AiMessage.from("Agent Answer");
        ChatResponse chatResponse = ChatResponse.builder().aiMessage(aiMessage).build();
        
        org.mockito.Mockito.lenient().when(chatModel.chat(
                org.mockito.ArgumentMatchers.any(dev.langchain4j.model.chat.request.ChatRequest.class)
        )).thenReturn(chatResponse);
        org.mockito.Mockito.lenient().when(chatModel.chat(
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn("Agent Answer");

        // When
        com.aimsgraph.ingest.LlmService.AgentResponse response = llmService.query("tenant-1", "Explain Harness Framework", "gpt-4o-mini", false);
        System.out.println("TEST RESPONSE: " + response.answer());

        // Then
        assertEquals("Agent Answer", response.answer());
    }

    @Test
    void saveGraphToNeo4j_shouldExecuteMergeQueries() {
        // Given
        Map<String, Object> node = Map.of(
            "id", "concept-1",
            "name", "Concept 1"
        );
        Map<String, Object> link = Map.of(
            "source", "concept-1",
            "target", "concept-2",
            "label", "RELATES_TO"
        );
        Map<String, Object> graphData = Map.of(
            "nodes", List.of(node),
            "links", List.of(link)
        );

        org.springframework.data.neo4j.core.Neo4jClient.UnboundRunnableSpec unboundRunnableSpec =
                org.mockito.Mockito.mock(org.springframework.data.neo4j.core.Neo4jClient.UnboundRunnableSpec.class);
        org.springframework.data.neo4j.core.Neo4jClient.RunnableSpec runnableSpec =
                org.mockito.Mockito.mock(org.springframework.data.neo4j.core.Neo4jClient.RunnableSpec.class);
        org.springframework.data.neo4j.core.Neo4jClient.OngoingBindSpec ongoingBindSpec =
                org.mockito.Mockito.mock(org.springframework.data.neo4j.core.Neo4jClient.OngoingBindSpec.class);

        when(neo4jClient.query(anyString())).thenReturn(unboundRunnableSpec);
        when(unboundRunnableSpec.bind(org.mockito.ArgumentMatchers.any())).thenReturn(ongoingBindSpec);
        when(runnableSpec.bind(org.mockito.ArgumentMatchers.any())).thenReturn(ongoingBindSpec);
        when(ongoingBindSpec.to(anyString())).thenReturn(runnableSpec);

        // When
        llmService.saveGraphToNeo4j(graphData, "tenant-1");

        // Then
        org.mockito.Mockito.verify(neo4jClient, org.mockito.Mockito.atLeast(2)).query(anyString());
    }
}

