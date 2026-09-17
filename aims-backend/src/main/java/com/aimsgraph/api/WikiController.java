package com.aimsgraph.api;

import com.aimsgraph.auth.JwtInterceptor;
import com.aimsgraph.domain.wiki.WikiService;
import java.io.IOException;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Slf4j
@RestController
@RequestMapping("/api/v1/wiki")
@RequiredArgsConstructor
public class WikiController {

  private final WikiService wikiService;

  @GetMapping("/{conceptName}")
  public ResponseEntity<?> getWikiContent(@PathVariable String conceptName) throws IOException {
    // 경로 조작 공격(Path Traversal) 방지를 위해 ../ 와 같은 문자만 차단하고, 파일명에 쓸 수 있는 문자는 최대한 허용합니다.
    if (conceptName == null
        || conceptName.isBlank()
        || conceptName.contains("..")
        || conceptName.contains("/")
        || conceptName.contains("\\")) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(
              Map.of(
                  "error",
                  "INVALID_CONCEPT_NAME",
                  "message",
                  "The concept name contains invalid characters."));
    }

    String workspaceId = "default-workspace";
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs != null) {
      String attrWorkspaceId =
          (String)
              attrs.getAttribute(
                  JwtInterceptor.WORKSPACE_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
      if (attrWorkspaceId != null && !attrWorkspaceId.isEmpty()) {
        workspaceId = attrWorkspaceId;
      }
    }

    String content = wikiService.getWikiContent(workspaceId, conceptName);
    return ResponseEntity.ok(new WikiResponse(content));
  }

  @Data
  public static class WikiResponse {
    private final String content;
  }
}
