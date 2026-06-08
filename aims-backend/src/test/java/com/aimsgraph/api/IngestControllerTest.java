package com.aimsgraph.api;

import com.aimsgraph.domain.source.RawSource;
import com.aimsgraph.domain.source.RawSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(IngestController.class)
public class IngestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RawSourceService rawSourceService;

    @MockBean
    private com.aimsgraph.auth.JwtInterceptor jwtInterceptor;

    @Test
    void ingest_Success() throws Exception {
        org.mockito.Mockito.when(jwtInterceptor.preHandle(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()))
            .thenReturn(true);

        doNothing().when(rawSourceService).ingestSource(any(RawSource.class));

        String jsonPayload = """
                {
                  "title": "Test Title",
                  "content": "Test Content",
                  "sourceUri": "https://example.com/doc1"
                }
                """;

        mockMvc.perform(post("/api/v1/ingest")
                .header("Authorization", "Bearer test-workspace")
                .requestAttr("workspaceId", "test-workspace")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
