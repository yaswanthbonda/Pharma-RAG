package com.pharmarag.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * A drug record stored (as text + embedding) in ChromaDB.
 *
 * Data provenance is explicit via {@code sourceType}:
 *  - "seed_curated"       drug facts from resources/seed/drugs.json (curated reference, not authoritative)
 *  - formulary lines      from resources/seed/formulary.json (SYNTHETIC - invented tiers and copays)
 *  - labelExcerpt         real label text from the OpenFDA API when enrichment succeeds
 */
@Data
@Builder
public class DrugDocument {

    private String id;
    private String sourceType;
    private String genericName;
    private String brandName;
    private String drugClass;
    private String pharmacologicClass;
    private String routeOfAdministration;
    private List<String> dosageForms;
    private List<String> activeIngredients;
    private List<String> interactions;
    private List<String> contraindications;
    private List<String> allergyFlags;
    /** One line per plan, e.g. "STANDARD-PPO-2024: tier 1, copay $10" or "BASIC-HMO-2024: NOT COVERED" */
    private List<String> formulary;
    private String notes;
    /** Short excerpt of the real FDA label (OpenFDA), if available */
    private String labelExcerpt;
    private String manufacturer;
    private String ndc;

    /** Text that is embedded AND handed to Claude as retrieved context. */
    public String toEmbeddingText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Drug: ").append(brandName != null ? brandName : genericName)
          .append(". Generic name: ").append(genericName)
          .append(". Class: ").append(drugClass != null ? drugClass : "unknown")
          .append(". Pharmacologic class: ").append(pharmacologicClass != null ? pharmacologicClass : "unknown")
          .append(". Route: ").append(routeOfAdministration != null ? routeOfAdministration : "oral")
          .append(". Dosage forms: ").append(join(dosageForms, "tablet"))
          .append(". Interactions: ").append(join(interactions, "none listed"))
          .append(". Contraindications: ").append(join(contraindications, "none listed"))
          .append(". Allergy flags: ").append(join(allergyFlags, "none")).append('.');
        if (formulary != null && !formulary.isEmpty()) {
            sb.append(" Formulary (synthetic demo data): ").append(String.join("; ", formulary)).append('.');
        }
        if (notes != null && !notes.isBlank()) sb.append(" Notes: ").append(notes).append('.');
        if (labelExcerpt != null && !labelExcerpt.isBlank()) {
            sb.append(" FDA label excerpt (OpenFDA): ").append(labelExcerpt);
        }
        return sb.toString();
    }

    private static String join(List<String> v, String fallback) {
        return v != null && !v.isEmpty() ? String.join(", ", v) : fallback;
    }
}
