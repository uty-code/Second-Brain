package com.aimsgraph.lint;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class LintResponse {
    @JsonProperty("issues_found")
    private int issuesFound;

    @JsonProperty("auto_fixed")
    private int autoFixed;

    @JsonProperty("requires_review")
    private List<LintIssue> requiresReview = new ArrayList<>();
}
