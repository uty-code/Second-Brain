package com.aimsgraph.api;

import com.aimsgraph.auth.JwtInterceptor;
import com.aimsgraph.domain.wiki.FileBackService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/query")
@RequiredArgsConstructor
public class QueryController {

    private final FileBackService fileBackService;
    private final com.aimsgraph.ingest.LlmService llmService;

    @PostMapping
    public ResponseEntity<Map<String, String>> query(@RequestBody QueryRequest request) {
        String workspaceId = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        if (workspaceId == null || workspaceId.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED", "message", "Workspace ID missing"));
        }

        String answer = llmService.query(workspaceId, request.getQuery());
        
        Map<String, String> response = new HashMap<>();
        response.put("answer", answer);

        if (request.isFile_back()) {
            String title = "Insight from: " + request.getQuery();
            String fileName = fileBackService.saveInsight(workspaceId, title, answer);
            response.put("insightFile", fileName);
        }

        return ResponseEntity.ok(response);
    }

    @Data
    public static class QueryRequest {
        private String query;
        private boolean file_back;
    }
}
