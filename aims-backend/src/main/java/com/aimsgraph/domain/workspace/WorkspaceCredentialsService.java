package com.aimsgraph.domain.workspace;

import com.aimsgraph.domain.workspace.mapper.WorkspaceCredentialsMapper;
import com.aimsgraph.util.CryptoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceCredentialsService {

  private final WorkspaceCredentialsMapper credentialsMapper;
  private final CryptoUtils cryptoUtils;

  public WorkspaceCredentialsService(
      WorkspaceCredentialsMapper credentialsMapper, CryptoUtils cryptoUtils) {
    this.credentialsMapper = credentialsMapper;
    this.cryptoUtils = cryptoUtils;
  }

  public WorkspaceCredentials getCredentials(String workspaceId) {
    WorkspaceCredentials credentials = credentialsMapper.findByWorkspaceId(workspaceId);
    if (credentials == null) {
      return null;
    }

    // Decrypt the keys before returning
    credentials.setNotionApiKey(cryptoUtils.decrypt(credentials.getNotionApiKey()));
    credentials.setGithubApiKey(cryptoUtils.decrypt(credentials.getGithubApiKey()));
    credentials.setDeepseekApiKey(cryptoUtils.decrypt(credentials.getDeepseekApiKey()));

    return credentials;
  }

  public WorkspaceCredentials getRawCredentials(String workspaceId) {
    return credentialsMapper.findByWorkspaceId(workspaceId);
  }

  @Transactional
  public void updateCredentials(
      String workspaceId, String notionApiKey, String githubApiKey, String deepseekApiKey) {
    WorkspaceCredentials credentials = new WorkspaceCredentials();
    credentials.setWorkspaceId(workspaceId);

    if (notionApiKey != null) {
      credentials.setNotionApiKey(cryptoUtils.encrypt(notionApiKey));
    }
    if (githubApiKey != null) {
      credentials.setGithubApiKey(cryptoUtils.encrypt(githubApiKey));
    }
    if (deepseekApiKey != null) {
      credentials.setDeepseekApiKey(cryptoUtils.encrypt(deepseekApiKey));
    }

    credentialsMapper.mergeCredentials(credentials);
  }
}
