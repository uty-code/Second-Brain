package com.aimsgraph.api;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/health")
public class LlmHealthController {

  @Value("${llm.api-key:demo}")
  private String fallbackApiKey;

  @GetMapping("/llm")
  public ResponseEntity<Map<String, Object>> checkLlmHealth(
      @RequestParam(value = "model", defaultValue = "gpt-4o-mini") String modelName) {
    Map<String, Object> result = new HashMap<>();
    result.put("timestamp", System.currentTimeMillis());
    result.put("model", modelName);

    boolean isDeepseek = "deepseek-v4".equalsIgnoreCase(modelName);
    String apiKey;
    String targetModel;
    String baseUrl = null;

    if (isDeepseek) {
      apiKey = System.getenv("DEEPSEEK_API_KEY");
      targetModel = "deepseek/deepseek-v4-pro";
      baseUrl = "https://openrouter.ai/api/v1";

      if (apiKey == null || apiKey.isBlank()) {
        result.put("status", "DOWN");
        result.put("message", "DEEPSEEK_API_KEY environment variable is not configured.");
        result.put("apiKeyConfigured", false);
        return ResponseEntity.status(503).body(result);
      }
    } else {
      String envKey = System.getenv("OPENAI_API_KEY");
      apiKey = (envKey != null && !envKey.isBlank()) ? envKey : fallbackApiKey;
      targetModel = modelName;

      if (apiKey == null || apiKey.isBlank() || "demo".equalsIgnoreCase(apiKey)) {
        result.put("status", "DOWN");
        result.put("message", "OPENAI_API_KEY environment variable is not configured.");
        result.put("apiKeyConfigured", false);
        return ResponseEntity.status(503).body(result);
      }
    }

    result.put("apiKeyConfigured", true);
    result.put("maskedApiKey", maskKey(apiKey));

    long start = System.currentTimeMillis();
    try {
      var builder =
          OpenAiChatModel.builder()
              .apiKey(apiKey)
              .modelName(targetModel)
              .maxTokens(50)
              .temperature(0.0);

      if (baseUrl != null) {
        builder.baseUrl(baseUrl);
      }

      ChatModel model = builder.build();
      String reply = model.chat("Respond with: 'LLM connection is healthy!'");
      long elapsed = System.currentTimeMillis() - start;

      result.put("status", "UP");
      result.put("latencyMs", elapsed);
      result.put("reply", reply.trim());
      result.put("message", "LLM API connection verified successfully.");
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      long elapsed = System.currentTimeMillis() - start;
      log.error("LLM health check failed", e);
      result.put("status", "DOWN");
      result.put("latencyMs", elapsed);
      result.put("error", e.getMessage());
      result.put("message", "Failed to communicate with LLM provider.");
      return ResponseEntity.status(500).body(result);
    }
  }

  private String maskKey(String key) {
    if (key == null || key.length() <= 8) return "****";
    return key.substring(0, 7) + "..." + key.substring(key.length() - 4);
  }
}
