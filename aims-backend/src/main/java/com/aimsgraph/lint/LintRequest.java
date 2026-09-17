package com.aimsgraph.lint;

import lombok.Data;

@Data
public class LintRequest {
  private String scope;
  private String since;
}
