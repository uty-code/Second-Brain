package com.aimsgraph.domain.source;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RawSource {
  private Long id;
  private String workspaceId;
  private String sourceUri;
  private String title;
  private String contentHash;
  private String sourceType;
  private String status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
