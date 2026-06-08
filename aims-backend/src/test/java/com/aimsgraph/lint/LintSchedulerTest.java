package com.aimsgraph.lint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class LintSchedulerTest {

    @Mock
    private LintService lintService;

    @InjectMocks
    private LintScheduler lintScheduler;

    @Test
    void shouldRunDaemonAndCallLintService() throws Exception {
        // Arrange
        java.nio.file.Path tempBase = java.nio.file.Files.createTempDirectory("workspaces");
        java.nio.file.Files.createDirectories(tempBase.resolve("test-workspace"));
        org.springframework.test.util.ReflectionTestUtils.setField(lintScheduler, "wikiBaseDir", tempBase.toString());

        LintResponse mockResponse = new LintResponse();
        mockResponse.setIssuesFound(0);
        mockResponse.setAutoFixed(0);
        given(lintService.performLint(any(LintRequest.class), any(String.class))).willReturn(mockResponse);

        // Act
        lintScheduler.runDaemon();

        // Assert
        verify(lintService).performLint(any(LintRequest.class), any(String.class));
    }
}
