package com.aimsgraph.api;

import com.aimsgraph.auth.JwtInterceptor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/api/v1/wiki")
@RequiredArgsConstructor
public class WikiController {

    private final Neo4jClient neo4jClient;

    @Value("${wiki.base.dir:workspaces}")
    private String wikiBaseDir;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9-_]+$");

    @GetMapping("/{conceptName}")
    public ResponseEntity<?> getWikiContent(@PathVariable String conceptName) {
        // 경로 조작 공격(Path Traversal) 방지를 위해 ../ 와 같은 문자만 차단하고, 파일명에 쓸 수 있는 문자는 최대한 허용합니다.
        if (conceptName == null || conceptName.isBlank() || conceptName.contains("..") || conceptName.contains("/") || conceptName.contains("\\")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "INVALID_CONCEPT_NAME", "message", "The concept name contains invalid characters."));
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
            Path wsPath = Paths.get(wikiBaseDir, workspaceId, "wiki").toAbsolutePath().normalize();
            Path rootWikiPath = Paths.get("wiki").toAbsolutePath().normalize();

            String slug = null;

            // 1. If it's already a valid slug format, try to use it directly
            if (conceptName != null && NAME_PATTERN.matcher(conceptName).matches()) {
                slug = conceptName;
            }

            // 2. Try to find the file with the slug
            Path targetFile = null;
            if (slug != null) {
                targetFile = findFile(wsPath, slug);
                if (targetFile == null) {
                    targetFile = findFile(rootWikiPath, slug);
                }
            }

            // 3. If file not found directly, or if name is invalid slug format (like Korean), resolve slug via Neo4j
            if (targetFile == null) {
                slug = resolveSlug(workspaceId, conceptName);
                if (slug != null && NAME_PATTERN.matcher(slug).matches()) {
                    targetFile = findFile(wsPath, slug);
                    if (targetFile == null) {
                        targetFile = findFile(rootWikiPath, slug);
                    }
                }
            }

            if (targetFile == null) {
                log.info("Wiki file not found for concept: {} (resolved slug: {}) in workspace: {} or root wiki", conceptName, slug, workspaceId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "FILE_NOT_FOUND", "message", "The requested wiki page could not be found."));
            }

            String content = Files.readString(targetFile);
            return ResponseEntity.ok(new WikiResponse(content));

        } catch (IOException e) {
            log.error("Failed to read wiki file: {}", conceptName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "READ_FAILED", "message", "Failed to read the wiki page content."));
        }
    }

    private String resolveSlug(String workspaceId, String conceptName) {
        if (conceptName == null || conceptName.isBlank()) {
            return null;
        }
        try {
            return neo4jClient.query(
                "MATCH (c:Concept {workspaceId: $workspaceId}) " +
                "WHERE c.name = $conceptName OR c.title = $conceptName " +
                "RETURN c.name AS name LIMIT 1"
            )
            .bind(workspaceId).to("workspaceId")
            .bind(conceptName).to("conceptName")
            .fetchAs(String.class)
            .first()
            .orElse(null);
        } catch (Exception e) {
            log.error("Failed to resolve slug from Neo4j for concept: {}", conceptName, e);
            return null;
        }
    }

    private Path findFile(Path basePath, String conceptName) {
        if (!Files.exists(basePath)) {
            return null;
        }
        Path conceptPath = basePath.resolve("concepts/" + conceptName + ".md").toAbsolutePath().normalize();
        Path entityPath = basePath.resolve("entities/" + conceptName + ".md").toAbsolutePath().normalize();
        Path insightPath = basePath.resolve("insights/" + conceptName + ".md").toAbsolutePath().normalize();

        if (Files.exists(conceptPath) && conceptPath.startsWith(basePath)) {
            return conceptPath;
        } else if (Files.exists(entityPath) && entityPath.startsWith(basePath)) {
            return entityPath;
        } else if (Files.exists(insightPath) && insightPath.startsWith(basePath)) {
            return insightPath;
        }
        return null;
    }

    @Data
    public static class WikiResponse {
        private final String content;
    }
}
