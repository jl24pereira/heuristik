package com.kalapa.heuristik.infrastructure.client;

import com.kalapa.heuristik.domain.service.EmbeddingGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OllamaEmbeddingService implements EmbeddingGenerator {

    private final EmbeddingModel embeddingModel;

    @Override
    public float[] generateVector(String texto) {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(texto));
        return response.getResults().get(0).getOutput();
    }
}
