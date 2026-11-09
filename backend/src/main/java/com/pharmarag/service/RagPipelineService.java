package com.pharmarag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmarag.audit.AuditLogger;
import com.pharmarag.model.SubstitutionRequest;
import com.pharmarag.model.SubstitutionResponse;
import com.pharmarag.model.SubstitutionResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagPipelineService {

    private final EmbeddingService embeddingService;
    private final ClaudeService claudeService;
    private final ChromaService chromaService;
    private final AuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    private static final int TOP_K = 5;

    /**
     * Full RAG pipeline:
     * 1. Embed the query
     * 2. Retrieve top-K drug documents from ChromaDB
     * 3. Build the Claude prompt with retrieved context
     * 4. Parse and return Claude's structured response
     */
    public SubstitutionResponse process(SubstitutionRequest request) {

        // HIPAA: Log the request with pseudonymized member ID only (no drug details in audit)
        auditLogger.logRequest(request.getMemberId(), "SUBSTITUTION_REQUEST");

        long retrievalStart = System.currentTimeMillis();

        // Step 1: Build the retrieval query and embed it (same model used at ingestion)
        String queryText = buildRetrievalQuery(request);
        List<Double> queryEmbedding = embeddingService.embed(queryText);

        // Step 2: Retrieve semantically similar drug records
        ChromaService.ChromaQueryResult chromaResult =
                chromaService.queryByEmbedding(queryEmbedding, TOP_K);

        long retrievalMs = System.currentTimeMillis() - retrievalStart;
        log.info("Retrieved {} documents from ChromaDB in {}ms", TOP_K, retrievalMs);

        // Step 3: Build prompt with retrieved context
        List<String> retrievedDocs = chromaResult.flatDocuments();
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(request, retrievedDocs);

        // Step 4: Call Claude
        long llmStart = System.currentTimeMillis();
        String rawResponse = claudeService.chat(systemPrompt, userPrompt);
        long llmMs = System.currentTimeMillis() - llmStart;

        // Step 5: Parse response
        SubstitutionResponse response = parseClaudeResponse(rawResponse);

        // Enrich with RAG metadata
        response.setGeneratedAt(Instant.now());
        response.setRagMetadata(RagMetadata.builder()
                .documentsRetrieved(retrievedDocs.size())
                .topSimilarityScore(chromaResult.topScore())
                .vectorCollection(chromaService.getCollectionName())
                .retrievalTimeMs(retrievalMs)
                .llmTimeMs(llmMs)
                .build());

        // HIPAA: Log outcome (status only, no drug details)
        auditLogger.logResponse(request.getMemberId(), response.getStatus().name(), response.getConfidence());

        return response;
    }

    /**
     * Text used to FIND candidate drugs. It deliberately contains only the drug and its class:
     * allergies, current medications and plan details are applied later by Claude's safety gates,
     * and including them here would pull the embedding away from the drug documents.
     */
    private String buildRetrievalQuery(SubstitutionRequest req) {
        StringBuilder q = new StringBuilder("Therapeutic alternatives for ").append(req.getDrugName());
        if (req.getDosage() != null && !req.getDosage().isBlank()) q.append(' ').append(req.getDosage());
        if (req.getDrugClass() != null && !req.getDrugClass().isBlank()) {
            q.append(". Drug class: ").append(req.getDrugClass());
        }
        return q.toString();
    }

    private String buildSystemPrompt() {
        return """
                You are a clinical pharmacist AI assistant at a pharmacy benefit management company.
                You help identify safe, formulary-covered drug substitutions when a prescribed drug is unavailable.

                CRITICAL RULES:
                1. NEVER recommend a drug that fails ANY of the 4 safety gates below.
                2. ALWAYS evaluate every candidate through ALL 4 gates in order.
                3. If no safe substitute exists, return the appropriate no-substitute status.
                4. Respond ONLY with valid JSON matching the schema below.
                5. NEVER include PHI (patient names, DOB, SSN) in your response.

                THE 4 SAFETY GATES (evaluate in order):
                Gate 1 - THERAPEUTIC EQUIVALENCY: Same drug class, mechanism, and clinical effect
                Gate 2 - FORMULARY COVERAGE: Covered under member's plan (use retrieved formulary data)
                Gate 3 - DRUG INTERACTIONS: No major interactions with current medications
                Gate 4 - ALLERGY CHECK: No contraindications with member's documented allergies

                RESPONSE JSON SCHEMA:
                {
                  "status": "FOUND|NO_SAFE_SUBSTITUTE|NO_COVERED_SUBSTITUTE|UNIQUE_DRUG_NO_EQUIVALENT",
                  "reason": "brief explanation",
                  "confidence": 0.0-1.0,
                  "primaryRecommendation": {
                    "drugName": "...", "genericName": "...", "brandName": "...",
                    "dosage": "...", "drugClass": "...",
                    "formularyTier": 1-4, "memberCopay": "$XX",
                    "equivalencyNote": "...",
                    "allergyCheckResult": "PASSED|FAILED|N/A",
                    "interactionCheckResult": "PASSED|FAILED|N/A",
                    "requiresPrescriberApproval": false,
                    "prescriberApprovalReason": null
                  },
                  "conditionalOption": null,
                  "evaluatedCandidates": [
                    { "drugName": "...", "equivalencyPassed": true, "formularyPassed": true,
                      "interactionPassed": true, "allergyPassed": true, "exclusionReason": null }
                  ],
                  "sourceDocuments": ["doc1", "doc2"],
                  "escalationQueue": null,
                  "prescriberContactRequired": false
                }

                DISCLAIMER: This is a decision-support tool only. All recommendations must be
                reviewed and approved by a licensed pharmacist or physician before dispensing.
                """;
    }

    private String buildUserPrompt(SubstitutionRequest req, List<String> retrievedDocs) {
        StringBuilder sb = new StringBuilder();
        sb.append("SUBSTITUTION REQUEST:\n");
        sb.append("Drug: ").append(req.getDrugName());
        if (req.getDosage() != null) sb.append(" ").append(req.getDosage());
        sb.append("\n");
        sb.append("Reason: ").append(req.getReason() != null ? req.getReason().name() : "OUT_OF_STOCK").append("\n");
        sb.append("Plan ID: ").append(req.getPlanId() != null ? req.getPlanId() : "STANDARD-PPO").append("\n");

        if (req.getAllergies() != null && !req.getAllergies().isEmpty()) {
            sb.append("Member allergies: ").append(String.join(", ", req.getAllergies())).append("\n");
        }
        if (req.getCurrentMedications() != null && !req.getCurrentMedications().isEmpty()) {
            sb.append("Current medications: ").append(String.join(", ", req.getCurrentMedications())).append("\n");
        }

        sb.append("\nRETRIEVED DRUG KNOWLEDGE BASE CONTEXT:\n");
        for (int i = 0; i < retrievedDocs.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(retrievedDocs.get(i)).append("\n");
        }

        sb.append("\nEvaluate each retrieved drug as a potential substitute. ");
        sb.append("Think step by step through all 4 safety gates for each candidate. ");
        sb.append("Return ONLY valid JSON matching the schema in your instructions.");

        return sb.toString();
    }

    private SubstitutionResponse parseClaudeResponse(String raw) {
        try {
            // Claude may wrap the JSON in markdown fences or add a sentence around it.
            // Keep only the outermost {...} object.
            String json = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new IllegalArgumentException("no JSON object found in model output");
            }
            return objectMapper.readValue(json.substring(start, end + 1), SubstitutionResponse.class);
        } catch (Exception e) {
            // HIPAA: never log the raw model output at INFO/ERROR - it can echo clinical details.
            log.error("Failed to parse Claude response ({} chars): {}", raw == null ? 0 : raw.length(), e.getMessage());
            log.debug("Raw Claude response (DEBUG only): {}", raw);
            // Return a safe fallback
            return SubstitutionResponse.builder()
                    .status(SubstitutionStatus.NO_SAFE_SUBSTITUTE)
                    .reason("Unable to process substitution request — please contact your pharmacist")
                    .confidence(0.0)
                    .generatedAt(Instant.now())
                    .build();
        }
    }
}
