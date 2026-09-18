package com.aimsgraph.ingest;

import static org.junit.jupiter.api.Assertions.*;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class LlmLiveConnectionTest {

  @Test
  @DisplayName("OpenAI API 라이브 연결 및 응답 테스트")
  void testLiveOpenAiConnection() {
    String apiKey = System.getenv("OPENAI_API_KEY");
    assertNotNull(apiKey, "OPENAI_API_KEY 환경변수가 설정되어 있지 않습니다. 시스템 환경변수를 확인해 주세요.");
    assertFalse(apiKey.trim().isEmpty(), "OPENAI_API_KEY가 비어 있습니다.");

    System.out.println("==================================================");
    System.out.println("[LLM 연결 테스트 시작]");
    System.out.println("API Key 확인: " + maskKey(apiKey));
    System.out.println("테스트 대상 모델: gpt-4o-mini");

    long startTime = System.currentTimeMillis();

    ChatModel model =
        OpenAiChatModel.builder()
            .apiKey(apiKey)
            .modelName("gpt-4o-mini")
            .maxTokens(50)
            .temperature(0.0)
            .build();

    String prompt = "Reply with exactly: 'PING_OK'";
    String response = model.chat(prompt);

    long elapsed = System.currentTimeMillis() - startTime;

    System.out.println("LLM 응답 메시지: " + response.trim());
    System.out.println("응답 소요 시간: " + elapsed + " ms");
    System.out.println("==================================================");

    assertNotNull(response, "LLM 응답이 null입니다.");
    assertTrue(
        response.contains("PING_OK"), "LLM 응답에 예상 키워드(PING_OK)가 포함되어 있지 않습니다. 수신 내용: " + response);
  }

  private String maskKey(String key) {
    if (key == null || key.length() <= 8) return "****";
    return key.substring(0, 7) + "..." + key.substring(key.length() - 4);
  }
}
