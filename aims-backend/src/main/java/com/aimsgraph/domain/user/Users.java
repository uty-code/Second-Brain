package com.aimsgraph.domain.user;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Users {
    private Long id;
    private String username;
    private String passwordHash;
    private String defaultWorkspaceId;
    private LocalDateTime createdAt;
}
