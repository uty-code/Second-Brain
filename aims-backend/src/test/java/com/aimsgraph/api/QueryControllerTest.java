package com.aimsgraph.api;

import com.aimsgraph.domain.wiki.FileBackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
public class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileBackService fileBackService;

    @MockBean
    private com.aimsgraph.ingest.LlmService llmService;

    @MockBean
    private com.aimsgraph.auth.JwtInterceptor jwtInterceptor;

    @Test
    void query_Success() throws Exception {
        org.mockito.Mockito.when(jwtInterceptor.preHandle(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(true);

        when(fileBackService.saveInsight(anyString(), anyString(), anyString())).thenReturn("insight-123.md");
        when(llmService.query(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.nullable(String.class))).thenReturn("Actual LLM Answer");

        String jsonPayload = """
                {
                  "query": "What is AIMS-Graph?",
                  "file_back": true
                }
                """;

        mockMvc.perform(post("/api/v1/query")
                .header("Authorization", "Bearer test-workspace")
                .requestAttr("workspaceId", "test-workspace")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.insightFile").value("insight-123.md"));
    }
}
