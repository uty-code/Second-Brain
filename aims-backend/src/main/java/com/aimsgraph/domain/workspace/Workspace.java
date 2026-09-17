package com.aimsgraph.domain.workspace;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace {
  private String id;
  private String name;

  private LocalDateTime createdAt;
}
