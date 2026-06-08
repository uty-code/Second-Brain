package com.aimsgraph.mcp;

import com.aimsgraph.auth.JwtInterceptor;
import com.aimsgraph.domain.wiki.FileBackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class McpToolsImpl {

    private final Neo4jClient neo4jClient;
    private final FileBackService fileBackService;
    private final ObjectMapper objectMapper;

    @Value("${wiki.base.dir:workspaces}")
    private String wikiBaseDir;

    private String getWorkspaceId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String workspaceId = (String) attrs.getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (workspaceId != null && !workspaceId.isEmpty()) {
                return workspaceId;
            }
        }
        return "default"; // Fallback for tests without context
    }

    @Tool(description = "Search the knowledge graph")
    public String searchGraph(String conceptName) {
        String workspaceId = getWorkspaceId();
        String cypher = "MATCH (n {workspaceId: $workspaceId})-[r]-(m {workspaceId: $workspaceId}) " +
                "WHERE n.name = $conceptName OR n.id = $conceptName " +
                "RETURN n, r, m LIMIT 50";
        try {
            var results = neo4jClient.query(cypher)
                    .bind(workspaceId).to("workspaceId")
                    .bind(conceptName).to("conceptName")
                    .fetch().all();
            return results.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Get context for graph traversal (Markdown + Edges)")
    public String getNodeContext(String conceptName) {
        String workspaceId = getWorkspaceId();
        try {
            Path wsPath = Path.of(wikiBaseDir, workspaceId, "wiki").toAbsolutePath().normalize();
            Path conceptPath = wsPath.resolve("concepts/" + conceptName + ".md");
            Path entityPath = wsPath.resolve("entities/" + conceptName + ".md");
            
            String content = "File not found.";
            if (Files.exists(conceptPath)) content = Files.readString(conceptPath);
            else if (Files.exists(entityPath)) content = Files.readString(entityPath);
            
            String edgeCypher = "MATCH (n {workspaceId: $workspaceId, name: $conceptName})-[r]->(m) " +
                                "RETURN n.name as source, type(r) as rel, m.name as target " +
                                "UNION " +
                                "MATCH (m)-[r]->(n {workspaceId: $workspaceId, name: $conceptName}) " +
                                "RETURN m.name as source, type(r) as rel, n.name as target";
            
            var edgeResults = neo4jClient.query(edgeCypher)
                    .bind(workspaceId).to("workspaceId")
                    .bind(conceptName).to("conceptName")
                    .fetch().all();
                    
            List<String> edges = new ArrayList<>();
            for (var row : edgeResults) {
                edges.add(row.get("source") + " [" + row.get("rel") + "] -> " + row.get("target"));
            }
            
            return objectMapper.writeValueAsString(Map.of("content", content, "edges", edges));
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Read a wiki page")
    public String readWikiPage(String pagePath) {
        String workspaceId = getWorkspaceId();
        try {
            Path wsPath = Path.of(wikiBaseDir, workspaceId, "wiki").toAbsolutePath().normalize();
            Path filePath = wsPath.resolve(pagePath).toAbsolutePath().normalize();
            if (!filePath.startsWith(wsPath)) return "Error: Invalid path";
            if (Files.exists(filePath)) return Files.readString(filePath);
            return "Error: File not found";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Save an insight")
    public String fileBackInsight(String title, String content) {
        String workspaceId = getWorkspaceId();
        try {
            String savedFile = fileBackService.saveInsight(workspaceId, title, content);
            return "Successfully saved insight: " + savedFile;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "List recent wiki changes")
    public String listRecentChanges() {
        String workspaceId = getWorkspaceId();
        try {
            Path logPath = Path.of(wikiBaseDir, workspaceId, "wiki", "log.md");
            if (Files.exists(logPath)) return Files.readString(logPath);
            return "No recent changes found.";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
