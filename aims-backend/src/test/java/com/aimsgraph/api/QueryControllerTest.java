package com.aimsgraph.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aimsgraph.domain.wiki.FileBackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(QueryController.class)
@org.springframework.security.test.context.support.WithMockUser(username = "testuser")
public class QueryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private FileBackService fileBackService;

  @MockBean private com.aimsgraph.ingest.LlmService llmService;

  @MockBean private com.aimsgraph.auth.JwtInterceptor jwtInterceptor;

  @MockBean private com.aimsgraph.auth.JwtUtil jwtUtil;

  @Test
  void query_Success() throws Exception {
    org.mockito.Mockito.when(
            jwtInterceptor.preHandle(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);

    org.mockito.Mockito.when(jwtUtil.validateToken("test-workspace")).thenReturn(true);
    org.mockito.Mockito.when(jwtUtil.getUsernameFromToken("test-workspace")).thenReturn("testuser");
    org.mockito.Mockito.when(jwtUtil.getWorkspaceIdFromToken("test-workspace"))
        .thenReturn("test-workspace");

    when(fileBackService.saveInsight(anyString(), anyString(), anyString()))
        .thenReturn("insight-123.md");
    com.aimsgraph.ingest.LlmService.AgentResponse mockResponse =
        new com.aimsgraph.ingest.LlmService.AgentResponse("Actual LLM Answer", false);
    when(llmService.query(
            anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean()))
        .thenReturn(mockResponse);

    String jsonPayload =
        """
                {
                  "query": "What is AIMS-Graph?",
                  "file_back": true
                }
                """;

    mockMvc
        .perform(
            post("/api/v1/query")
                .header("Authorization", "Bearer test-workspace")
                .requestAttr("workspaceId", "test-workspace")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload)
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.answer").isNotEmpty())
        .andExpect(jsonPath("$.insightFile").value("insight-123.md"));
  }
}
