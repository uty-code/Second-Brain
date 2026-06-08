package com.aimsgraph.api;

import com.aimsgraph.auth.JwtInterceptor;
import com.aimsgraph.domain.source.RawSource;
import com.aimsgraph.domain.source.RawSourceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
public class IngestController {

    private final RawSourceService rawSourceService;

    @PostMapping
    public ResponseEntity<Map<String, String>> ingest(@RequestBody IngestRequest request) {
        String workspaceId = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        if (workspaceId == null || workspaceId.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED", "message", "Workspace ID missing"));
        }

        RawSource source = new RawSource();
        source.setWorkspaceId(workspaceId);
        source.setTitle(request.getTitle());
        source.setContentHash(String.valueOf(request.getContent().hashCode()));
        source.setSourceType("DOCUMENT");
        source.setSourceUri(request.getSourceUri());
        source.setCreatedAt(LocalDateTime.now());
        source.setUpdatedAt(LocalDateTime.now());

        rawSourceService.ingestSource(source);

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Ingestion started"));
    }

    @Data
    public static class IngestRequest {
        private String title;
        private String content;
        private String sourceUri;
    }
}
