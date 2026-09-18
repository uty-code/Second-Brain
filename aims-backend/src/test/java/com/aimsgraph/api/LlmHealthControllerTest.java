package com.aimsgraph.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

public class LlmHealthControllerTest {

  @Test
  @DisplayName("LlmHealthController fallback 테스트 (API 키 없을 때)")
  void checkLlmHealth_whenNoKey() {
    LlmHealthController controller = new LlmHealthController();
    // 환경변수 fallback이 demo일 경우 상태 검증
    ResponseEntity<Map<String, Object>> response = controller.checkLlmHealth("gpt-4o-mini");
    assertNotNull(response);
    assertNotNull(response.getBody());
  }
}
