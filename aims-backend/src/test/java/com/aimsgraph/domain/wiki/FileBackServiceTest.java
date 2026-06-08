package com.aimsgraph.domain.wiki;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileBackServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    private FileBackService fileBackService;
    
    private final String wikiBaseDir = "target/workspaces_test";

    @BeforeEach
    void setUp() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(eq(10L), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        
        fileBackService = new FileBackService(redissonClient, wikiBaseDir);
    }

    @Test
    void saveInsight_Success() throws Exception {
        String workspaceId = "ws-123";
        String title = "Test Insight";
        String content = "This is a test insight.";

        fileBackService.saveInsight(workspaceId, title, content);

        // Verify lock was used with workspace id
        verify(redissonClient).getLock("lock:wiki:" + workspaceId);
        verify(lock).tryLock(10L, 30L, TimeUnit.SECONDS);
        verify(lock).unlock();

        // Verify files were created
        Path workspaceWikiPath = Path.of(wikiBaseDir, workspaceId, "wiki");
        Path insightDir = workspaceWikiPath.resolve("insights");
        
        assertTrue(Files.exists(insightDir), "Insights directory should be created");
        
        // Find the newly created insight file
        boolean hasMdFile = Files.list(insightDir)
                                 .anyMatch(p -> p.toString().endsWith(".md"));
        assertTrue(hasMdFile, "A new markdown file should be created for the insight");
        
        // Check index.md and log.md
        assertTrue(Files.exists(workspaceWikiPath.resolve("index.md")));
        assertTrue(Files.exists(workspaceWikiPath.resolve("log.md")));
    }
}
