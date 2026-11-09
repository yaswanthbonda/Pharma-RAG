package com.pharmarag.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: the model classes must be deserializable from the JSON Claude returns.
 * Uses a plain ObjectMapper (same modules as JacksonConfig), so it fails fast if a model
 * class loses its no-args constructor.
 */
class SubstitutionResponseJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void deserializesTypicalClaudeResponse() throws Exception {
        String json = """
            {
              "status": "FOUND",
              "reason": "Rosuvastatin is equivalent and covered",
              "confidence": 0.93,
              "primaryRecommendation": {
                "drugName": "Rosuvastatin 20mg", "genericName": "Rosuvastatin", "brandName": "Crestor",
                "formularyTier": 2, "memberCopay": "$45",
                "allergyCheckResult": "PASSED", "interactionCheckResult": "PASSED",
                "requiresPrescriberApproval": false, "prescriberApprovalReason": null
              },
              "conditionalOption": null,
              "evaluatedCandidates": [
                { "drugName": "Rosuvastatin 20mg", "equivalencyPassed": true, "formularyPassed": true,
                  "interactionPassed": true, "allergyPassed": true, "exclusionReason": null }
              ],
              "sourceDocuments": ["fda-1", "fda-2"],
              "escalationQueue": null,
              "prescriberContactRequired": false,
              "someFieldTheModelInvented": "ignored"
            }
            """;

        SubstitutionResponse r = mapper.readValue(json, SubstitutionResponse.class);

        assertThat(r.getStatus()).isEqualTo(SubstitutionResponse.SubstitutionStatus.FOUND);
        assertThat(r.getConfidence()).isEqualTo(0.93);
        assertThat(r.getPrimaryRecommendation().getDrugName()).isEqualTo("Rosuvastatin 20mg");
        assertThat(r.getPrimaryRecommendation().getFormularyTier()).isEqualTo(2);
        assertThat(r.getEvaluatedCandidates()).hasSize(1);
        assertThat(r.getEvaluatedCandidates().get(0).isInteractionPassed()).isTrue();
        assertThat(r.getSourceDocuments()).containsExactly("fda-1", "fda-2");
    }

    @Test
    void deserializesNoSubstituteResponseWithEscalation() throws Exception {
        String json = """
            { "status": "NO_SAFE_SUBSTITUTE", "reason": "interaction blocks all candidates",
              "confidence": 0.97, "primaryRecommendation": null,
              "escalationQueue": "clinical-review-urgent", "prescriberContactRequired": true }
            """;

        SubstitutionResponse r = mapper.readValue(json, SubstitutionResponse.class);

        assertThat(r.getStatus()).isEqualTo(SubstitutionResponse.SubstitutionStatus.NO_SAFE_SUBSTITUTE);
        assertThat(r.getPrimaryRecommendation()).isNull();
        assertThat(r.getEscalationQueue()).isEqualTo("clinical-review-urgent");
        assertThat(r.isPrescriberContactRequired()).isTrue();
    }
}
