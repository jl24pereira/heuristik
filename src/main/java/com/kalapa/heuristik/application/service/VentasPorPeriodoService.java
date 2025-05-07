package com.kalapa.heuristik.application.service;

import com.kalapa.heuristik.domain.repository.VentaRepository;
import com.kalapa.heuristik.domain.service.EmbeddingGenerator;
import com.kalapa.heuristik.interfaces.dto.ResumenVentasEmbeddingDto;
import com.kalapa.heuristik.interfaces.dto.VentasPorDiaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentasPorPeriodoService {

    private final VentaRepository ventaRepository;
    private final EmbeddingGenerator embeddingGenerator;

    public ResumenVentasEmbeddingDto generarEmbedding(String mes) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy");
        YearMonth yearMonth = YearMonth.parse(mes, formatter);

        ZoneId zona = ZoneId.systemDefault(); // o ZoneOffset.UTC si prefieres

        Instant inicio = yearMonth.atDay(1).atStartOfDay(zona).toInstant();
        Instant fin = yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zona).toInstant();

        List<VentasPorDiaDto> ventasPorDia = ventaRepository.findVentasAgrupadasPorDia(inicio, fin);

        if (ventasPorDia.isEmpty()) {
            return new ResumenVentasEmbeddingDto(
                    "No hay datos de ventas para el mes " + mes,
                    new float[0]
            );
        }

        VentasPorDiaDto mejorDia = ventasPorDia.get(0);
        VentasPorDiaDto peorDia = ventasPorDia.get(ventasPorDia.size() - 1);

        StringBuilder resumen = new StringBuilder();
        resumen.append("Durante el mes ").append(mes).append(": ");
        resumen.append("El día con más ventas fue ")
                .append(mejorDia.dia()).append(" (")
                .append(diaEnEspañol(mejorDia.dia().toLocalDate().getDayOfWeek())).append(") con ")
                .append(mejorDia.totalVentas()).append(" transacciones. ");
        resumen.append("El día con menos ventas fue ")
                .append(peorDia.dia()).append(" (")
                .append(diaEnEspañol(peorDia.dia().toLocalDate().getDayOfWeek())).append(") con ")
                .append(peorDia.totalVentas()).append(" transacciones. ");
        resumen.append("Esto podría indicar que los ")
                .append(diaEnEspañol(mejorDia.dia().toLocalDate().getDayOfWeek()))
                .append("s son fuertes comercialmente, mientras que los ")
                .append(diaEnEspañol(peorDia.dia().toLocalDate().getDayOfWeek()))
                .append("s podrían requerir incentivos o promociones.");

        float[] embedding = embeddingGenerator.generateVector(resumen.toString());

        return new ResumenVentasEmbeddingDto(resumen.toString(), embedding);
    }

    private String diaEnEspañol(DayOfWeek dia) {
        return switch (dia) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miércoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }
}
