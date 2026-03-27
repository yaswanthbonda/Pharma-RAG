package com.pharmarag.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pharmarag.model.DrugDocument;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChromaService {

    @Qualifier("chromaWebClient")
    private final WebClient chromaWebClient;

    @Value("${chroma.collection-name}")
    private String collectionName;

    private String collectionId;

    /**
     * Creates the ChromaDB collection if it does not exist.
     * Called at application startup.
     */
    public void initializeCollection() {
        chromaWebClient.post()
                .uri("/api/v1/collections")
                .bodyValue(Map.of(
                        "name", collectionName,
                        "metadata", Map.of("description", "PharmaSub drug embeddings")
                ))
                .retrieve()
                .bodyToMono(ChromaCollection.class)
                .doOnSuccess(c -> {
                    this.collectionId = c.getId();
                    log.info("ChromaDB collection initialized: {} (id={})", collectionName, collectionId);
                })
                .doOnError(e -> log.warn("Collection may already exist, fetching existing: {}", e.getMessage()))
                .onErrorResume(e -> getCollection())
                .block();
    }

    private Mono<ChromaCollection> getCollection() {
        return chromaWebClient.get()
                .uri("/api/v1/collections/" + collectionName)
                .retrieve()
                .bodyToMono(ChromaCollection.class)
                .doOnSuccess(c -> {
                    this.collectionId = c.getId();
                    log.info("Using existing ChromaDB collection: {}", collectionId);
                });
    }

    /**
     * Adds a batch of drug documents with their embeddings to ChromaDB.
     */
    public void addDocuments(List<DrugDocument> docs, List<List<Double>> embeddings) {
        if (collectionId == null) initializeCollection();

        List<String> ids = docs.stream().map(DrugDocument::getId).toList();
        List<String> texts = docs.stream().map(DrugDocument::toEmbeddingText).toList();
        List<Map<String, Object>> metadatas = docs.stream().map(doc -> {
            Map<String, Object> m = new HashMap<>();
            m.put("genericName", doc.getGenericName() != null ? doc.getGenericName() : "");
            m.put("brandName", doc.getBrandName() != null ? doc.getBrandName() : "");
            m.put("drugClass", doc.getDrugClass() != null ? doc.getDrugClass() : "");
            m.put("ndc", doc.getNdc() != null ? doc.getNdc() : "");
            return m;
        }).toList();

        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        body.put("embeddings", embeddings);
        body.put("documents", texts);
        body.put("metadatas", metadatas);

        chromaWebClient.post()
                .uri("/api/v1/collections/" + collectionId + "/add")
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.info("Added {} documents to ChromaDB", docs.size()))
                .doOnError(e -> log.error("Failed to add documents to ChromaDB: {}", e.getMessage()))
                .block();
    }

    /**
     * Performs semantic similarity search against the drug collection.
     *
     * @param queryEmbedding The embedded query vector
     * @param topK           Number of results to return
     * @return ChromaDB query results with documents and distances
     */
    public ChromaQueryResult queryByEmbedding(List<Double> queryEmbedding, int topK) {
        if (collectionId == null) initializeCollection();

        Map<String, Object> body = Map.of(
                "query_embeddings", List.of(queryEmbedding),
                "n_results", topK,
                "include", List.of("documents", "metadatas", "distances")
        );

        return chromaWebClient.post()
                .uri("/api/v1/collections/" + collectionId + "/query")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(ChromaQueryResult.class)
                .doOnError(e -> log.error("ChromaDB query failed: {}", e.getMessage()))
                .block();
    }

    /**
     * Returns the total count of documents in the collection.
     */
    public int getDocumentCount() {
        if (collectionId == null) return 0;
        try {
            Map<?, ?> result = chromaWebClient.get()
                    .uri("/api/v1/collections/" + collectionId + "/count")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return result != null ? ((Number) result.getOrDefault("count", 0)).intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ── ChromaDB response DTOs ──────────────────────────────────────────────

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChromaCollection {
        private String id;
        private String name;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChromaQueryResult {
        private List<List<String>> ids;
        private List<List<String>> documents;
        private List<List<Map<String, Object>>> metadatas;
        private List<List<Double>> distances;

        /** Convenience: returns flattened top-K documents */
        public List<String> flatDocuments() {
            return documents != null && !documents.isEmpty() ? documents.get(0) : List.of();
        }

        /** Convenience: returns top similarity score (1 - distance for cosine) */
        public double topScore() {
            if (distances == null || distances.isEmpty() || distances.get(0).isEmpty()) return 0.0;
            return 1.0 - distances.get(0).get(0);
        }
    }
}
