package com.pharmarag.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * HIPAA-compliant audit logger.
 *
 * Logs every substitution request and response for compliance traceability.
 * NEVER logs: real patient names, DOB, SSN, address, or full member records.
 * ONLY logs: pseudonymized member ID, action type, outcome status, confidence score, timestamp.
 */
@Component
@Slf4j
public class AuditLogger {

    private static final String AUDIT_PREFIX = "[AUDIT]";

    /**
     * Logs an incoming substitution request.
     * Only the pseudonymized member ID and action type are recorded.
     */
    public void logRequest(String pseudoMemberId, String action) {
        log.info("{} timestamp={} memberId={} action={} status=RECEIVED",
                AUDIT_PREFIX,
                Instant.now(),
                maskMemberId(pseudoMemberId),
                action);
    }

    /**
     * Logs the outcome of a substitution request.
     * Status and confidence score are safe to log (not PHI).
     */
    public void logResponse(String pseudoMemberId, String status, double confidence) {
        log.info("{} timestamp={} memberId={} action=SUBSTITUTION_RESPONSE status={} confidence={}",
                AUDIT_PREFIX,
                Instant.now(),
                maskMemberId(pseudoMemberId),
                status,
                String.format("%.2f", confidence));
    }

    /**
     * Logs a validation error (e.g., invalid member ID format).
     */
    public void logValidationError(String field, String reason) {
        log.warn("{} timestamp={} action=VALIDATION_ERROR field={} reason={}",
                AUDIT_PREFIX,
                Instant.now(),
                field,
                reason);
    }

    /**
     * Logs an escalation event.
     */
    public void logEscalation(String pseudoMemberId, String queue, String reason) {
        log.info("{} timestamp={} memberId={} action=ESCALATION queue={} reason={}",
                AUDIT_PREFIX,
                Instant.now(),
                maskMemberId(pseudoMemberId),
                queue,
                reason);
    }

    /**
     * Partially masks the member ID for log output.
     * MBR-ABCD1234 → MBR-****1234
     */
    private String maskMemberId(String memberId) {
        if (memberId == null || memberId.length() < 5) return "MBR-XXXX";
        return memberId.substring(0, 4) + "****" + memberId.substring(memberId.length() - 4);
    }
}
