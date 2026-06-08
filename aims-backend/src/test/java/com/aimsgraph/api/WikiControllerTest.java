package com.aimsgraph.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WikiController.class, properties = "wiki.base.dir=build/test-workspaces")
public class WikiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private com.aimsgraph.auth.JwtInterceptor jwtInterceptor;

    @MockBean
    private org.springframework.data.neo4j.core.Neo4jClient neo4jClient;

    private final Path testWorkspacePath = Paths.get("build/test-workspaces/test-workspace/wiki");

    @BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.when(jwtInterceptor.preHandle(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(true);
            
        Files.createDirectories(testWorkspacePath.resolve("concepts"));
        Files.createDirectories(testWorkspacePath.resolve("entities"));
        Files.createDirectories(testWorkspacePath.resolve("insights"));
    }

    @AfterEach
    void tearDown() throws IOException {
        deleteDirectory(Paths.get("build/test-workspaces"));
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                 .sorted((p1, p2) -> p2.compareTo(p1))
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                     } catch (IOException e) {
                         // Ignore
                     }
                 });
        }
    }

    @Test
    void getWikiContent_Concept_Success() throws Exception {
        String conceptName = "neural-network";
        String content = "# Neural Network\\nThis is a test concept.";
        Files.writeString(testWorkspacePath.resolve("concepts/" + conceptName + ".md"), content);

        mockMvc.perform(get("/api/v1/wiki/" + conceptName)
                .requestAttr("workspaceId", "test-workspace")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    void getWikiContent_Entity_Success() throws Exception {
        String entityName = "openai";
        String content = "# OpenAI\\nThis is a test entity.";
        Files.writeString(testWorkspacePath.resolve("entities/" + entityName + ".md"), content);

        mockMvc.perform(get("/api/v1/wiki/" + entityName)
                .requestAttr("workspaceId", "test-workspace")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    void getWikiContent_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/wiki/non-existent-concept")
                .requestAttr("workspaceId", "test-workspace")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FILE_NOT_FOUND"));
    }

    @Test
    void getWikiContent_InvalidName() throws Exception {
        mockMvc.perform(get("/api/v1/wiki/invalid-name!")
                .requestAttr("workspaceId", "test-workspace")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_CONCEPT_NAME"));
    }
}
