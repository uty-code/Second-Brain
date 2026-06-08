package com.aimsgraph.ingest;

import com.aimsgraph.domain.workspace.WorkspaceService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    private ChatLanguageModel chatLanguageModel;

    @InjectMocks
    private LlmService llmService;

    @BeforeEach
    void setUp() {
        Map<String, ChatLanguageModel> mockCache = new ConcurrentHashMap<>();
        mockCache.put("tenant-1", chatLanguageModel);
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

        when(chatLanguageModel.generate(anyString())).thenReturn(mockJsonResponse);

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
    void query_shouldRetrieveWikiFilesAndEnrichQuery() throws Exception {
        // Given
        ReflectionTestUtils.setField(llmService, "wikiBaseDir", tempWikiDir.toAbsolutePath().toString());
        ReflectionTestUtils.setField(llmService, "defaultApiKey", "test-api-key");

        java.nio.file.Path conceptDir = tempWikiDir.resolve("tenant-1").resolve("wiki").resolve("concepts");
        java.nio.file.Files.createDirectories(conceptDir);
        java.nio.file.Path file1 = conceptDir.resolve("harness-framework.md");
        java.nio.file.Files.writeString(file1, "Harness Framework is a software deployment framework.");

        java.nio.file.Path entityDir = tempWikiDir.resolve("tenant-1").resolve("wiki").resolve("entities");
        java.nio.file.Files.createDirectories(entityDir);
        java.nio.file.Path file2 = entityDir.resolve("some-entity.md");
        java.nio.file.Files.writeString(file2, "Entity info.");

        when(chatLanguageModel.generate(anyString())).thenReturn("[0]");

        LlmService spyService = org.mockito.Mockito.spy(llmService);
        org.mockito.ArgumentCaptor<String> queryCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.doReturn("Mocked Response").when(spyService).queryDirect(anyString(), queryCaptor.capture());

        // When
        String response = spyService.query("tenant-1", "Explain Harness Framework");

        // Then
        assertEquals("Mocked Response", response);
        String capturedQuery = queryCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(capturedQuery.contains("Harness Framework is a software deployment framework."));
        org.junit.jupiter.api.Assertions.assertTrue(capturedQuery.contains("Explain Harness Framework"));
    }
}

