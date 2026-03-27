package com.pharmarag.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents a drug record stored in ChromaDB.
 * Sourced from OpenFDA API.
 */
@Data
@Builder
public class DrugDocument {

    private String id;
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
    private String manufacturer;
    private String ndc;

    /** Pre-formatted text used for embedding */
    public String toEmbeddingText() {
        return String.format(
            "Drug: %s. Generic name: %s. Class: %s. Pharmacologic class: %s. " +
            "Route: %s. Dosage forms: %s. Active ingredients: %s. " +
            "Interactions: %s. Contraindications: %s. Allergy flags: %s.",
            brandName != null ? brandName : genericName,
            genericName,
            drugClass != null ? drugClass : "unknown",
            pharmacologicClass != null ? pharmacologicClass : "unknown",
            routeOfAdministration != null ? routeOfAdministration : "oral",
            dosageForms != null ? String.join(", ", dosageForms) : "tablet",
            activeIngredients != null ? String.join(", ", activeIngredients) : genericName,
            interactions != null ? String.join(", ", interactions) : "none listed",
            contraindications != null ? String.join(", ", contraindications) : "none listed",
            allergyFlags != null ? String.join(", ", allergyFlags) : "none"
        );
    }
}
