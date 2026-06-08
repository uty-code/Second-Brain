package com.aimsgraph.lint;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.aimsgraph.api.NotificationController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LintScheduler {

    private final LintService lintService;
    
    @org.springframework.beans.factory.annotation.Value("${wiki.base.dir:workspaces}")
    private String wikiBaseDir;

    // Run every day at 3 AM or every 5 minutes for testing? Let's say every hour.
    @Scheduled(cron = "0 0 * * * *")
    public void runDaemon() {
        log.info("Starting scheduled Lint Daemon...");
        LintRequest request = new LintRequest();
        request.setScope("CHANGED_SUBGRAPH");
        // For example, check since 1 hour ago
        request.setSince(LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        try {
            java.nio.file.Path base = java.nio.file.Paths.get(wikiBaseDir);
            if (java.nio.file.Files.exists(base)) {
                try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(base)) {
                    stream.filter(java.nio.file.Files::isDirectory).forEach(wsDir -> {
                        String workspaceId = wsDir.getFileName().toString();
                        try {
                            LintResponse response = lintService.performLint(request, workspaceId);
                            log.info("Lint completed for workspace {}. Issues found: {}, Auto-fixed: {}", workspaceId, response.getIssuesFound(), response.getAutoFixed());
                            
                            if (response.getIssuesFound() > response.getAutoFixed()) {
                                NotificationController.broadcastNotification(workspaceId, "lint_error", "Lint issues found: " + (response.getIssuesFound() - response.getAutoFixed()) + " unresolved.");
                            }
                        } catch (Exception e) {
                            log.error("Error running Lint Daemon for workspace " + workspaceId, e);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("Error accessing workspaces for Lint Daemon", e);
        }
    }
}
