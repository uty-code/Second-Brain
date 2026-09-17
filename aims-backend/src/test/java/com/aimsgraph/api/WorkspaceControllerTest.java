package com.aimsgraph.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceController.class)
@org.springframework.security.test.context.support.WithMockUser(username = "testuser")
public class WorkspaceControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private org.springframework.data.neo4j.core.Neo4jClient neo4jClient;

  @MockBean private com.aimsgraph.domain.workspace.WorkspaceCredentialsService credentialsService;

  @MockBean private com.aimsgraph.auth.JwtInterceptor jwtInterceptor;

  @MockBean private com.aimsgraph.auth.JwtUtil jwtUtil;

  @Test
  void listWorkspaces_Success() throws Exception {
    org.mockito.Mockito.when(
            jwtInterceptor.preHandle(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);

    org.mockito.Mockito.when(jwtUtil.validateToken("valid-token")).thenReturn(true);
    org.mockito.Mockito.when(jwtUtil.getUsernameFromToken("valid-token")).thenReturn("testuser");
    org.mockito.Mockito.when(jwtUtil.getWorkspaceIdFromToken("valid-token"))
        .thenReturn("ws-testuser");

    mockMvc
        .perform(
            get("/api/v1/workspaces/list")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray());
  }
}
