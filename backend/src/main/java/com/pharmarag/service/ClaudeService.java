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
import java.util.stream.Collectors;

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

        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            throw new RuntimeException("Empty response from Claude API");
        }

        // The reply can contain several blocks (e.g. a reasoning block before the answer).
        // Keep only the text blocks; never assume the first block is text.
        String text = response.getContent().stream()
                .filter(b -> "text".equals(b.getType()) && b.getText() != null)
                .map(ClaudeResponse.ContentBlock::getText)
                .collect(Collectors.joining("\n"));

        // Metadata only - no clinical content in this log line.
        log.info("Claude API call completed in {}ms (stop_reason={}, blocks={}, text_chars={})",
                elapsed, response.getStopReason(),
                response.getContent().stream().map(ClaudeResponse.ContentBlock::getType).toList(),
                text.length());

        if (text.isBlank()) {
            throw new RuntimeException("Claude returned no text content (stop_reason=" + response.getStopReason() + ")");
        }
        return text;
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
        @JsonProperty("stop_reason")
        private String stopReason;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        static class ContentBlock {
            private String type;
            private String text;
        }
    }
}
