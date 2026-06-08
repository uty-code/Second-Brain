package com.aimsgraph.domain.wiki;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WikiPage {
    private Long id;
    private String workspaceId;
    private String pagePath;
    private String title;
    private String pageType;
    private String contentHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
