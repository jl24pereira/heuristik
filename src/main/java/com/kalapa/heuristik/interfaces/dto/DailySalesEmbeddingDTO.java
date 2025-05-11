package com.kalapa.heuristik.interfaces.dto;

public record DailySalesEmbeddingDTO(
        String resumen,
        float[] embedding
) {
}
