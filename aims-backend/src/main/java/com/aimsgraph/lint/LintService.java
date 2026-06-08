package com.aimsgraph.lint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class LintService {

    private final Neo4jClient neo4jClient;
    private final RedissonClient redissonClient;

    @Value("${wiki.base.dir:wiki}")
    private String wikiBaseDir;

    public LintResponse performLint(LintRequest request, String workspaceId) {
        LintResponse response = new LintResponse();
        
        RLock indexLock = redissonClient.getLock("wiki:" + workspaceId + ":index.md");
        RLock logLock = redissonClient.getLock("wiki:" + workspaceId + ":log.md");
        
        boolean indexLocked = false;
        boolean logLocked = false;
        
        try {
            indexLocked = indexLock.tryLock(10, 30, TimeUnit.SECONDS);
            logLocked = logLock.tryLock(10, 30, TimeUnit.SECONDS);
            
            if (!indexLocked || !logLocked) {
                throw new RuntimeException("Could not acquire locks for linting");
            }
            
            List<Path> filesToCheck = getFilesToCheck(request, workspaceId);
            List<String> indexLinks = getIndexLinks(workspaceId);
            
            int issuesFound = 0;
            int autoFixed = 0;
            List<LintIssue> requiresReview = new ArrayList<>();
            
            for (Path file : filesToCheck) {
                String content = Files.readString(file);
                String fileName = file.getFileName().toString();
                String slug = fileName.replace(".md", "");
                
                boolean fileModified = false;
                
                // 1. ORPHAN_PAGE check
                if (!indexLinks.contains(slug)) {
                    issuesFound++;
                    addToIndex(slug, workspaceId);
                    indexLinks.add(slug);
                    autoFixed++;
                    logLintEvent(slug, "Auto-fixed ORPHAN_PAGE", workspaceId);
                }
                
                // 2. MISSING_FRONTMATTER check
                if (!hasCompleteFrontmatter(content)) {
                    issuesFound++;
                    content = autoFixFrontmatter(content, slug);
                    fileModified = true;
                    autoFixed++;
                    logLintEvent(slug, "Auto-fixed MISSING_FRONTMATTER", workspaceId);
                }
                
                // 3. BROKEN_LINK check
                List<String> brokenLinks = findBrokenLinks(content, filesToCheck);
                for (String bl : brokenLinks) {
                    issuesFound++;
                    LintIssue issue = new LintIssue();
                    issue.setPage(file.toString());
                    issue.setIssue("BROKEN_LINK: " + bl);
                    requiresReview.add(issue);
                }
                
                // 4. STALE_DATA check
                if (isStale(content)) {
                    issuesFound++;
                    LintIssue issue = new LintIssue();
                    issue.setPage(file.toString());
                    issue.setIssue("STALE_DATA");
                    requiresReview.add(issue);
                }
                
                // 5. CONTRADICTION check
                if (hasContradiction(slug, workspaceId)) {
                    issuesFound++;
                    LintIssue issue = new LintIssue();
                    issue.setPage(file.toString());
                    issue.setIssue("CONTRADICTION");
                    requiresReview.add(issue);
                }
                
                if (fileModified) {
                    Files.writeString(file, content, StandardOpenOption.TRUNCATE_EXISTING);
                }
            }
            
            response.setIssuesFound(issuesFound);
            response.setAutoFixed(autoFixed);
            response.setRequiresReview(requiresReview);
            
        } catch (InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lint process interrupted or IO error", e);
        } finally {
            if (indexLocked) indexLock.unlock();
            if (logLocked) logLock.unlock();
        }
        
        return response;
    }

    private List<Path> getFilesToCheck(LintRequest request, String workspaceId) throws IOException {
        Path base = Paths.get(wikiBaseDir, workspaceId, "wiki");
        if (!Files.exists(base)) return Collections.emptyList();
        
        List<Path> allFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(base)) {
            allFiles = stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> !p.getFileName().toString().equals("index.md"))
                    .filter(p -> !p.getFileName().toString().equals("log.md"))
                    .collect(Collectors.toList());
        }
        
        if ("CHANGED_SUBGRAPH".equals(request.getScope()) && request.getSince() != null) {
            String cypher = "MATCH (c:Concept {workspaceId: $workspaceId}) WHERE c.updatedAt >= datetime($since) " +
                            "OPTIONAL MATCH (c)-[:RELATES_TO*0..2]->(dependent) " +
                            "RETURN DISTINCT coalesce(dependent.name, c.name) as name";
            
            Collection<Map<String, Object>> results = neo4jClient.query(cypher)
                .bind(workspaceId).to("workspaceId")
                .bind(request.getSince()).to("since")
                .fetch().all();
                
            Set<String> affectedSlugs = results.stream()
                .map(r -> (String) r.get("name"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
                
            return allFiles.stream()
                .filter(p -> affectedSlugs.contains(p.getFileName().toString().replace(".md", "")))
                .collect(Collectors.toList());
        }
        
        return allFiles;
    }

    private List<String> getIndexLinks(String workspaceId) throws IOException {
        Path indexPath = Paths.get(wikiBaseDir, workspaceId, "wiki", "index.md");
        if (!Files.exists(indexPath)) return new ArrayList<>();
        
        String content = Files.readString(indexPath);
        List<String> links = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            links.add(matcher.group(1));
        }
        return links;
    }

    private void addToIndex(String slug, String workspaceId) throws IOException {
        Path indexPath = Paths.get(wikiBaseDir, workspaceId, "wiki", "index.md");
        String entry = String.format("- [[%s]]: Auto-recovered orphan page\n", slug);
        if (!Files.exists(indexPath)) {
            Files.writeString(indexPath, "# Index\n\n" + entry, StandardOpenOption.CREATE);
        } else {
            Files.writeString(indexPath, entry, StandardOpenOption.APPEND);
        }
    }

    private boolean hasCompleteFrontmatter(String content) {
        if (!content.startsWith("---\n")) return false;
        return content.contains("title:") && content.contains("type:") && 
               content.contains("created_at:") && content.contains("updated_at:");
    }

    private String autoFixFrontmatter(String content, String slug) {
        String today = LocalDate.now().toString();
        if (!content.startsWith("---\n")) {
            String fm = String.format("---\ntitle: \"%s\"\ntype: concept\ncreated_at: %s\nupdated_at: %s\n---\n\n", 
                                      slug, today, today);
            return fm + content;
        }
        
        StringBuilder newFm = new StringBuilder();
        newFm.append("---\n");
        if (!content.contains("title:")) newFm.append("title: \"").append(slug).append("\"\n");
        if (!content.contains("type:")) newFm.append("type: concept\n");
        if (!content.contains("created_at:")) newFm.append("created_at: ").append(today).append("\n");
        if (!content.contains("updated_at:")) newFm.append("updated_at: ").append(today).append("\n");
        
        return content.replaceFirst("---\n", newFm.toString());
    }

    private List<String> findBrokenLinks(String content, List<Path> allFiles) {
        Set<String> allSlugs = allFiles.stream()
            .map(p -> p.getFileName().toString().replace(".md", ""))
            .collect(Collectors.toSet());
            
        List<String> broken = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String target = matcher.group(1);
            if (!allSlugs.contains(target)) {
                broken.add(target);
            }
        }
        return broken;
    }

    private boolean isStale(String content) {
        Pattern p = Pattern.compile("updated_at:\\s*([0-9]{4}-[0-9]{2}-[0-9]{2})");
        Matcher m = p.matcher(content);
        if (m.find()) {
            try {
                LocalDate date = LocalDate.parse(m.group(1));
                return ChronoUnit.DAYS.between(date, LocalDate.now()) > 90;
            } catch (Exception e) {
                // ignore parsing error
            }
        }
        return false;
    }

    private boolean hasContradiction(String slug, String workspaceId) {
        String cypher = "MATCH (c:Concept {name: $name, workspaceId: $workspaceId})-[:CONTRADICTS]-(other {workspaceId: $workspaceId}) RETURN count(other) > 0 as hasContradiction";
        try {
            Collection<Map<String, Object>> result = neo4jClient.query(cypher).bind(slug).to("name").bind(workspaceId).to("workspaceId").fetch().all();
            if (!result.isEmpty()) {
                Object val = result.iterator().next().get("hasContradiction");
                if (val instanceof Boolean) return (Boolean) val;
            }
        } catch (Exception e) {
            log.error("Error querying contradiction", e);
        }
        return false;
    }

    private void logLintEvent(String slug, String message, String workspaceId) {
        try {
            Path logPath = Paths.get(wikiBaseDir, workspaceId, "wiki", "log.md");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String logEntry = String.format("- [%s] [LINT] [[%s]]: %s\n", timestamp, slug, message);
            if (!Files.exists(logPath)) {
                Files.writeString(logPath, "# Log\n\n" + logEntry, StandardOpenOption.CREATE);
            } else {
                String content = Files.readString(logPath);
                Files.writeString(logPath, logEntry + content, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to write to log.md", e);
        }
    }
}
