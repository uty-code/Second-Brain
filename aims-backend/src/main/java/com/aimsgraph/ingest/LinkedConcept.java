package com.aimsgraph.ingest;

import lombok.Data;

@Data
public class LinkedConcept {
    private String name;
    private String type; // e.g., EXTENDS, CONTRADICTS, DEPENDS_ON, EXPLAINS, RELATED_TO
}
