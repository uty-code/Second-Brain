package com.aimsgraph.lint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import com.aimsgraph.auth.JwtInterceptor;

@RestController
@RequestMapping("/api/internal/lint")
@RequiredArgsConstructor
public class LintController {

    private final LintService lintService;

    @PostMapping
    public ResponseEntity<LintResponse> lint(@RequestBody LintRequest request) {
        String workspaceId = (String) RequestContextHolder.currentRequestAttributes()
                .getAttribute(JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);

        if (workspaceId == null || workspaceId.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        LintResponse response = lintService.performLint(request, workspaceId);
        return ResponseEntity.ok(response);
    }
}
