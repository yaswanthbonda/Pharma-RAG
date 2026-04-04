package com.pharmarag.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLoggerTest {

    private AuditLogger auditLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger();
        Logger logger = (Logger) LoggerFactory.getLogger(AuditLogger.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @Test
    void logRequestMasksMemberIdAndNeverLogsFullId() {
        auditLogger.logRequest("MBR-ABCD5678", "SUBSTITUTION_REQUEST");

        String logMessage = logAppender.list.get(0).getFormattedMessage();

        // Full member ID must NOT appear
        assertThat(logMessage).doesNotContain("MBR-ABCD5678");
        // Masked version should appear (last 4 chars preserved)
        assertThat(logMessage).contains("5678");
        // Action should be logged
        assertThat(logMessage).contains("SUBSTITUTION_REQUEST");
        // AUDIT prefix
        assertThat(logMessage).contains("[AUDIT]");
    }

    @Test
    void logResponseNeverContainsDrugNames() {
        // Ensure we never accidentally log PHI-adjacent info in the response audit
        auditLogger.logResponse("MBR-TEST1234", "FOUND", 0.94);

        String logMessage = logAppender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("FOUND");
        assertThat(logMessage).contains("0.94");
        // Member ID should be masked, not full
        assertThat(logMessage).doesNotContain("MBR-TEST1234");
    }

    @Test
    void logEscalationIncludesQueueAndMaskedId() {
        auditLogger.logEscalation("MBR-XYZ12345", "clinical-review-urgent", "drug_interaction");

        String logMessage = logAppender.list.get(0).getFormattedMessage();
        assertThat(logMessage).contains("clinical-review-urgent");
        assertThat(logMessage).contains("drug_interaction");
        assertThat(logMessage).doesNotContain("MBR-XYZ12345");
    }
}
