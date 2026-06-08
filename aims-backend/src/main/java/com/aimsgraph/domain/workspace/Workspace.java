package com.aimsgraph.domain.workspace;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workspace {
    private String id;
    private String name;

    private LocalDateTime createdAt;
}
