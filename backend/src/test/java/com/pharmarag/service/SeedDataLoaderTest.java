package com.pharmarag.service;

import com.pharmarag.model.DrugDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SeedDataLoaderTest {

    private final List<DrugDocument> docs = new SeedDataLoader().load();

    @Test
    void coversAllTenDrugClasses() {
        Set<String> classes = docs.stream().map(DrugDocument::getDrugClass).collect(Collectors.toSet());
        assertThat(classes).containsExactlyInAnyOrder(
                "statin", "beta blocker", "ACE inhibitor", "antibiotic", "proton pump inhibitor",
                "antidepressant", "antihistamine", "blood thinner", "calcium channel blocker", "diuretic");
    }

    @Test
    void idsAreUnique() {
        assertThat(docs.stream().map(DrugDocument::getId).distinct().count()).isEqualTo(docs.size());
    }

    @Test
    void everyDrugHasAFormularyLineForEveryPlan() {
        assertThat(docs).allSatisfy(d -> assertThat(d.getFormulary()).hasSize(2));
    }

    @Test
    void embeddingTextCarriesFormularyAndAllergyData() {
        DrugDocument amox = docs.stream().filter(d -> d.getGenericName().equals("amoxicillin")).findFirst().orElseThrow();
        String text = amox.toEmbeddingText();
        assertThat(text).contains("STANDARD-PPO-2024: tier 1").contains("penicillin").contains("synthetic");
    }

    @Test
    void someDrugsAreNotCoveredOnTheHmoPlanSoNoCoveredSubstituteIsDemoable() {
        assertThat(docs.stream().anyMatch(d -> d.getFormulary().contains("BASIC-HMO-2024: NOT COVERED"))).isTrue();
    }
}
