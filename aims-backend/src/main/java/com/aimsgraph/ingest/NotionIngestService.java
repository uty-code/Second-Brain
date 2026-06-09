package com.aimsgraph.ingest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotionIngestService {

    private final ObjectMapper objectMapper;

    public String fetchNotionPageText(String pageId, String apiKey) {
        String cleanId = pageId.split("\\?")[0].replaceAll("[^a-zA-Z0-9]", "");
        if (cleanId.length() >= 32) {
            cleanId = cleanId.substring(cleanId.length() - 32);
        } else {
            throw new IllegalArgumentException("Invalid Notion Page ID or URL format.");
        }
        log.info("Fetching Notion page blocks for pageId: {}", cleanId);
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.notion.com/v1/blocks/" + cleanId + "/children")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Notion-Version", "2022-06-28")
                .build();

        String responseBody = restClient.get()
                .retrieve()
                .body(String.class);

        return extractTextFromBlocks(responseBody);
    }

    private String extractTextFromBlocks(String jsonResponse) {
        StringBuilder sb = new StringBuilder();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode block : results) {
                    String type = block.path("type").asText();
                    JsonNode typeNode = block.path(type);
                    if (typeNode.isObject()) {
                        JsonNode richTextArray = typeNode.path("rich_text");
                        if (richTextArray.isArray()) {
                            for (JsonNode richText : richTextArray) {
                                sb.append(richText.path("plain_text").asText());
                            }
                            sb.append("\n\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Notion response", e);
            throw new RuntimeException("Failed to parse Notion API response", e);
        }
        return sb.toString();
    }

    public boolean verifyToken(String apiKey) {
        try {
            log.info("Verifying Notion API token...");
            RestClient restClient = RestClient.builder()
                    .baseUrl("https://api.notion.com/v1/users/me")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Notion-Version", "2022-06-28")
                    .build();

            org.springframework.http.ResponseEntity<String> response = restClient.get()
                    .retrieve()
                    .toEntity(String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Token verification failed: {}", e.getMessage());
            return false;
        }
    }
}
