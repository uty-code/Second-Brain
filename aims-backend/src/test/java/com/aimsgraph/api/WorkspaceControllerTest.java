package com.aimsgraph.api;

import com.aimsgraph.domain.workspace.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkspaceController.class)
public class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkspaceService workspaceService;
    
    @MockBean
    private org.springframework.data.neo4j.core.Neo4jClient neo4jClient;
    
    @MockBean
    private com.aimsgraph.auth.JwtInterceptor jwtInterceptor;

    @Test
    void registerApiKey_Success() throws Exception {
        org.mockito.Mockito.when(jwtInterceptor.preHandle(
                org.mockito.ArgumentMatchers.any(), 
                org.mockito.ArgumentMatchers.any(), 
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(true);
            
        // doNothing().when(workspaceService).verifyAndSaveApiKey(anyString(), anyString(), anyString());

        String jsonPayload = """
                {
                  "llm_provider": "OPENAI",
                  "api_key": "sk-proj-test-key"
                }
                """;

        mockMvc.perform(post("/api/v1/workspaces/test-workspace/api-key")
                .header("Authorization", "Bearer test-workspace")
                .requestAttr("workspaceId", "test-workspace")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("API Key has been verified, encrypted, and saved."));
    }
}
