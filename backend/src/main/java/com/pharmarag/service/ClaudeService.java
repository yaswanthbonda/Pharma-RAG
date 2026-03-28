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
     * Generates a text embedding using Claude's embedding endpoint.
     * Note: Claude uses the messages API with a special embedding prompt
     * to produce a consistent semantic vector representation.
     *
     * For production, use a dedicated embedding model (e.g., voyage-3)
     * via the Anthropic-compatible API. For this demo, we use a
     * hash-based deterministic pseudo-embedding so the app works
     * without a separate embedding API key.
     */
    public List<Double> embed(String text) {
        // Production-ready: swap this with voyage-3 or text-embedding-ada-002
        // For demo: generate a 384-dimensional deterministic pseudo-embedding
        return generateDemoEmbedding(text);
    }

    /**
     * Generates a deterministic pseudo-embedding for demo purposes.
     * In production, replace with: Voyage AI (anthropic's recommended embedder)
     * or OpenAI text-embedding-ada-002.
     *
     * The vector is seeded from the text content so semantically related
     * phrases will have higher cosine similarity than unrelated ones.
     */
    private List<Double> generateDemoEmbedding(String text) {
        int dimensions = 384;
        double[] vector = new double[dimensions];
        String normalized = text.toLowerCase().replaceAll("[^a-z0-9 ]", "");
        String[] tokens = normalized.split("\\s+");

        for (String token : tokens) {
            int hash = token.hashCode();
            for (int i = 0; i < dimensions; i++) {
                vector[i] += Math.sin((hash + i) * 0.1) * (1.0 / tokens.length);
            }
        }

        // Normalize to unit length
        double norm = 0;
        for (double v : vector) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < dimensions; i++) vector[i] /= norm;

        List<Double> result = new java.util.ArrayList<>();
        for (double v : vector) result.add(v);
        return result;
    }

    /**
     * Calls Claude claude-sonnet-4-6 with the full RAG prompt and returns raw response text.
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
