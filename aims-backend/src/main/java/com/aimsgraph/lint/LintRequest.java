package com.aimsgraph.lint;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LintRequest {
    private String scope;
    private String since;
}
