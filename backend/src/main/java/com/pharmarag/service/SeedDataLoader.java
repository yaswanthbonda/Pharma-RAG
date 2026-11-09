package com.pharmarag.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmarag.model.DrugDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Builds DrugDocuments from the bundled seed files:
 *  - seed/drugs.json     curated drug facts (class, interactions, contraindications, allergy flags)
 *  - seed/formulary.json SYNTHETIC plan tiers/copays (invented for the demo)
 * This replaces asking the LLM to invent formulary data.
 */
@Component
public class SeedDataLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public List<DrugDocument> load() {
        try {
            SeedDrugs drugs = read("seed/drugs.json", SeedDrugs.class);
            Formulary formulary = read("seed/formulary.json", Formulary.class);

            List<DrugDocument> docs = new ArrayList<>();
            for (SeedDrug d : drugs.drugs) {
                List<String> lines = new ArrayList<>();
                formulary.plans.forEach((plan, entries) -> {
                    List<Object> e = entries.get(d.genericName);
                    lines.add(e == null
                            ? plan + ": NOT COVERED"
                            : plan + ": tier " + e.get(0) + ", copay " + e.get(1));
                });
                docs.add(DrugDocument.builder()
                        .id("seed-" + d.genericName)
                        .sourceType("seed_curated")
                        .genericName(d.genericName)
                        .brandName(d.brandName)
                        .drugClass(d.drugClass)
                        .pharmacologicClass(d.pharmacologicClass)
                        .routeOfAdministration(d.route)
                        .dosageForms(d.dosageForms)
                        .activeIngredients(List.of(d.genericName))
                        .interactions(d.interactions)
                        .contraindications(d.contraindications)
                        .allergyFlags(d.allergyFlags)
                        .formulary(lines)
                        .notes(d.notes)
                        .build());
            }
            return docs;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load seed data from classpath:seed/", e);
        }
    }

    private <T> T read(String path, Class<T> type) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing resource " + path);
            return mapper.readValue(in, type);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeedDrugs { public List<SeedDrug> drugs; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeedDrug {
        public String genericName, brandName, drugClass, pharmacologicClass, route, notes;
        public List<String> dosageForms, interactions, contraindications, allergyFlags;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Formulary {
        /** plan id -> generic name -> [tier, copay] */
        public Map<String, Map<String, List<Object>>> plans;
    }
}
