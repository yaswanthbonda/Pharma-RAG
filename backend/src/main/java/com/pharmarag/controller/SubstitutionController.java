package com.pharmarag.controller;

import com.pharmarag.model.SubstitutionRequest;
import com.pharmarag.model.SubstitutionResponse;
import com.pharmarag.service.RagPipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class SubstitutionController {

    private final RagPipelineService ragPipelineService;

    /**
     * Main endpoint: accepts a drug substitution request and returns
     * Claude-powered RAG recommendations.
     *
     * POST /api/v1/substitution/recommend
     */
    @PostMapping("/substitution/recommend")
    public ResponseEntity<SubstitutionResponse> recommend(
            @Valid @RequestBody SubstitutionRequest request) {

        log.info("Substitution request received for drug: {} (member: {})",
                sanitizeForLog(request.getDrugName()), request.getMemberId());

        SubstitutionResponse response = ragPipelineService.process(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     * GET /api/v1/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "pharma-rag-backend",
                "disclaimer", "For demonstration purposes only. Not for clinical use."
        ));
    }

    /** Sanitize drug name for logging — remove special chars that could inject log entries */
    private String sanitizeForLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[^a-zA-Z0-9 \\-]", "").substring(0, Math.min(input.length(), 50));
    }
}
