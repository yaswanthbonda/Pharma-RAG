package com.pharmarag.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Incoming drug substitution request.
 * HIPAA note: No real member names, DOB, or SSN accepted.
 * memberId is a pseudonymized identifier only.
 */
@Data
public class SubstitutionRequest {

    @NotBlank(message = "Drug name is required")
    @Size(max = 200)
    private String drugName;

    @Size(max = 20)
    private String dosage;

    /** NDC (National Drug Code) - not PHI */
    @Pattern(regexp = "^[0-9]{0,11}$", message = "Invalid NDC format")
    private String ndc;

    /** Pseudonymized member ID - never a real name */
    @NotBlank(message = "Member ID is required")
    @Pattern(regexp = "^MBR-[A-Z0-9]{4,12}$", message = "Invalid member ID format (use MBR-XXXX)")
    private String memberId;

    /** Insurance plan ID */
    @Size(max = 50)
    private String planId;

    /** Drug class / therapeutic category */
    @Size(max = 100)
    private String drugClass;

    /** Reason for substitution request */
    private SubstitutionReason reason;

    /** Member allergies (drug classes, not detailed PHI) */
    private List<@Size(max = 100) String> allergies;

    /** Current medications (generic names only, no dosing PHI) */
    private List<@Size(max = 200) String> currentMedications;

    public enum SubstitutionReason {
        OUT_OF_STOCK,
        COST_REDUCTION,
        FORMULARY_CHANGE,
        PATIENT_PREFERENCE
    }
}
