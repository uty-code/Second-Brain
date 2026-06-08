package com.aimsgraph.domain.wiki;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileBackService {

    private final RedissonClient redissonClient;
    private final String wikiBaseDir;

    public FileBackService(RedissonClient redissonClient, 
                           @Value("${wiki.base.dir:workspaces}") String wikiBaseDir) {
        this.redissonClient = redissonClient;
        this.wikiBaseDir = wikiBaseDir;
    }

    public String saveInsight(String workspaceId, String title, String content) {
        String lockKey = "lock:wiki:" + workspaceId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("Could not acquire lock for wiki file update");
            }

            Path wsPath = Path.of(wikiBaseDir, workspaceId, "wiki");
            Path insightsPath = wsPath.resolve("insights");
            
            if (!Files.exists(insightsPath)) {
                Files.createDirectories(insightsPath);
            }

            String fileName = UUID.randomUUID().toString() + ".md";
            Path insightFile = insightsPath.resolve(fileName);
            
            String formattedContent = "# " + title + "\n\n" + content;
            Files.writeString(insightFile, formattedContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            // Update index.md
            Path indexFile = wsPath.resolve("index.md");
            String indexEntry = String.format("- [%s](insights/%s)\n", title, fileName);
            if (!Files.exists(indexFile)) {
                Files.writeString(indexFile, "# Index\n\n" + indexEntry, StandardOpenOption.CREATE);
            } else {
                Files.writeString(indexFile, indexEntry, StandardOpenOption.APPEND);
            }

            // Update log.md
            Path logFile = wsPath.resolve("log.md");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logEntry = String.format("[%s] Insight added: %s\n", timestamp, title);
            if (!Files.exists(logFile)) {
                Files.writeString(logFile, "# Log\n\n" + logEntry, StandardOpenOption.CREATE);
            } else {
                Files.writeString(logFile, logEntry, StandardOpenOption.APPEND);
            }
            
            return fileName;
            
        } catch (InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save insight", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
