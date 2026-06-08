package com.aimsgraph.domain.workspace;

import com.aimsgraph.domain.workspace.mapper.WorkspaceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceMapper workspaceMapper;

    @InjectMocks
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        // 암호화 키 세팅 (Reflection 이용)
        ReflectionTestUtils.setField(workspaceService, "encryptionKey", "12345678901234567890123456789012");
    }

/*
    @Test
    void verifyAndSaveApiKey_ValidKey_SavesEncrypted() {
        // Arrange
        String workspaceId = "test-ws";
        String llmProvider = "OPENAI";
        String validKey = "sk-valid-key";
        
        WorkspaceService spyService = spy(workspaceService);
        doNothing().when(spyService).verifyApiKeyWithProvider(anyString(), anyString());

        when(workspaceMapper.findById(workspaceId)).thenReturn(null);

        // Act
        spyService.verifyAndSaveApiKey(workspaceId, llmProvider, validKey);

        // Assert
        verify(workspaceMapper).insertWorkspace(argThat(ws -> 
            ws.getId().equals(workspaceId) &&
            ws.getLlmProvider().equals(llmProvider) &&
            !ws.getEncryptedApiKey().contains("sk-valid")
        ));
    }

    @Test
    void verifyAndSaveApiKey_InvalidKey_ThrowsException() {
        // Arrange
        String workspaceId = "test-ws";
        String llmProvider = "OPENAI";
        String emptyKey = "";

        // Act & Assert
        assertThatThrownBy(() -> workspaceService.verifyAndSaveApiKey(workspaceId, llmProvider, emptyKey))
            .isInstanceOf(IllegalArgumentException.class);
            
        verify(workspaceMapper, never()).insertWorkspace(any());
        verify(workspaceMapper, never()).updateApiKey(anyString(), anyString(), anyString());
    }
*/
}
