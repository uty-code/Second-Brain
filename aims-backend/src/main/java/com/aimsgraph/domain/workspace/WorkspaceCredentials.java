package com.aimsgraph.domain.workspace;

import java.time.LocalDateTime;

public class WorkspaceCredentials {
    private String workspaceId;
    private String notionApiKey;
    private String githubApiKey;
    private String deepseekApiKey;
    private LocalDateTime updatedAt;

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getNotionApiKey() {
        return notionApiKey;
    }

    public void setNotionApiKey(String notionApiKey) {
        this.notionApiKey = notionApiKey;
    }

    public String getGithubApiKey() {
        return githubApiKey;
    }

    public void setGithubApiKey(String githubApiKey) {
        this.githubApiKey = githubApiKey;
    }

    public String getDeepseekApiKey() {
        return deepseekApiKey;
    }

    public void setDeepseekApiKey(String deepseekApiKey) {
        this.deepseekApiKey = deepseekApiKey;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
