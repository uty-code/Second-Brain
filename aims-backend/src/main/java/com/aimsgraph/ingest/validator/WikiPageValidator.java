package com.aimsgraph.ingest.validator;

import com.aimsgraph.ingest.LlmService.StructuredWikiPage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class WikiPageValidator {

  private static final Pattern ID_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
  private static final Pattern CROSS_REF_PATTERN = Pattern.compile("\\[\\[([^\\]]*)\\]\\]");
  private static final Pattern PLACEHOLDER_PATTERN =
      Pattern.compile("(?i)\\b(TODO|TBD)\\b|\\.{3,}|\\(내용\\s*없음\\)|내용\\s*없음|\\[작성\\s*예정\\]");
  private static final Pattern FRONTMATTER_PATTERN =
      Pattern.compile("^---\\r?\\n([\\s\\S]*?)\\r?\\n---", Pattern.MULTILINE);

  public static final List<String> REQUIRED_FRONTMATTER_KEYS =
      List.of(
          "title:", "type:", "created_at:", "source_supported:", "general_knowledge_supplemented:");

  /** StructuredWikiPage에 대한 순수 검증 및 자기참조 정제를 수행합니다. LLM 호출 ❌, 파일/DB 저장 ❌ */
  public ValidationResult validate(StructuredWikiPage page, Set<String> availableConceptIds) {
    List<ValidationError> errors = new ArrayList<>();

    if (page == null) {
      errors.add(
          ValidationError.hard(
              ErrorType.EMPTY_CONTENT, "page", "StructuredWikiPage cannot be null"));
      return ValidationResult.failure(errors, null);
    }

    // 1. ID 형식 검증 (^[a-z0-9]+(?:-[a-z0-9]+)*$)
    String id = page.id();
    if (id == null || id.isBlank()) {
      errors.add(
          ValidationError.hard(ErrorType.INVALID_ID, "id", "Concept ID is missing or empty"));
    } else if (!ID_PATTERN.matcher(id.trim()).matches()) {
      errors.add(
          ValidationError.hard(
              ErrorType.INVALID_ID,
              "id",
              "Concept ID must be lowercase alphanumeric with single hyphens (e.g. 'event-driven-architecture'): "
                  + id));
    }

    // 2. 기본 필수 필드 검증
    if (page.title() == null || page.title().isBlank()) {
      errors.add(ValidationError.hard(ErrorType.MISSING_FIELD, "title", "Title is missing"));
    }
    if (page.type() == null || page.type().isBlank()) {
      errors.add(ValidationError.hard(ErrorType.MISSING_FIELD, "type", "Type is missing"));
    }
    if (page.summary() == null || page.summary().isBlank()) {
      errors.add(ValidationError.hard(ErrorType.MISSING_FIELD, "summary", "Summary is missing"));
    }
    if (page.content() == null || page.content().isBlank()) {
      errors.add(ValidationError.hard(ErrorType.EMPTY_CONTENT, "content", "Content is missing"));
      return ValidationResult.failure(errors, page);
    }

    // 3. Summary 길이 검증 (공백 제거 후 30~80자 엄격 계약)
    if (page.summary() != null) {
      String trimmedSummary = page.summary().trim();
      int len = trimmedSummary.length();
      if (len < 30 || len > 80) {
        errors.add(
            ValidationError.hard(
                ErrorType.SUMMARY_LENGTH_INVALID,
                "summary",
                "Summary length must be strictly between 30 and 80 characters (Current length: "
                    + len
                    + ")"));
      }
      // Summary placeholder 검사
      if (PLACEHOLDER_PATTERN.matcher(trimmedSummary).find()) {
        errors.add(
            ValidationError.hard(
                ErrorType.PLACEHOLDER_DETECTED,
                "summary",
                "Placeholder or incomplete marker detected in summary"));
      }
    }

    String content = page.content();

    // 4. Placeholder 검사 (본문 전체)
    if (PLACEHOLDER_PATTERN.matcher(content).find()) {
      errors.add(
          ValidationError.hard(
              ErrorType.PLACEHOLDER_DETECTED,
              "content",
              "Placeholder or incomplete marker (TODO, TBD, ..., etc.) detected in content"));
    }

    // 5. 본문 실질 텍스트 존재 검증 (헤더 라인 및 마크다운 서식 제외 후 의미있는 텍스트 확인)
    String plainText =
        content
            .replaceAll("(?m)^#{1,6}[^\r\n]*", "")
            .replaceAll("\\[\\[[^\\]]*\\]\\]", "")
            .replaceAll("[*_`>\\-]", "")
            .trim();

    if (plainText.isEmpty()) {
      errors.add(
          ValidationError.hard(
              ErrorType.BODY_EMPTY,
              "content",
              "Document body contains no meaningful text (only headers or markdown symbols found)"));
    }

    // 6. Cross-reference ([[...]]) 무결성 및 자기참조 sanitize
    String sanitizedContent = content;
    Matcher matcher = CROSS_REF_PATTERN.matcher(content);
    String currentId = page.id() != null ? page.id().trim().toLowerCase() : "";

    while (matcher.find()) {
      String rawSlug = matcher.group(1);
      String slug = rawSlug != null ? rawSlug.trim() : "";

      if (slug.isEmpty()) {
        errors.add(
            ValidationError.hard(
                ErrorType.INVALID_SLUG_FORMAT,
                "content",
                "Empty cross-reference link '[[]]' found"));
        continue;
      }

      // Case A: 자기참조 (Self-reference) -> Auto-Sanitize (볼드 텍스트로 치환)
      if (!currentId.isEmpty() && slug.equalsIgnoreCase(currentId)) {
        errors.add(
            ValidationError.sanitizable(
                ErrorType.SELF_REFERENCE,
                "content",
                "Self-reference detected to current concept [["
                    + slug
                    + "]]. Auto-sanitized to plain text."));
        String replacement = "**" + (page.title() != null ? page.title() : slug) + "**";
        sanitizedContent = sanitizedContent.replace("[[" + rawSlug + "]]", replacement);
        continue;
      }

      // Case B: 비정상 slug 문법 (한글, 대문자, 공백 등) -> Hard Fail (Retry 대상)
      if (!ID_PATTERN.matcher(slug).matches()) {
        errors.add(
            ValidationError.hard(
                ErrorType.INVALID_SLUG_FORMAT,
                "content",
                "Invalid cross-reference slug format '[["
                    + rawSlug
                    + "]]'. Must be lowercase english-slug with hyphens."));
        continue;
      }

      // Case C: 존재하지 않는 노드 참조 (Dangling Reference) -> Hard Fail (절대 숨기지 않음!)
      if (availableConceptIds != null && !availableConceptIds.contains(slug.toLowerCase())) {
        errors.add(
            ValidationError.hard(
                ErrorType.UNKNOWN_REFERENCE,
                "content",
                "Unknown cross-reference '[["
                    + slug
                    + "]]'. Concept ID does NOT exist in available concepts."));
      }
    }

    // 결과 빌드
    StructuredWikiPage finalPage =
        new StructuredWikiPage(
            page.id(),
            page.title(),
            page.type(),
            page.summary(),
            page.tags(),
            page.aliases(),
            sanitizedContent,
            page.relatedConcepts());

    boolean hasHard = errors.stream().anyMatch(e -> !e.sanitizable());
    if (hasHard) {
      return ValidationResult.failure(errors, finalPage);
    } else if (!errors.isEmpty()) {
      return ValidationResult.sanitized(finalPage, errors);
    } else {
      return ValidationResult.success(finalPage);
    }
  }

  /** 최종 마크다운 헤더의 YAML Frontmatter 구조 및 필수 키 유효성을 검증합니다. */
  public ValidationResult validateFrontmatter(String markdown) {
    List<ValidationError> errors = new ArrayList<>();
    if (markdown == null || markdown.isBlank()) {
      errors.add(
          ValidationError.hard(ErrorType.EMPTY_CONTENT, "markdown", "Markdown content is empty"));
      return ValidationResult.failure(errors, null);
    }

    Matcher fmMatcher = FRONTMATTER_PATTERN.matcher(markdown);
    if (!fmMatcher.find()) {
      errors.add(
          ValidationError.hard(
              ErrorType.INVALID_FRONTMATTER,
              "frontmatter",
              "YAML frontmatter delimiter ('---') not found at the beginning of the markdown"));
      return ValidationResult.failure(errors, null);
    }

    String frontmatter = fmMatcher.group(1);
    for (String reqKey : REQUIRED_FRONTMATTER_KEYS) {
      if (!frontmatter.contains(reqKey)) {
        errors.add(
            ValidationError.hard(
                ErrorType.INVALID_FRONTMATTER,
                "frontmatter",
                "Missing required frontmatter key: '" + reqKey + "'"));
      }
    }

    if (!errors.isEmpty()) {
      return ValidationResult.failure(errors, null);
    }
    return ValidationResult.success(null);
  }

  /** 마크다운 전문(YAML Frontmatter + 본문)에 대해 통합 구조 및 내용 검증을 수행합니다. */
  public ValidationResult validateMarkdownDocument(
      String markdown, String fallbackId, String fallbackTitle, Set<String> availableIds) {
    if (markdown == null || markdown.isBlank()) {
      return ValidationResult.failure(
          List.of(ValidationError.hard(ErrorType.EMPTY_CONTENT, "markdown", "Markdown is empty")),
          null);
    }

    Matcher fmM = FRONTMATTER_PATTERN.matcher(markdown);
    String id = fallbackId;
    String title = fallbackTitle;
    String type = "concept";
    String summary = "";
    String content = markdown;
    List<String> tags = new ArrayList<>();
    List<String> aliases = new ArrayList<>();

    if (fmM.find()) {
      String frontmatter = fmM.group(1);
      content = markdown.substring(fmM.end()).trim();

      for (String line : frontmatter.split("\\r?\\n")) {
        String trimmed = line.trim();
        if (trimmed.startsWith("id:")) {
          id = trimmed.substring(3).trim();
        } else if (trimmed.startsWith("title:")) {
          title = trimmed.substring(6).trim();
        } else if (trimmed.startsWith("type:")) {
          type = trimmed.substring(5).trim();
        } else if (trimmed.startsWith("summary:")) {
          summary = trimmed.substring(8).trim();
        }
      }
    }

    if (summary.isBlank()) {
      String cleanedContent = content.replaceAll("(?m)^#+.*$", "").trim();
      summary = cleanedContent.length() > 60 ? cleanedContent.substring(0, 60) : cleanedContent;
      if (summary.length() < 30) {
        summary = (title != null ? title : "Wiki Concept") + "에 대한 상세 위키 문서 및 지식 체계 설명입니다.";
      }
    }

    StructuredWikiPage page =
        new StructuredWikiPage(id, title, type, summary, tags, aliases, content, List.of());

    return validate(page, availableIds);
  }
}
