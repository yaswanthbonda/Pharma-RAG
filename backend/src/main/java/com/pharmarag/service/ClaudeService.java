package com.pharmarag.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClaudeService {

    @Qualifier("anthropicWebClient")
    private final WebClient anthropicWebClient;

    @Value("${anthropic.api.model}")
    private String model;

    @Value("${anthropic.api.max-tokens}")
    private int maxTokens;

    /**
     * Calls Claude with the full RAG prompt and returns raw response text.
     *
     * @param systemPrompt The HIPAA-aware system instructions
     * @param userPrompt   The assembled context + query
     * @return Claude's raw text response (JSON string)
     */
    public String chat(String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();

        ClaudeRequest request = new ClaudeRequest(
                model,
                maxTokens,
                systemPrompt,
                List.of(new ClaudeMessage("user", userPrompt))
        );

        ClaudeResponse response = anthropicWebClient.post()
                .uri("/v1/messages")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .doOnError(e -> log.error("Claude API call failed: {}", e.getMessage()))
                .block();

        long elapsed = System.currentTimeMillis() - start;
        log.info("Claude API call completed in {}ms", elapsed);

        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            throw new RuntimeException("Empty response from Claude API");
        }

        return response.getContent().get(0).getText();
    }

    // ── Claude API DTOs ─────────────────────────────────────────────────────

    @Data
    private static class ClaudeRequest {
        private final String model;
        @JsonProperty("max_tokens")
        private final int maxTokens;
        private final String system;
        private final List<ClaudeMessage> messages;
    }

    @Data
    private static class ClaudeMessage {
        private final String role;
        private final String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ClaudeResponse {
        private List<ContentBlock> content;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class ContentBlock {
            private String type;
            private String text;
        }
    }
}
