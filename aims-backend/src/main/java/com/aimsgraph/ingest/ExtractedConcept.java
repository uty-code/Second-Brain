package com.aimsgraph.ingest;

import java.util.List;
import lombok.Data;

@Data
public class ExtractedConcept {
  private String name;
  private String title;
  private String type;
  private List<String> tags;
  private List<String> aliases;
  private String summary;
  private String content;
  private List<LinkedConcept> linkedConcepts;
}
