package com.aimsgraph.api;

import com.aimsgraph.ingest.LlmService;
import com.aimsgraph.auth.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/analyze")
@RequiredArgsConstructor
public class AnalyzeController {

    private final LlmService llmService;

    /**
     * 여러 파일을 업로드하면 OpenAI Responses API를 통해 지식을 추출하고
     * 통합된 그래프 데이터(nodes + links)로 반환합니다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> analyzeFiles(
            @RequestParam("files") MultipartFile[] files) {
        
        log.info("Received {} file(s) for analysis", files.length);

        if (files.length == 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "NO_FILES", "message", "파일이 없습니다."));
        }

        String workspaceId = "default-workspace";
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String attrWorkspaceId = (String) attrs.getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (attrWorkspaceId != null && !attrWorkspaceId.isEmpty()) {
                workspaceId = attrWorkspaceId;
            }
        }

        try {
            Map<String, Object> graphData = llmService.analyzeFilesWithOpenAI(files, workspaceId);
            return ResponseEntity.ok(graphData);
        } catch (Exception e) {
            log.error("File analysis failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "ANALYSIS_FAILED", "message", e.getMessage()));
        }
    }
}
