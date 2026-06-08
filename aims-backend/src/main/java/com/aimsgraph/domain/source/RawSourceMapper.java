package com.aimsgraph.domain.source;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RawSourceMapper {
    void insert(RawSource rawSource);
    RawSource findByWorkspaceIdAndUri(@Param("workspaceId") String workspaceId, @Param("sourceUri") String sourceUri);
}
