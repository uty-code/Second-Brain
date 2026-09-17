package com.aimsgraph.domain.wiki;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WikiPage {
  private Long id;
  private String workspaceId;
  private String pagePath;
  private String title;
  private String pageType;
  private String contentHash;
  private String content;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
