package com.kalapa.heuristik.interfaces.dto;

public record ResumenVentasEmbeddingDto(
        String resumen,
        float[] embedding
) {
}
