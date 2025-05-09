package com.kalapa.heuristik.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesEmbeddingDTO(
        LocalDate fecha,
        String diaSemana,
        BigDecimal montoTotal,
        String clasificacion,
        String metodoPagoMasUsado,
        String categoriaMasVendida,
        float[] embedding
) {
}
