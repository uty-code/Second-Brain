package com.aimsgraph.ingest.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimsgraph.ingest.LlmService.StructuredWikiPage;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class WikiPageValidatorTest {

  private WikiPageValidator validator;
  private Set<String> availableIds;

  @BeforeEach
  void setUp() {
    validator = new WikiPageValidator();
    availableIds = Set.of("message-broker", "kafka", "event-driven-architecture", "cqrs");
  }

  private String createValidContent() {
    return "## 개요\n"
        + "이벤트 드리븐 아키텍처는 분산 시스템 간 비동기 메시지를 통해 결합도를 낮추는 핵심 설계 패턴입니다.\n\n"
        + "## 핵심 컴포넌트\n"
        + "| 컴포넌트 | 역할 |\n"
        + "|---|---|\n"
        + "| Producer | 이벤트를 발행하는 주체 |\n"
        + "| [[message-broker]] | 이벤트를 라우팅하고 보관하는 미들웨어 |\n\n"
        + "## 트레이드오프 및 고려사항\n"
        + "컴포넌트 간 결합도를 낮추고 확장성을 얻을 수 있지만 최종 일관성 관리와 분산 트레이싱 난이도가 증가합니다.";
  }

  @Test
  @DisplayName("정상 페이지: 자유로운 섹션 구조, 유효한 ID, 적절한 길이의 summary, 유효한 참조가 모두 충족되면 통과")
  void validate_validPage_shouldPass() {
    // 35자 summary
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    StructuredWikiPage page =
        new StructuredWikiPage(
            "event-driven-architecture",
            "이벤트 드리븐 아키텍처",
            "concept",
            summary,
            List.of("아키텍처", "비동기"),
            List.of("EDA"),
            createValidContent(),
            List.of("message-broker"));

    ValidationResult result = validator.validate(page, availableIds);

    assertTrue(result.valid());
    assertFalse(result.hasHardFailures());
    assertEquals(0, result.errors().size());
  }

  @Test
  @DisplayName("Summary 경계값 테스트: 29자(Fail) / 30자(Pass) / 80자(Pass) / 81자(Fail)")
  void validate_summaryBoundaries() {
    // 29자
    String s29 = "일이삼사오육칠팔구십일이삼사오육칠팔구십일이삼사오육칠팔구";
    assertEquals(29, s29.length());

    // 30자
    String s30 = "일이삼사오육칠팔구십일이삼사오육칠팔구십일이삼사오육칠팔구십";
    assertEquals(30, s30.length());

    // 80자
    String s80 = "가".repeat(80);
    assertEquals(80, s80.length());

    // 81자
    String s81 = "가".repeat(81);
    assertEquals(81, s81.length());

    String content = createValidContent();

    // 29자 -> FAIL
    StructuredWikiPage page29 =
        new StructuredWikiPage(
            "eda", "EDA", "concept", s29, List.of(), List.of(), content, List.of());
    ValidationResult r29 = validator.validate(page29, availableIds);
    assertFalse(r29.valid());
    assertTrue(r29.hasHardFailures());
    assertTrue(r29.errors().stream().anyMatch(e -> e.type() == ErrorType.SUMMARY_LENGTH_INVALID));

    // 30자 -> PASS
    StructuredWikiPage page30 =
        new StructuredWikiPage(
            "eda", "EDA", "concept", s30, List.of(), List.of(), content, List.of());
    ValidationResult r30 = validator.validate(page30, availableIds);
    assertTrue(r30.valid());

    // 80자 -> PASS
    StructuredWikiPage page80 =
        new StructuredWikiPage(
            "eda", "EDA", "concept", s80, List.of(), List.of(), content, List.of());
    ValidationResult r80 = validator.validate(page80, availableIds);
    assertTrue(r80.valid());

    // 81자 -> FAIL
    StructuredWikiPage page81 =
        new StructuredWikiPage(
            "eda", "EDA", "concept", s81, List.of(), List.of(), content, List.of());
    ValidationResult r81 = validator.validate(page81, availableIds);
    assertFalse(r81.valid());
    assertTrue(r81.hasHardFailures());
    assertTrue(r81.errors().stream().anyMatch(e -> e.type() == ErrorType.SUMMARY_LENGTH_INVALID));
  }

  @Test
  @DisplayName("ID 형식 검증: 올바르지 않은 영문 슬러그(한글, 공백, 대문자, 밑줄)는 INVALID_ID Fail")
  void validate_idFormats() {
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    String content = createValidContent();

    List<String> invalidIds =
        List.of(
            "EventDriven", "event_driven", "이벤트아키텍처", "event driven", "-event-", "event--driven");

    for (String invalidId : invalidIds) {
      StructuredWikiPage page =
          new StructuredWikiPage(
              invalidId, "Title", "concept", summary, List.of(), List.of(), content, List.of());
      ValidationResult res = validator.validate(page, availableIds);
      assertFalse(res.valid(), "ID should be invalid: " + invalidId);
      assertTrue(res.errors().stream().anyMatch(e -> e.type() == ErrorType.INVALID_ID));
    }
  }

  @Test
  @DisplayName("본문 실질 텍스트 검증: 헤더나 마크다운 서식 기호만 있고 실질 텍스트가 없으면 BODY_EMPTY Fail")
  void validate_emptyBodyContent_shouldFail() {
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";

    List<String> emptyBodies =
        List.of("## 정의", "## 정의\n### 핵심 내용\n", "## 헤더\n- \n> \n** ** ` `", "   \n\n## 소제목\n   ");

    for (String emptyBody : emptyBodies) {
      StructuredWikiPage page =
          new StructuredWikiPage(
              "event-driven-architecture",
              "EDA",
              "concept",
              summary,
              List.of(),
              List.of(),
              emptyBody,
              List.of());

      ValidationResult result = validator.validate(page, availableIds);
      assertFalse(result.valid(), "Body should be empty for: " + emptyBody);
      assertTrue(
          result.errors().stream().anyMatch(e -> e.type() == ErrorType.BODY_EMPTY),
          "Should contain BODY_EMPTY error");
    }
  }

  @Test
  @DisplayName("소제목 1개만 있어도 유효한 실질 내용이 있으면 통과")
  void validate_singleSectionDoc_shouldPass() {
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    String singleSectionContent =
        "## 핵심 개념\n"
            + "HTTP 404는 요청한 리소스를 서버에서 찾을 수 없음을 나타내는 표준 HTTP 상태 코드입니다. "
            + "클라이언트가 잘못된 URL을 요청했거나 리소스가 삭제되었을 때 주로 발생합니다.";

    StructuredWikiPage page =
        new StructuredWikiPage(
            "http-404",
            "HTTP 404",
            "concept",
            summary,
            List.of(),
            List.of(),
            singleSectionContent,
            List.of());

    ValidationResult result = validator.validate(page, availableIds);
    assertTrue(result.valid());
    assertFalse(result.hasHardFailures());
  }

  @Test
  @DisplayName("소제목(##)이 없는 순수 텍스트 문서도 유효한 내용이 있으면 통과")
  void validate_noHeadingDoc_shouldPass() {
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    String plainDocContent =
        "HTTP 404는 요청한 리소스를 찾을 수 없음을 나타내는 가장 대표적인 클라이언트 오류 상태 코드입니다. "
            + "REST API 설계 시 부재 리소스에 대한 응답으로 표준적으로 사용됩니다.";

    StructuredWikiPage page =
        new StructuredWikiPage(
            "http-404",
            "HTTP 404",
            "concept",
            summary,
            List.of(),
            List.of(),
            plainDocContent,
            List.of());

    ValidationResult result = validator.validate(page, availableIds);
    assertTrue(result.valid());
    assertFalse(result.hasHardFailures());
  }

  @Test
  @DisplayName("수학·과학·인문학 등 다양한 도메인의 자유로운 소제목 구조도 모두 통과")
  void validate_diverseHeadingStructure_shouldPass() {
    String summary = "직각삼각형의 세 변 사이의 기하학적 관계를 나타내는 유클리드 기하학의 기본 정리";
    String mathContent =
        "## 정리 명제\n"
            + "직각삼각형에서 빗변의 제곱은 다른 두 변의 제곱의 합과 같다 ($a^2 + b^2 = c^2$).\n\n"
            + "## 증명\n"
            + "유클리드의 풍차 그림 증명 및 대수적 정사각형 면적 분할법 등 수백 가지 증명이 존재합니다.\n\n"
            + "## 실생활 응용\n"
            + "항법, 컴퓨터 그래픽스, 건축 공학 등 현대 기하 연산의 필수 기초 원리입니다.";

    StructuredWikiPage page =
        new StructuredWikiPage(
            "pythagorean-theorem",
            "피타고라스 정리",
            "concept",
            summary,
            List.of("수학"),
            List.of(),
            mathContent,
            List.of());

    ValidationResult result = validator.validate(page, availableIds);
    assertTrue(result.valid());
    assertFalse(result.hasHardFailures());
  }

  @Test
  @DisplayName("Placeholder 검증: ..., TODO, TBD, (내용 없음), [작성 예정] 감지 시 PLACEHOLDER_DETECTED Fail")
  void validate_placeholder_shouldFail() {
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    String placeholderContent =
        createValidContent().replace("이벤트를 라우팅하고 보관하는 미들웨어", "TODO: 추후 작성 예정...");

    StructuredWikiPage page =
        new StructuredWikiPage(
            "eda", "EDA", "concept", summary, List.of(), List.of(), placeholderContent, List.of());

    ValidationResult result = validator.validate(page, availableIds);
    assertFalse(result.valid());
    assertTrue(result.errors().stream().anyMatch(e -> e.type() == ErrorType.PLACEHOLDER_DETECTED));
  }

  @Test
  @DisplayName(
      "자기참조 검증: 자기 자신 [[event-driven-architecture]]는 sanitizedPage에서 볼드로 안전하게 치환되고 Hard Fail은 아님")
  void validate_selfReference_shouldSanitize() {
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    // 자기 자신 링크 [[event-driven-architecture]] 포함
    String selfRefContent =
        createValidContent() + "\n\n참고로 [[event-driven-architecture]]는 MSA의 기본입니다.";

    StructuredWikiPage page =
        new StructuredWikiPage(
            "event-driven-architecture",
            "이벤트 드리븐 아키텍처",
            "concept",
            summary,
            List.of(),
            List.of(),
            selfRefContent,
            List.of());

    ValidationResult result = validator.validate(page, availableIds);

    // Hard Fail은 없어야 함
    assertFalse(result.hasHardFailures());
    assertTrue(result.valid());
    // 경고에 SELF_REFERENCE 기록
    assertTrue(
        result.errors().stream()
            .anyMatch(e -> e.type() == ErrorType.SELF_REFERENCE && e.sanitizable()));
    // sanitizedPage의 본문에서 [[event-driven-architecture]]가 제거되었는지 확인
    assertNotNull(result.sanitizedPage());
    assertFalse(result.sanitizedPage().content().contains("[[event-driven-architecture]]"));
    assertTrue(
        result.sanitizedPage().content().contains("**이벤트 드리븐 아키텍처**")
            || result.sanitizedPage().content().contains("이벤트 드리븐 아키텍처"));
  }

  @Test
  @DisplayName(
      "Dangling Reference 검증: availableIds에 없는 [[unknown-concept]]는 절대 sanitize하지 않고 UNKNOWN_REFERENCE Hard Fail")
  void validate_danglingReference_unknownConcept_shouldFail() {
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    // availableIds에 없는 [[distributed-consensus]] 포함
    String danglingContent =
        createValidContent() + "\n\n합의 알고리즘으로는 [[distributed-consensus]] 기법이 쓰입니다.";

    StructuredWikiPage page =
        new StructuredWikiPage(
            "event-driven-architecture",
            "이벤트 드리븐 아키텍처",
            "concept",
            summary,
            List.of(),
            List.of(),
            danglingContent,
            List.of());

    ValidationResult result = validator.validate(page, availableIds);

    assertFalse(result.valid());
    assertTrue(result.hasHardFailures());
    assertTrue(
        result.errors().stream()
            .anyMatch(e -> e.type() == ErrorType.UNKNOWN_REFERENCE && !e.sanitizable()));
  }

  @Test
  @DisplayName("Cross-reference 문법 검증: 한글, 공백, 대문자 등 비규격 링크는 INVALID_SLUG_FORMAT Hard Fail")
  void validate_crossReference_invalidSyntax_shouldFail() {
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    String invalidSyntaxContent =
        createValidContent() + "\n\n참조: [[한글개념]], [[UPPER-CASE]], [[with space]]";

    StructuredWikiPage page =
        new StructuredWikiPage(
            "event-driven-architecture",
            "이벤트 드리븐 아키텍처",
            "concept",
            summary,
            List.of(),
            List.of(),
            invalidSyntaxContent,
            List.of());

    ValidationResult result = validator.validate(page, availableIds);

    assertFalse(result.valid());
    assertTrue(result.hasHardFailures());
    assertTrue(result.errors().stream().anyMatch(e -> e.type() == ErrorType.INVALID_SLUG_FORMAT));
  }

  @Test
  @DisplayName("Frontmatter 검증: 완성된 마크다운 앞단의 YAML 헤더 필수 필드가 모두 올바르면 통과")
  void validate_frontmatter_validMarkdown_shouldPass() {
    String validMarkdown =
        "---\n"
            + "title: 이벤트 드리븐 아키텍처\n"
            + "type: concept\n"
            + "created_at: 2026-09-17\n"
            + "source_supported: true\n"
            + "general_knowledge_supplemented: true\n"
            + "tags:\n"
            + "  - 아키텍처\n"
            + "---\n\n"
            + createValidContent();

    ValidationResult res = validator.validateFrontmatter(validMarkdown);
    assertTrue(res.valid());
    assertEquals(0, res.errors().size());
  }

  @Test
  @DisplayName("Frontmatter 검증: 필수 필드 누락 또는 문법 오류 시 INVALID_FRONTMATTER Fail")
  void validate_frontmatter_invalidMarkdown_shouldFail() {
    // source_supported 누락
    String brokenMarkdown =
        "---\n"
            + "title: 이벤트 드리븐 아키텍처\n"
            + "type: concept\n"
            + "created_at: 2026-09-17\n"
            + "---\n\n"
            + createValidContent();

    ValidationResult res = validator.validateFrontmatter(brokenMarkdown);
    assertFalse(res.valid());
    assertTrue(res.errors().stream().anyMatch(e -> e.type() == ErrorType.INVALID_FRONTMATTER));
  }
}
