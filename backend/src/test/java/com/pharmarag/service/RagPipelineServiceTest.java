package com.pharmarag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pharmarag.audit.AuditLogger;
import com.pharmarag.model.SubstitutionRequest;
import com.pharmarag.model.SubstitutionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagPipelineServiceTest {

    @Mock private ClaudeService claudeService;
    @Mock private ChromaService chromaService;
    @Mock private AuditLogger auditLogger;

    private RagPipelineService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
        service = new RagPipelineService(claudeService, chromaService, auditLogger, objectMapper);
    }

    @Test
    void shouldReturnFoundStatusWhenClaudeFindsSubstitute() {
        // Arrange
        SubstitutionRequest request = buildRequest();

        ChromaService.ChromaQueryResult chromaResult = new ChromaService.ChromaQueryResult();
        List<List<String>> docs = List.of(List.of(
                "Drug: Rosuvastatin. Generic name: rosuvastatin. Class: statin.",
                "Drug: Pravastatin. Generic name: pravastatin. Class: statin."
        ));
        List<List<Double>> distances = List.of(List.of(0.05, 0.12));
        chromaResult.setDocuments(docs);
        chromaResult.setDistances(distances);

        when(claudeService.embed(anyString())).thenReturn(List.of(0.1, 0.2, 0.3));
        when(chromaService.queryByEmbedding(anyList(), anyInt())).thenReturn(chromaResult);
        when(claudeService.chat(anyString(), anyString())).thenReturn(buildFoundJson());

        // Act
        SubstitutionResponse response = service.process(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(SubstitutionResponse.SubstitutionStatus.FOUND);
        assertThat(response.getPrimaryRecommendation()).isNotNull();
        assertThat(response.getPrimaryRecommendation().getDrugName()).isEqualTo("Rosuvastatin 20mg");
        assertThat(response.getConfidence()).isGreaterThan(0.8);
        assertThat(response.getRagMetadata()).isNotNull();
        assertThat(response.getRagMetadata().getDocumentsRetrieved()).isEqualTo(2);
    }

    @Test
    void shouldReturnNoSafeSubstituteWhenInteractionBlocksAll() {
        // Arrange
        SubstitutionRequest request = buildRequest();
        request.setCurrentMedications(List.of("Warfarin"));

        ChromaService.ChromaQueryResult chromaResult = new ChromaService.ChromaQueryResult();
        chromaResult.setDocuments(List.of(List.of("Drug: Rosuvastatin. Interactions: warfarin.")));
        chromaResult.setDistances(List.of(List.of(0.08)));

        when(claudeService.embed(anyString())).thenReturn(List.of(0.1, 0.2));
        when(chromaService.queryByEmbedding(anyList(), anyInt())).thenReturn(chromaResult);
        when(claudeService.chat(anyString(), anyString())).thenReturn(buildNoSubstituteJson());

        // Act
        SubstitutionResponse response = service.process(request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(SubstitutionResponse.SubstitutionStatus.NO_SAFE_SUBSTITUTE);
        assertThat(response.getPrimaryRecommendation()).isNull();
        assertThat(response.getEscalationQueue()).isEqualTo("clinical-review-urgent");
    }

    @Test
    void shouldGracefullyHandleMalformedClaudeResponse() {
        // Arrange - Claude returns garbage (shouldn't happen, but we test resilience)
        SubstitutionRequest request = buildRequest();
        ChromaService.ChromaQueryResult chromaResult = new ChromaService.ChromaQueryResult();
        chromaResult.setDocuments(List.of(List.of("Drug: Test.")));
        chromaResult.setDistances(List.of(List.of(0.1)));

        when(claudeService.embed(anyString())).thenReturn(List.of(0.1));
        when(chromaService.queryByEmbedding(anyList(), anyInt())).thenReturn(chromaResult);
        when(claudeService.chat(anyString(), anyString())).thenReturn("THIS IS NOT JSON !!!");

        // Act - should NOT throw, should return safe fallback
        SubstitutionResponse response = service.process(request);

        // Assert - safe fallback
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(SubstitutionResponse.SubstitutionStatus.NO_SAFE_SUBSTITUTE);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private SubstitutionRequest buildRequest() {
        SubstitutionRequest req = new SubstitutionRequest();
        req.setDrugName("Lipitor");
        req.setDosage("40mg");
        req.setMemberId("MBR-TEST1234");
        req.setPlanId("STANDARD-PPO-2024");
        req.setDrugClass("statin");
        req.setReason(SubstitutionRequest.SubstitutionReason.OUT_OF_STOCK);
        req.setAllergies(List.of());
        req.setCurrentMedications(List.of());
        return req;
    }

    private String buildFoundJson() {
        return """
            {
              "status": "FOUND",
              "reason": "Rosuvastatin 20mg is therapeutically equivalent and covered under member plan",
              "confidence": 0.94,
              "primaryRecommendation": {
                "drugName": "Rosuvastatin 20mg",
                "genericName": "Rosuvastatin",
                "brandName": "Crestor",
                "dosage": "20mg",
                "drugClass": "statin",
                "formularyTier": 2,
                "memberCopay": "$45",
                "equivalencyNote": "High-intensity statin, guideline-equivalent to Atorvastatin 40mg",
                "allergyCheckResult": "PASSED",
                "interactionCheckResult": "PASSED",
                "requiresPrescriberApproval": false,
                "prescriberApprovalReason": null
              },
              "conditionalOption": null,
              "evaluatedCandidates": [
                { "drugName": "Rosuvastatin 20mg", "equivalencyPassed": true, "formularyPassed": true,
                  "interactionPassed": true, "allergyPassed": true, "exclusionReason": null },
                { "drugName": "Pravastatin 40mg", "equivalencyPassed": true, "formularyPassed": true,
                  "interactionPassed": true, "allergyPassed": true,
                  "exclusionReason": "Lower intensity — prescriber approval required" }
              ],
              "sourceDocuments": ["fda-statin-001", "fda-statin-002"],
              "escalationQueue": null,
              "prescriberContactRequired": false
            }
            """;
    }

    private String buildNoSubstituteJson() {
        return """
            {
              "status": "NO_SAFE_SUBSTITUTE",
              "reason": "All equivalent statins have documented interactions with Warfarin",
              "confidence": 0.97,
              "primaryRecommendation": null,
              "conditionalOption": null,
              "evaluatedCandidates": [
                { "drugName": "Rosuvastatin", "equivalencyPassed": true, "formularyPassed": true,
                  "interactionPassed": false, "allergyPassed": true,
                  "exclusionReason": "Major interaction with Warfarin — INR elevation risk" }
              ],
              "sourceDocuments": ["interaction-db-4421"],
              "escalationQueue": "clinical-review-urgent",
              "prescriberContactRequired": true
            }
            """;
    }
}
