package com.pharmarag.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmarag.model.SubstitutionRequest;
import com.pharmarag.model.SubstitutionResponse;
import com.pharmarag.service.RagPipelineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubstitutionController.class)
class SubstitutionControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean RagPipelineService ragPipelineService;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("pharma-rag-backend"));
    }

    @Test
    void recommendEndpointReturnsResultForValidRequest() throws Exception {
        // Arrange
        SubstitutionResponse mockResponse = SubstitutionResponse.builder()
                .status(SubstitutionResponse.SubstitutionStatus.FOUND)
                .reason("Rosuvastatin is therapeutically equivalent and covered")
                .confidence(0.92)
                .primaryRecommendation(SubstitutionResponse.DrugRecommendation.builder()
                        .drugName("Rosuvastatin 20mg")
                        .genericName("Rosuvastatin")
                        .formularyTier(2)
                        .memberCopay("$45")
                        .allergyCheckResult("PASSED")
                        .interactionCheckResult("PASSED")
                        .requiresPrescriberApproval(false)
                        .build())
                .evaluatedCandidates(List.of())
                .sourceDocuments(List.of("fda-statin-001"))
                .prescriberContactRequired(false)
                .generatedAt(Instant.now())
                .build();

        when(ragPipelineService.process(any(SubstitutionRequest.class))).thenReturn(mockResponse);

        SubstitutionRequest request = new SubstitutionRequest();
        request.setDrugName("Lipitor");
        request.setDosage("40mg");
        request.setMemberId("MBR-TEST1234");
        request.setPlanId("STANDARD-PPO-2024");
        request.setReason(SubstitutionRequest.SubstitutionReason.OUT_OF_STOCK);

        // Act & Assert
        mockMvc.perform(post("/api/v1/substitution/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FOUND"))
                .andExpect(jsonPath("$.primaryRecommendation.drugName").value("Rosuvastatin 20mg"))
                .andExpect(jsonPath("$.confidence").value(0.92));
    }

    @Test
    void recommendEndpointReturnsBadRequestForInvalidMemberId() throws Exception {
        SubstitutionRequest request = new SubstitutionRequest();
        request.setDrugName("Lipitor");
        request.setMemberId("John Smith"); // Real name — invalid, violates HIPAA format

        mockMvc.perform(post("/api/v1/substitution/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void recommendEndpointReturnsBadRequestWhenDrugNameMissing() throws Exception {
        SubstitutionRequest request = new SubstitutionRequest();
        request.setMemberId("MBR-TEST1234");
        // drugName intentionally missing

        mockMvc.perform(post("/api/v1/substitution/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.drugName").exists());
    }
}
