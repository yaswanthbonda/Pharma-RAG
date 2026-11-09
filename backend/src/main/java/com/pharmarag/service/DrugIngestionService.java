package com.pharmarag.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pharmarag.model.DrugDocument;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the knowledge base into ChromaDB on first start:
 *  1. curated drug facts + synthetic formulary from seed files (always)
 *  2. optionally enriches each drug with a real label excerpt from OpenFDA (best effort)
 */
@Service
@Slf4j
public class DrugIngestionService {

    private static final int EXCERPT_CHARS = 400;

    private final WebClient openFdaWebClient;
    private final ChromaService chromaService;
    private final EmbeddingService embeddingService;
    private final SeedDataLoader seedDataLoader;
    private final boolean enrichmentEnabled;

    public DrugIngestionService(@Qualifier("openFdaWebClient") WebClient openFdaWebClient,
                                ChromaService chromaService,
                                EmbeddingService embeddingService,
                                SeedDataLoader seedDataLoader,
                                @Value("${pharmarag.ingest.openfda-enrichment:true}") boolean enrichmentEnabled) {
        this.openFdaWebClient = openFdaWebClient;
        this.chromaService = chromaService;
        this.embeddingService = embeddingService;
        this.seedDataLoader = seedDataLoader;
        this.enrichmentEnabled = enrichmentEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnStartup() {
        chromaService.initializeCollection();
        int existing = chromaService.getDocumentCount();
        if (existing > 0) {
            log.info("ChromaDB already has {} documents. Skipping ingestion.", existing);
            return;
        }

        List<DrugDocument> docs = seedDataLoader.load();
        log.info("Loaded {} seed drugs", docs.size());

        if (enrichmentEnabled) {
            int enriched = 0;
            for (DrugDocument doc : docs) {
                String excerpt = fetchLabelExcerpt(doc.getGenericName());
                if (excerpt != null) {
                    doc.setLabelExcerpt(excerpt);
                    enriched++;
                }
                sleepQuietly(150); // stay well under OpenFDA's anonymous rate limit
            }
            log.info("Enriched {}/{} drugs with OpenFDA label excerpts", enriched, docs.size());
        }

        List<List<Double>> embeddings = new ArrayList<>();
        for (DrugDocument doc : docs) embeddings.add(embeddingService.embed(doc.toEmbeddingText()));
        chromaService.addDocuments(docs, embeddings);
        log.info("Ingestion complete: {} documents", docs.size());
    }

    /** Returns a short excerpt of the contraindications section of the FDA label, or null. */
    String fetchLabelExcerpt(String genericName) {
        try {
            OpenFdaResponse r = openFdaWebClient.get()
                    .uri(u -> u.path("/label.json")
                            .queryParam("search", "openfda.generic_name:\"" + genericName + "\"")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .bodyToMono(OpenFdaResponse.class)
                    .block(Duration.ofSeconds(8));
            if (r == null || r.getResults() == null || r.getResults().isEmpty()) return null;
            List<String> contra = r.getResults().get(0).getContraindications();
            if (contra == null || contra.isEmpty()) return null;
            String text = contra.get(0).replaceAll("\\s+", " ").trim();
            return text.length() > EXCERPT_CHARS ? text.substring(0, EXCERPT_CHARS) + "..." : text;
        } catch (Exception e) {
            log.debug("OpenFDA enrichment skipped for '{}': {}", genericName, e.getMessage());
            return null;
        }
    }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenFdaResponse {
        private List<OpenFdaLabel> results;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenFdaLabel {
        private List<String> contraindications;
    }
}
