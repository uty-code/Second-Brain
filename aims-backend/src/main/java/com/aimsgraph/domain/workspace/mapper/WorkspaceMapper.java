package com.aimsgraph.domain.workspace.mapper;

import com.aimsgraph.domain.workspace.Workspace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WorkspaceMapper {
    void insertWorkspace(Workspace workspace);

    Workspace findById(@Param("id") String id);
}
