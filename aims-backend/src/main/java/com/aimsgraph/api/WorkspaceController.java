package com.aimsgraph.api;

import com.aimsgraph.auth.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;
import org.springframework.data.neo4j.core.Neo4jClient;
import jakarta.servlet.http.HttpServletResponse;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.nio.file.*;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.aimsgraph.domain.workspace.WorkspaceCredentialsService;
import com.aimsgraph.domain.workspace.WorkspaceCredentials;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {



    private final Neo4jClient neo4jClient;
    private final ObjectMapper objectMapper;
    private final WorkspaceCredentialsService credentialsService;
    
    @org.springframework.beans.factory.annotation.Value("${wiki.base.dir:workspaces}")
    private String wikiBaseDir;


    
    @GetMapping("/list")
    public ResponseEntity<java.util.List<String>> listWorkspaces() {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser") || username.equals("dummy_user")) {
            return ResponseEntity.ok(java.util.List.of());
        }

        Path path = Paths.get(wikiBaseDir);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            return ResponseEntity.ok(java.util.List.of());
        }

        try (Stream<Path> stream = Files.list(path)) {
            java.util.List<String> dirs = stream
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.equals("ws-" + username) || name.startsWith(username + "_"))
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(dirs);
        } catch (IOException e) {
            return ResponseEntity.ok(java.util.List.of());
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createWorkspace(@RequestBody Map<String, String> body) {
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null || username.equals("anonymousUser") || username.equals("dummy_user")) {
            return ResponseEntity.status(401).body(Map.of("error", "UNAUTHORIZED", "message", "User must be logged in to create a workspace"));
        }

        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_NAME", "message", "Workspace name is required"));
        }

        // Sanitize: only allow lowercase letters, numbers, hyphens
        String sanitizedSuffix = name.trim().toLowerCase().replaceAll("[^a-z0-9\\-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (sanitizedSuffix.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_NAME", "message", "Workspace name must contain at least one alphanumeric character"));
        }

        String sanitized = username + "_" + sanitizedSuffix;

        Path wsPath = Paths.get(wikiBaseDir, sanitized);
        if (Files.exists(wsPath)) {
            return ResponseEntity.status(409).body(Map.of("error", "ALREADY_EXISTS", "message", "Workspace '" + sanitized + "' already exists"));
        }

        try {
            Files.createDirectories(wsPath.resolve("wiki").resolve("concepts"));
            return ResponseEntity.ok(Map.of("status", "OK", "workspaceId", sanitized));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "CREATE_FAILED", "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping("/{workspace_id}/graph")
    public ResponseEntity<Map<String, Object>> getWorkspaceGraph(
            @PathVariable("workspace_id") String workspaceId) {
        
        String currentTenant = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                
        if (!workspaceId.equals(currentTenant)) {
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN", "message", "Workspace mismatch"));
        }

        try {
            // 1. Fetch nodes from Neo4j
            var nodes = neo4jClient.query(
                "MATCH (n:Concept {workspaceId: $workspaceId}) " +
                "RETURN n.name AS id, n.title AS name"
            )
            .bind(workspaceId).to("workspaceId")
            .fetch().all()
            .stream()
            .map(row -> Map.of(
                "id", row.get("id"),
                "name", row.get("name") != null ? row.get("name") : row.get("id"),
                "type", "concept",
                "val", 1
            ))
            .collect(Collectors.toList());

            // 2. Fetch links from Neo4j
            var links = neo4jClient.query(
                "MATCH (n:Concept {workspaceId: $workspaceId})-[r]->(m:Concept {workspaceId: $workspaceId}) " +
                "RETURN n.name AS source, m.name AS target, type(r) AS label"
            )
            .bind(workspaceId).to("workspaceId")
            .fetch().all()
            .stream()
            .map(row -> Map.of(
                "source", row.get("source"),
                "target", row.get("target"),
                "label", row.get("label")
            ))
            .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                "nodes", nodes,
                "links", links
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "GRAPH_FETCH_FAILED",
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping(value = "/{workspace_id}/export", produces = "application/zip")
    public void exportWorkspace(
            @PathVariable("workspace_id") String workspaceId,
            HttpServletResponse response) throws IOException {
        
        String currentTenant = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
                
        if (!workspaceId.equals(currentTenant)) {
            response.sendError(403, "Workspace mismatch");
            return;
        }

        response.setStatus(200);
        response.setHeader("Content-Disposition", "attachment; filename=\"workspace-" + workspaceId + ".zip\"");
        
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            // 1. Export Neo4j Data
            var results = neo4jClient.query("MATCH (n {workspaceId: $workspaceId})-[r]-(m {workspaceId: $workspaceId}) RETURN n, r, m")
                    .bind(workspaceId).to("workspaceId")
                    .fetch().all();
            
            String jsonGraph = objectMapper.writeValueAsString(results);
            zos.putNextEntry(new ZipEntry("graph_data.json"));
            zos.write(jsonGraph.getBytes());
            zos.closeEntry();
            
            // 2. Export Wiki Files
            Path wsPath = Paths.get(wikiBaseDir, workspaceId, "wiki");
            if (Files.exists(wsPath)) {
                Files.walk(wsPath).filter(p -> !Files.isDirectory(p)).forEach(path -> {
                    try {
                        String zipName = wsPath.relativize(path).toString().replace("\\", "/");
                        zos.putNextEntry(new ZipEntry("wiki/" + zipName));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }
    

    @DeleteMapping("/{workspace_id}/data")
    public ResponseEntity<Map<String, Object>> deleteWorkspaceData(
            @PathVariable("workspace_id") String workspaceId) {

        String currentTenant = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        if (!workspaceId.equals(currentTenant)) {
            return ResponseEntity.status(403).body(Map.of("error", "FORBIDDEN", "message", "Workspace mismatch"));
        }

        try {
            // 1. Delete all Neo4j nodes and relationships for this workspace
            var deleteResult = neo4jClient.query(
                "MATCH (n {workspaceId: $workspaceId}) DETACH DELETE n RETURN count(n) AS deleted"
            )
            .bind(workspaceId).to("workspaceId")
            .fetch().one();

            long deletedNodes = 0;
            if (deleteResult.isPresent()) {
                Object val = deleteResult.get().get("deleted");
                deletedNodes = val instanceof Number ? ((Number) val).longValue() : 0;
            }

            // 2. Delete all files and the workspace directory itself
            long deletedFiles = 0;
            Path workspacePath = Paths.get(wikiBaseDir, workspaceId);
            if (Files.exists(workspacePath)) {
                // Walk in reverse depth order so files are deleted before directories
                java.util.List<Path> filesToDelete = Files.walk(workspacePath)
                        .sorted(java.util.Comparator.reverseOrder())
                        .collect(Collectors.toList());
                for (Path p : filesToDelete) {
                    if (!Files.isDirectory(p)) {
                        deletedFiles++;
                    }
                    Files.deleteIfExists(p);
                }
            }

            return ResponseEntity.ok(Map.of(
                "status", "OK",
                "deletedNodes", deletedNodes,
                "deletedFiles", deletedFiles
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "DELETE_FAILED",
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    @GetMapping("/{workspace_id}/credentials/status")
    public ResponseEntity<Map<String, Boolean>> getCredentialsStatus(
            @PathVariable("workspace_id") String workspaceId) {
        String currentTenant = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (!workspaceId.equals(currentTenant)) {
            return ResponseEntity.status(403).build();
        }
        
        WorkspaceCredentials creds = credentialsService.getRawCredentials(workspaceId);
        
        boolean hasNotion = creds != null && creds.getNotionApiKey() != null && !creds.getNotionApiKey().isBlank();
        boolean hasGithub = creds != null && creds.getGithubApiKey() != null && !creds.getGithubApiKey().isBlank();
        boolean hasDeepseek = creds != null && creds.getDeepseekApiKey() != null && !creds.getDeepseekApiKey().isBlank();

        return ResponseEntity.ok(Map.of(
            "hasNotionKey", hasNotion,
            "hasGithubKey", hasGithub,
            "hasDeepseekKey", hasDeepseek
        ));
    }

    @PutMapping("/{workspace_id}/credentials")
    public ResponseEntity<Map<String, String>> updateCredentials(
            @PathVariable("workspace_id") String workspaceId,
            @RequestBody Map<String, String> body) {
        String currentTenant = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (!workspaceId.equals(currentTenant)) {
            return ResponseEntity.status(403).build();
        }
        
        String notionApiKey = body.get("notionApiKey");
        String githubApiKey = body.get("githubApiKey");
        String deepseekApiKey = body.get("deepseekApiKey");
        
        credentialsService.updateCredentials(workspaceId, notionApiKey, githubApiKey, deepseekApiKey);
        
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}
