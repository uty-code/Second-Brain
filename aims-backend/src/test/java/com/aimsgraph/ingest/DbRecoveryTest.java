package com.aimsgraph.ingest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.neo4j.core.Neo4jClient;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
public class DbRecoveryTest {

    @Autowired
    private Neo4jClient neo4jClient;

    @Test
    public void recoverDb() throws IOException {
        String workspaceId = "1000_test";
        Path conceptsDir = Paths.get("workspaces", workspaceId, "wiki", "concepts");
        if (!Files.exists(conceptsDir)) {
            System.out.println("No concepts directory found!");
            return;
        }

        System.out.println("Starting DB recovery for workspace: " + workspaceId);

        Files.list(conceptsDir).forEach(filePath -> {
            if (Files.isDirectory(filePath) || !filePath.toString().endsWith(".md")) return;
            try {
                String content = Files.readString(filePath);
                String fileName = filePath.getFileName().toString();
                String conceptId = fileName.substring(0, fileName.lastIndexOf(".md"));

                // Parse title from Frontmatter
                String title = conceptId;
                Pattern titlePattern = Pattern.compile("title:\\s*(.*)");
                Matcher titleMatcher = titlePattern.matcher(content);
                if (titleMatcher.find()) {
                    title = titleMatcher.group(1).trim();
                }

                // MERGE Concept node
                String createNodeCypher = "MERGE (c:Concept {name: $name, workspaceId: $workspaceId}) " +
                        "ON CREATE SET c.title = $title, c.createdAt = datetime() " +
                        "ON MATCH SET c.title = $title, c.updatedAt = datetime()";
                neo4jClient.query(createNodeCypher)
                           .bind(conceptId).to("name")
                           .bind(workspaceId).to("workspaceId")
                           .bind(title).to("title")
                           .run();
                System.out.println("Recovered node: " + conceptId + " (" + title + ")");

                // Parse Related Concepts
                List<String> related = new ArrayList<>();
                Pattern relatedPattern = Pattern.compile("- \\[\\[(.*?)\\]\\]");
                Matcher relatedMatcher = relatedPattern.matcher(content);
                while (relatedMatcher.find()) {
                    related.add(relatedMatcher.group(1).trim());
                }

                for (String rel : related) {
                    String linkCypher = "MERGE (c:Concept {name: $name, workspaceId: $workspaceId}) " +
                                        "MERGE (l:Concept {name: $link, workspaceId: $workspaceId}) " +
                                        "MERGE (c)-[:RELATES_TO]->(l)";
                    neo4jClient.query(linkCypher)
                               .bind(conceptId).to("name")
                               .bind(rel).to("link")
                               .bind(workspaceId).to("workspaceId")
                               .run();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        System.out.println("DB Recovery Complete!");
    }
}
