package com.aimsgraph.lint;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LintController.class)
public class LintControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LintService lintService;
    
    @MockBean
    private com.aimsgraph.auth.JwtInterceptor jwtInterceptor;

    @Test
    void shouldTriggerLint() throws Exception {
        LintResponse mockResponse = new LintResponse();
        mockResponse.setIssuesFound(3);
        mockResponse.setAutoFixed(2);
        
        LintIssue issue = new LintIssue();
        issue.setPage("wiki/concepts/old-topic.md");
        issue.setIssue("STALE_DATA");
        mockResponse.setRequiresReview(List.of(issue));

        given(lintService.performLint(any(LintRequest.class), any(String.class))).willReturn(mockResponse);

        String jsonPayload = """
            {
              "scope": "CHANGED_SUBGRAPH",
              "since": "2026-06-01T00:00:00Z"
            }
            """;

        mockMvc.perform(post("/api/internal/lint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload)
                .requestAttr(com.aimsgraph.auth.JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, "test-workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issues_found").value(3))
                .andExpect(jsonPath("$.auto_fixed").value(2))
                .andExpect(jsonPath("$.requires_review[0].page").value("wiki/concepts/old-topic.md"));
    }
}
