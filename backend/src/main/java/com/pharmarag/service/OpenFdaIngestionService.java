package com.pharmarag.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pharmarag.model.DrugDocument;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenFdaIngestionService {

    @Qualifier("openFdaWebClient")
    private final WebClient openFdaWebClient;

    private final ChromaService chromaService;
    private final ClaudeService claudeService;

    // Common drug classes to ingest from OpenFDA
    private static final List<String> DRUG_CLASSES = List.of(
        "statin", "beta blocker", "ACE inhibitor", "antibiotic", "proton pump inhibitor",
        "antidepressant", "antihistamine", "blood thinner", "calcium channel blocker", "diuretic"
    );

    private static final int BATCH_SIZE = 10;

    @EventListener(ApplicationReadyEvent.class)
    public void ingestOnStartup() {
        chromaService.initializeCollection();
        int existingCount = chromaService.getDocumentCount();

        if (existingCount > 0) {
            log.info("ChromaDB already has {} drug documents. Skipping ingestion.", existingCount);
            return;
        }

        log.info("Starting OpenFDA drug data ingestion...");
        int totalIngested = 0;

        for (String drugClass : DRUG_CLASSES) {
            try {
                List<DrugDocument> docs = fetchDrugsForClass(drugClass);
                if (!docs.isEmpty()) {
                    ingestBatch(docs);
                    totalIngested += docs.size();
                    log.info("Ingested {} drugs for class: {}", docs.size(), drugClass);
                }
                Thread.sleep(200); // Respect OpenFDA rate limits
            } catch (Exception e) {
                log.warn("Failed to ingest drugs for class '{}': {}", drugClass, e.getMessage());
            }
        }

        log.info("OpenFDA ingestion complete. Total documents ingested: {}", totalIngested);
    }

    private List<DrugDocument> fetchDrugsForClass(String drugClass) {
        try {
            OpenFdaResponse response = openFdaWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/label.json")
                            .queryParam("search", "pharmacological_class:\"" + drugClass + "\"")
                            .queryParam("limit", BATCH_SIZE)
                            .build())
                    .retrieve()
                    .bodyToMono(OpenFdaResponse.class)
                    .block();

            if (response == null || response.getResults() == null) return List.of();
            return response.getResults().stream()
                    .map(r -> mapToDocument(r, drugClass))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("OpenFDA fetch failed for '{}', using fallback mock data: {}", drugClass, e.getMessage());
            return getFallbackData(drugClass);
        }
    }

    private DrugDocument mapToDocument(OpenFdaLabel label, String drugClass) {
        try {
            OpenFdaLabel.OpenFdaMeta meta = label.getOpenFda();
            String genericName = meta != null && meta.getGenericName() != null && !meta.getGenericName().isEmpty()
                    ? meta.getGenericName().get(0) : "Unknown";
            String brandName = meta != null && meta.getBrandName() != null && !meta.getBrandName().isEmpty()
                    ? meta.getBrandName().get(0) : genericName;
            String ndc = meta != null && meta.getProductNdc() != null && !meta.getProductNdc().isEmpty()
                    ? meta.getProductNdc().get(0) : UUID.randomUUID().toString();

            return DrugDocument.builder()
                    .id("fda-" + ndc.replace("/", "-"))
                    .genericName(genericName)
                    .brandName(brandName)
                    .drugClass(drugClass)
                    .pharmacologicClass(meta != null && meta.getPharmacologicalClass() != null
                            ? String.join(", ", meta.getPharmacologicalClass()) : drugClass)
                    .routeOfAdministration(label.getRoute() != null && !label.getRoute().isEmpty()
                            ? label.getRoute().get(0) : "oral")
                    .dosageForms(label.getDosageFormsAndStrengths() != null
                            ? List.of(label.getDosageFormsAndStrengths().get(0).substring(0, Math.min(100,
                                label.getDosageFormsAndStrengths().get(0).length()))) : List.of("tablet"))
                    .activeIngredients(List.of(genericName))
                    .interactions(label.getDrugInteractions() != null && !label.getDrugInteractions().isEmpty()
                            ? List.of(label.getDrugInteractions().get(0).substring(0, Math.min(200,
                                label.getDrugInteractions().get(0).length()))) : List.of("see prescribing information"))
                    .contraindications(label.getContraindications() != null && !label.getContraindications().isEmpty()
                            ? List.of(label.getContraindications().get(0).substring(0, Math.min(200,
                                label.getContraindications().get(0).length()))) : List.of("see prescribing information"))
                    .allergyFlags(extractAllergyFlags(label))
                    .ndc(ndc)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to map FDA label: {}", e.getMessage());
            return null;
        }
    }

    private List<String> extractAllergyFlags(OpenFdaLabel label) {
        List<String> flags = new ArrayList<>();
        if (label.getContraindications() != null) {
            String contra = String.join(" ", label.getContraindications()).toLowerCase();
            if (contra.contains("sulfa") || contra.contains("sulfonamide")) flags.add("sulfa");
            if (contra.contains("penicillin")) flags.add("penicillin");
            if (contra.contains("latex")) flags.add("latex");
            if (contra.contains("shellfish") || contra.contains("iodine")) flags.add("shellfish");
            if (contra.contains("aspirin") || contra.contains("nsaid")) flags.add("aspirin");
        }
        return flags.isEmpty() ? List.of("none") : flags;
    }

    private void ingestBatch(List<DrugDocument> docs) {
        List<List<Double>> embeddings = docs.stream()
                .map(doc -> claudeService.embed(doc.toEmbeddingText()))
                .collect(Collectors.toList());
        chromaService.addDocuments(docs, embeddings);
    }

    /** Fallback mock data if OpenFDA is unreachable */
    private List<DrugDocument> getFallbackData(String drugClass) {
        return switch (drugClass) {
            case "statin" -> List.of(
                DrugDocument.builder().id("mock-atorvastatin").genericName("Atorvastatin")
                    .brandName("Lipitor").drugClass("statin").pharmacologicClass("HMG-CoA reductase inhibitor")
                    .routeOfAdministration("oral").dosageForms(List.of("10mg tablet", "20mg tablet", "40mg tablet", "80mg tablet"))
                    .activeIngredients(List.of("atorvastatin calcium")).interactions(List.of("warfarin", "cyclosporine", "clarithromycin"))
                    .contraindications(List.of("active liver disease", "pregnancy")).allergyFlags(List.of("none")).ndc("0071-0155-23").build(),
                DrugDocument.builder().id("mock-rosuvastatin").genericName("Rosuvastatin")
                    .brandName("Crestor").drugClass("statin").pharmacologicClass("HMG-CoA reductase inhibitor")
                    .routeOfAdministration("oral").dosageForms(List.of("5mg tablet", "10mg tablet", "20mg tablet", "40mg tablet"))
                    .activeIngredients(List.of("rosuvastatin calcium")).interactions(List.of("warfarin", "antacids", "lopinavir"))
                    .contraindications(List.of("active liver disease", "pregnancy")).allergyFlags(List.of("none")).ndc("0310-0755-90").build(),
                DrugDocument.builder().id("mock-pravastatin").genericName("Pravastatin")
                    .brandName("Pravachol").drugClass("statin").pharmacologicClass("HMG-CoA reductase inhibitor")
                    .routeOfAdministration("oral").dosageForms(List.of("10mg tablet", "20mg tablet", "40mg tablet", "80mg tablet"))
                    .activeIngredients(List.of("pravastatin sodium")).interactions(List.of("cyclosporine", "clarithromycin"))
                    .contraindications(List.of("active liver disease")).allergyFlags(List.of("none")).ndc("0003-0154-58").build()
            );
            case "antibiotic" -> List.of(
                DrugDocument.builder().id("mock-amoxicillin").genericName("Amoxicillin")
                    .brandName("Amoxil").drugClass("antibiotic").pharmacologicClass("penicillin")
                    .routeOfAdministration("oral").dosageForms(List.of("250mg capsule", "500mg capsule"))
                    .activeIngredients(List.of("amoxicillin")).interactions(List.of("warfarin", "methotrexate"))
                    .contraindications(List.of("penicillin allergy")).allergyFlags(List.of("penicillin")).ndc("0093-3107-01").build(),
                DrugDocument.builder().id("mock-azithromycin").genericName("Azithromycin")
                    .brandName("Zithromax").drugClass("antibiotic").pharmacologicClass("macrolide")
                    .routeOfAdministration("oral").dosageForms(List.of("250mg tablet", "500mg tablet"))
                    .activeIngredients(List.of("azithromycin dihydrate")).interactions(List.of("warfarin", "digoxin", "antacids"))
                    .contraindications(List.of("macrolide allergy", "hepatic impairment")).allergyFlags(List.of("none")).ndc("0069-3060-20").build()
            );
            default -> List.of();
        };
    }

    // ── OpenFDA API response DTOs ───────────────────────────────────────────

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenFdaResponse {
        private List<OpenFdaLabel> results;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenFdaLabel {
        @JsonProperty("openfda")
        private OpenFdaMeta openFda;
        private List<String> route;
        @JsonProperty("dosage_forms_and_strengths")
        private List<String> dosageFormsAndStrengths;
        @JsonProperty("drug_interactions")
        private List<String> drugInteractions;
        private List<String> contraindications;
        @JsonProperty("warnings_and_cautions")
        private List<String> warningsAndCautions;

        @Data @JsonIgnoreProperties(ignoreUnknown = true)
        static class OpenFdaMeta {
            @JsonProperty("generic_name")
            private List<String> genericName;
            @JsonProperty("brand_name")
            private List<String> brandName;
            @JsonProperty("product_ndc")
            private List<String> productNdc;
            @JsonProperty("pharm_class_epc")
            private List<String> pharmacologicalClass;
        }
    }
}
