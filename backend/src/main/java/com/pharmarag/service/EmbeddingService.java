package com.pharmarag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Real semantic embeddings, computed in-process with the all-MiniLM-L6-v2 ONNX model
 * (via LangChain4j). 384 dimensions, no API key, and no text leaves the JVM -
 * a useful property when the text describes medications.
 *
 * The same instance embeds both the drug documents (ingestion) and the incoming
 * query (retrieval); using one model for both is what keeps the vectors comparable.
 */
@Service
@Slf4j
public class EmbeddingService {

    public static final int DIMENSIONS = 384;

    // Loading the ONNX model is expensive: create it once and reuse it.
    private final EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();

    public List<Double> embed(String text) {
        Embedding embedding = model.embed(text).content();
        float[] vector = embedding.vector();
        List<Double> result = new ArrayList<>(vector.length);
        for (float v : vector) {
            result.add((double) v);
        }
        return result;
    }
}
