package com.aimsgraph.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimsgraph.domain.workspace.WorkspaceCredentialsService;
import com.aimsgraph.domain.workspace.WorkspaceService;
import com.aimsgraph.ingest.validator.WikiPageValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class WikiGenerationPipelineTest {

  @Mock private WorkspaceService workspaceService;
  @Mock private WorkspaceCredentialsService credentialsService;
  @Mock private Neo4jClient neo4jClient;
  @Mock private NotionIngestService notionIngestService;

  @TempDir Path tempWikiDir;

  private HttpServer mockHttpServer;
  private int mockServerPort;
  private LlmService llmService;
  private WikiPageValidator validator;
  private ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws Exception {
    validator = new WikiPageValidator();
    llmService =
        new LlmService(
            workspaceService, credentialsService, neo4jClient, notionIngestService, validator);

    ReflectionTestUtils.setField(
        llmService, "wikiBaseDir", tempWikiDir.toAbsolutePath().toString());
    ReflectionTestUtils.setField(llmService, "defaultApiKey", "test-key");

    mockHttpServer = HttpServer.create(new InetSocketAddress(0), 0);
    mockServerPort = mockHttpServer.getAddress().getPort();
    ReflectionTestUtils.setField(
        llmService, "openAiApiUrl", "http://127.0.0.1:" + mockServerPort + "/v1/chat/completions");
  }

  @AfterEach
  void tearDown() {
    if (mockHttpServer != null) {
      mockHttpServer.stop(0);
    }
  }

  private String buildOpenAiResponseJson(String wikiJson) {
    try {
      Map<String, Object> resp =
          Map.of(
              "choices",
              List.of(Map.of("message", Map.of("role", "assistant", "content", wikiJson))));
      return mapper.writeValueAsString(resp);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String createValidWikiJson(String id, String title) {
    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    String content =
        "## 개요\n이벤트 드리븐 아키텍처는 비동기 메시징을 통해 서비스 간 결합도를 낮추는 아키텍처입니다.\n\n"
            + "## 핵심 컴포넌트\n- Producer: 이벤트 생성\n- Consumer: 이벤트 수신\n\n"
            + "## 아키텍처 고려사항\n높은 결합도 해소 및 확장성을 극대화하지만 분산 트랜잭션 관리가 요구됩니다.";

    return "{\"id\":\""
        + id
        + "\",\"title\":\""
        + title
        + "\",\"type\":\"concept\","
        + "\"summary\":\""
        + summary
        + "\",\"tags\":[\"architecture\"],\"aliases\":[\"EDA\"],"
        + "\"content\":\""
        + content.replace("\n", "\\n")
        + "\",\"relatedConcepts\":[]}";
  }

  @Test
  @DisplayName("Case 1: 1차 실패 (본문 실질 텍스트 누락 BODY_EMPTY) -> 2차 재시도 성공 -> 최종 저장 성공 (총 2회 호출)")
  void pipeline_case1_retryOnceSuccess() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);

    // 1차 응답: 헤더만 있고 실질 텍스트 없음 (BODY_EMPTY)
    String brokenJson =
        "{\"id\":\"event-driven-architecture\",\"title\":\"이벤트 드리븐 아키텍처\",\"type\":\"concept\","
            + "\"summary\":\"이벤트 기반 비동기 메시지 교환을 통해 결합도를 최소화하는 패턴\","
            + "\"tags\":[],\"aliases\":[],\"content\":\"## 개요\\n## 세부사항\\n\",\"relatedConcepts\":[]}";
    String validJson = createValidWikiJson("event-driven-architecture", "이벤트 드리븐 아키텍처");

    mockHttpServer.createContext(
        "/v1/chat/completions",
        exchange -> {
          int call = callCount.incrementAndGet();
          String responseBody =
              (call == 1)
                  ? buildOpenAiResponseJson(brokenJson)
                  : buildOpenAiResponseJson(validJson);
          byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    mockHttpServer.start();

    List<Map<String, Object>> nodes =
        List.of(
            Map.of(
                "id", "event-driven-architecture",
                "name", "이벤트 드리븐 아키텍처",
                "snippet", "EDA는 분산 시스템의 핵심이다."));

    Map<String, Object> graphData = Map.of("nodes", nodes, "links", List.of());
    llmService.generateWikiPages(graphData, "원문 텍스트입니다.", "tenant-test", "gpt-4o-mini");

    assertEquals(2, callCount.get(), "최초 1회 + 재시도 1회로 총 2회 호출되어야 함");

    Path savedFile = tempWikiDir.resolve("tenant-test/wiki/concepts/event-driven-architecture.md");
    assertTrue(Files.exists(savedFile), "재시도 후 성공하여 최종 마크다운 파일이 생성되어야 함");
    String savedContent = Files.readString(savedFile);
    assertTrue(savedContent.contains("## 핵심 컴포넌트"));
  }

  @Test
  @DisplayName("Case 2: 1차 실패 -> 2차 실패 -> 3차 성공 -> 최종 저장 성공 (총 3회 호출)")
  void pipeline_case2_retryTwiceSuccess() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);

    String brokenJson =
        "{\"id\":\"event-driven-architecture\",\"title\":\"EDA\",\"type\":\"concept\","
            + "\"summary\":\"너무 짧음\"," // summary 30자 미만
            + "\"tags\":[],\"aliases\":[],\"content\":\"## 개요\\n설명\\n\\n## 세부내용\\n내용\",\"relatedConcepts\":[]}";
    String validJson = createValidWikiJson("event-driven-architecture", "이벤트 드리븐 아키텍처");

    mockHttpServer.createContext(
        "/v1/chat/completions",
        exchange -> {
          int call = callCount.incrementAndGet();
          String responseBody =
              (call < 3) ? buildOpenAiResponseJson(brokenJson) : buildOpenAiResponseJson(validJson);
          byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    mockHttpServer.start();

    List<Map<String, Object>> nodes =
        List.of(
            Map.of(
                "id", "event-driven-architecture",
                "name", "이벤트 드리븐 아키텍처",
                "snippet", "EDA 설명"));

    Map<String, Object> graphData = Map.of("nodes", nodes, "links", List.of());
    llmService.generateWikiPages(graphData, "원문 텍스트", "tenant-test", "gpt-4o-mini");

    assertEquals(3, callCount.get(), "최초 1회 + 재시도 2회로 총 3회 호출되어야 함");

    Path savedFile = tempWikiDir.resolve("tenant-test/wiki/concepts/event-driven-architecture.md");
    assertTrue(Files.exists(savedFile), "3회차에 성공하여 최종 마크다운 파일이 생성되어야 함");
  }

  @Test
  @DisplayName("Case 3: 3회 모두 실패 (Dangling Link 또는 규격 미달) -> Reject (저장 안 됨, 총 3회 호출)")
  void pipeline_case3_rejectAfterThreeFailures() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);

    // availableConcepts에 없는 [[unknown-cluster]] 영구 참조 (Dangling Reference Hard Fail)
    String brokenJson =
        "{\"id\":\"event-driven-architecture\",\"title\":\"EDA\",\"type\":\"concept\","
            + "\"summary\":\"이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴\","
            + "\"tags\":[],\"aliases\":[],\"content\":\"## 개요\\n정의 내용입니다. 충분히 작성되었습니다.\\n\\n## 세부내용\\n[[unknown-cluster]] 참조\\n\",\"relatedConcepts\":[]}";

    mockHttpServer.createContext(
        "/v1/chat/completions",
        exchange -> {
          callCount.incrementAndGet();
          String responseBody = buildOpenAiResponseJson(brokenJson);
          byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    mockHttpServer.start();

    List<Map<String, Object>> nodes =
        List.of(
            Map.of(
                "id", "event-driven-architecture",
                "name", "이벤트 드리븐 아키텍처",
                "snippet", "EDA 설명"));

    Map<String, Object> graphData3 = Map.of("nodes", nodes, "links", List.of());
    llmService.generateWikiPages(graphData3, "원문 텍스트", "tenant-test", "gpt-4o-mini");

    assertEquals(3, callCount.get(), "최대 3회까지 재시도 후 중단되어야 함");

    Path savedFile = tempWikiDir.resolve("tenant-test/wiki/concepts/event-driven-architecture.md");
    assertFalse(Files.exists(savedFile), "3회 모두 실패 시 파일이 생성되지 않고 REJECT 되어야 함 (오염 방지)");
  }

  @Test
  @DisplayName("Case 4: 자기참조만 발생 -> Auto-Sanitize -> LLM 재호출 없이 1회 호출로 즉시 저장 성공")
  void pipeline_case4_selfReferenceAutoSanitize_singleCall() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);

    String summary = "이벤트 기반 비동기 메시지 교환을 통해 마이크로서비스 간 결합도를 최소화하는 아키텍처 패턴";
    // 자기 자신 링크 [[event-driven-architecture]] 포함
    String selfRefContent =
        "## 개요\n이벤트 드리븐 아키텍처는 비동기 메시징을 통해 결합도를 낮춥니다. [[event-driven-architecture]]는 핵심입니다.\n\n"
            + "## 핵심 메커니즘\n- 프로듀서가 이벤트를 발행하고 컨슈머가 구독합니다.\n- 비동기 방식으로 컴포넌트를 분리합니다.";

    String jsonWithSelfRef =
        "{\"id\":\"event-driven-architecture\",\"title\":\"이벤트 드리븐 아키텍처\",\"type\":\"concept\","
            + "\"summary\":\""
            + summary
            + "\",\"tags\":[\"architecture\"],\"aliases\":[\"EDA\"],"
            + "\"content\":\""
            + selfRefContent.replace("\n", "\\n")
            + "\",\"relatedConcepts\":[]}";

    mockHttpServer.createContext(
        "/v1/chat/completions",
        exchange -> {
          callCount.incrementAndGet();
          String responseBody = buildOpenAiResponseJson(jsonWithSelfRef);
          byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });
    mockHttpServer.start();

    List<Map<String, Object>> nodes =
        List.of(
            Map.of(
                "id", "event-driven-architecture",
                "name", "이벤트 드리븐 아키텍처",
                "snippet", "EDA 설명"));

    Map<String, Object> graphData4 = Map.of("nodes", nodes, "links", List.of());
    llmService.generateWikiPages(graphData4, "원문 텍스트", "tenant-test", "gpt-4o-mini");

    assertEquals(1, callCount.get(), "자기참조는 Auto-Sanitize되어 추가 재호출 없이 1회 호출로 끝나야 함");

    Path savedFile = tempWikiDir.resolve("tenant-test/wiki/concepts/event-driven-architecture.md");
    assertTrue(Files.exists(savedFile), "정상 저장되어야 함");
    String savedContent = Files.readString(savedFile);
    assertFalse(savedContent.contains("[[event-driven-architecture]]"), "자기참조 링크가 제거되어야 함");
    assertTrue(savedContent.contains("**이벤트 드리븐 아키텍처**"), "볼드 텍스트로 안전하게 교정되어야 함");
  }
}
