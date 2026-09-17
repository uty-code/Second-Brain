package com.aimsgraph.domain.workspace;

import com.aimsgraph.domain.workspace.mapper.WorkspaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

  private final WorkspaceMapper workspaceMapper;
}
