package com.aimsgraph.domain.workspace;

import com.aimsgraph.domain.workspace.mapper.WorkspaceCredentialsMapper;
import com.aimsgraph.util.CryptoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceCredentialsService {

    private final WorkspaceCredentialsMapper credentialsMapper;

    public WorkspaceCredentialsService(WorkspaceCredentialsMapper credentialsMapper) {
        this.credentialsMapper = credentialsMapper;
    }

    public WorkspaceCredentials getCredentials(String workspaceId) {
        WorkspaceCredentials credentials = credentialsMapper.findByWorkspaceId(workspaceId);
        if (credentials == null) {
            return null;
        }
        
        // Decrypt the keys before returning
        credentials.setNotionApiKey(CryptoUtils.decrypt(credentials.getNotionApiKey()));
        credentials.setGithubApiKey(CryptoUtils.decrypt(credentials.getGithubApiKey()));
        credentials.setDeepseekApiKey(CryptoUtils.decrypt(credentials.getDeepseekApiKey()));
        
        return credentials;
    }
    
    public WorkspaceCredentials getRawCredentials(String workspaceId) {
        return credentialsMapper.findByWorkspaceId(workspaceId);
    }

    @Transactional
    public void updateCredentials(String workspaceId, String notionApiKey, String githubApiKey, String deepseekApiKey) {
        WorkspaceCredentials credentials = new WorkspaceCredentials();
        credentials.setWorkspaceId(workspaceId);
        
        if (notionApiKey != null) {
            credentials.setNotionApiKey(CryptoUtils.encrypt(notionApiKey));
        }
        if (githubApiKey != null) {
            credentials.setGithubApiKey(CryptoUtils.encrypt(githubApiKey));
        }
        if (deepseekApiKey != null) {
            credentials.setDeepseekApiKey(CryptoUtils.encrypt(deepseekApiKey));
        }
        
        credentialsMapper.mergeCredentials(credentials);
    }
}
