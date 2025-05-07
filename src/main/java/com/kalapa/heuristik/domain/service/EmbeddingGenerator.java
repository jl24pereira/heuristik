package com.kalapa.heuristik.domain.service;

import java.util.List;

public interface EmbeddingGenerator {
    float[] generateVector(String texto);
}
