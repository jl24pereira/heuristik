package com.kalapa.heuristik.interfaces.dto;

import java.sql.Date;

public record VentasPorDiaDto(
        Date dia,
        Long totalVentas
) {
}
