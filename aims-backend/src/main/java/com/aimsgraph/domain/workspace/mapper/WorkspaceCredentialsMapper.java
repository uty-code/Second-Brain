package com.aimsgraph.domain.workspace.mapper;

import com.aimsgraph.domain.workspace.WorkspaceCredentials;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkspaceCredentialsMapper {
    WorkspaceCredentials findByWorkspaceId(@Param("workspaceId") String workspaceId);
    void mergeCredentials(WorkspaceCredentials credentials);
}
