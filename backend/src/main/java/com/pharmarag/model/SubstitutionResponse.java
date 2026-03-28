package com.pharmarag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubstitutionResponse {

    private SubstitutionStatus status;
    private String reason;

    /** Primary recommended drug (null if no safe substitute found) */
    private DrugRecommendation primaryRecommendation;

    /** Conditional option (e.g., needs prescriber approval) */
    private DrugRecommendation conditionalOption;

    /** List of checked candidates with per-gate results */
    private List<CandidateEvaluation> evaluatedCandidates;

    /** Confidence score 0.0 - 1.0 */
    private double confidence;

    /** Which source documents supported this decision */
    private List<String> sourceDocuments;

    /** Escalation queue if no safe substitute */
    private String escalationQueue;

    /** Whether prescriber contact is required */
    private boolean prescriberContactRequired;

    /** Audit timestamp */
    private Instant generatedAt;

    /** RAG pipeline metadata */
    private RagMetadata ragMetadata;

    public enum SubstitutionStatus {
        FOUND,
        NO_SAFE_SUBSTITUTE,
        NO_COVERED_SUBSTITUTE,
        UNIQUE_DRUG_NO_EQUIVALENT
    }

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DrugRecommendation {
        private String drugName;
        private String genericName;
        private String brandName;
        private String dosage;
        private String drugClass;
        private int formularyTier;
        private String memberCopay;
        private String equivalencyNote;
        private String allergyCheckResult;
        private String interactionCheckResult;
        private boolean requiresPrescriberApproval;
        private String prescriberApprovalReason;
    }

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CandidateEvaluation {
        private String drugName;
        private boolean equivalencyPassed;
        private boolean formularyPassed;
        private boolean interactionPassed;
        private boolean allergyPassed;
        private String exclusionReason;
    }

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RagMetadata {
        private int documentsRetrieved;
        private double topSimilarityScore;
        private String vectorCollection;
        private long retrievalTimeMs;
        private long llmTimeMs;
    }
}
