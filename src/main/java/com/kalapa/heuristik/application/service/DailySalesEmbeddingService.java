package com.kalapa.heuristik.application.service;

import com.kalapa.heuristik.domain.entities.DailySalesSummary;
import com.kalapa.heuristik.domain.repository.DailySalesSummaryRepository;
import com.kalapa.heuristik.domain.service.EmbeddingGenerator;
import com.kalapa.heuristik.interfaces.dto.ResumenVentasEmbeddingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailySalesEmbeddingService {

        private final DailySalesSummaryRepository dailySalesSummaryRepository;
    private final EmbeddingGenerator embeddingGenerator;

    public List<ResumenVentasEmbeddingDto> generateEmbedding(String month) {
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("MM-yyyy"));
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<DailySalesSummary> summaries = dailySalesSummaryRepository.findByDayBetween(start, end);
        int newTest = 1;

        return summaries.stream()
                .map(summary -> {
                    String text = generateResume(summary);
                    float[] embedding = embeddingGenerator.generateVector(text);
                    return new ResumenVentasEmbeddingDto(text, embedding);
                    //return new ResumenVentasEmbeddingDto(summary.getDay(), diaEnEspañol(summary.getDay().getDayOfWeek()), summary.getTotalAmount(), summary.getDayRating(), summary.getTopPaymentMethod(), summary.getTopCategory(), text, embedding);
                })
                .collect(Collectors.toList());
    }

    private String generateResume(DailySalesSummary r) {
        return String.format(
                "El día %s hubo %d transacciones por un monto total de %.2f. " +
                        "Este día fue clasificado como '%s'. El producto más vendido fue '%s' con %d unidades. " +
                        "El horario pico fue '%s', el método de pago más usado fue '%s' y la categoría más vendida fue '%s'.",
                r.getDay(),
                r.getTransactionCount(),
                r.getTotalAmount(),
                r.getDayRating(),
                r.getTopProduct(),
                r.getTotalSold(),
                r.getPeakTimeSlot(),
                r.getTopPaymentMethod(),
                r.getTopCategory()
        );
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
