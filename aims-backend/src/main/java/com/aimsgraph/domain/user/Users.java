package com.aimsgraph.domain.user;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Users {
  private Long id;
  private String username;
  private String passwordHash;
  private String defaultWorkspaceId;
  private LocalDateTime createdAt;
}
