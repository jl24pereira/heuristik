package com.kalapa.heuristik.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalapa.heuristik.domain.entities.DailySalesSummary;
import com.kalapa.heuristik.domain.repository.DailySalesSummaryRepository;
import com.kalapa.heuristik.interfaces.dto.SalesAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesSummaryAiService {

    private final DailySalesSummaryRepository dailySalesSummaryRepository;
    private final VectorStore vectorStore;
    private final OpenAiChatModel openAiChatModel;

    /**
     * Genera y almacena embeddings vectoriales de ventas diarias para un mes específico.
     */
    public String generateEmbedding(String month) {
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("MM-yyyy"));
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<DailySalesSummary> summaries = dailySalesSummaryRepository.findByDayBetween(start, end);

        List<Document> documents = summaries.stream()
                .filter(summary -> !existsVectorForDay(summary.getDay())) // Solo los nuevos
                .map(this::mapToDocument)
                .collect(Collectors.toList());

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }

        return "Vectores almacenados exitosamente";
    }

    /**
     * Genera un análisis mensual en formato estructurado JSON a partir de embeddings almacenados.
     */
    public SalesAnalysisResponse generateAISummary(String month) throws JsonProcessingException {
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("MM-yyyy"));
        String monthName = getMonthName(yearMonth.getMonth());

        SearchRequest request = SearchRequest.builder()
                .query("Resumen del mes: " + monthName)
                .topK(yearMonth.lengthOfMonth())
                .filterExpression(new FilterExpressionBuilder().eq("monthName", monthName).build())
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);
        String resume = documents.stream().map(Document::getText).collect(Collectors.joining("\n"));

        Prompt prompt = new Prompt(
                getAnalysisPrompt(resume),
                OpenAiChatOptions.builder()
                        .responseFormat(new ResponseFormat(ResponseFormat.Type.JSON_SCHEMA, getJsonSchema()))
                        .build()
        );

        ChatResponse response = openAiChatModel.call(prompt);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(response.getResult().getOutput().getText(), SalesAnalysisResponse.class);
    }

    private boolean existsVectorForDay(LocalDate day) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query("verificar existencia")
                .topK(1)
                .filterExpression(b.eq("day", day.toString()).build())
                .build();

        List<Document> docs = vectorStore.similaritySearch(request);
        return !docs.isEmpty();
    }

    private Document mapToDocument(DailySalesSummary summary) {
        String text = generateResume(summary);
        Map<String, Object> map = new HashMap<>();
        map.put("day", summary.getDay().toString());
        map.put("weekday", getDayName(summary.getDay().getDayOfWeek()));
        map.put("month", String.format("%02d", summary.getDay().getMonthValue()));
        map.put("monthName", getMonthName(summary.getDay().getMonth()));
        map.put("dayRating", summary.getDayRating());
        map.put("topProduct", summary.getTopProduct());
        map.put("totalSold", summary.getTotalSold());
        map.put("topCategory", summary.getTopCategory());
        map.put("topPaymentMethod", summary.getTopPaymentMethod());
        map.put("peakTimeSlot", summary.getPeakTimeSlot());
        map.put("totalAmount", summary.getTotalAmount());
        map.put("transactionCount", summary.getTransactionCount());
        return new Document(text, map);
    }

    private String generateResume(DailySalesSummary r) {
        return String.format(
                "El día %s, %02d de %s de %d hubo %d transacciones por un monto total de %.2f. " +
                        "Este día fue clasificado como '%s'. El producto más vendido fue '%s' con %d unidades. " +
                        "El horario pico fue '%s', el método de pago más usado fue '%s' y la categoría más vendida fue '%s'.",
                getDayName(r.getDay().getDayOfWeek()),
                r.getDay().getDayOfMonth(),
                getMonthName(r.getDay().getMonth()),
                r.getDay().getYear(),
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

    private String getAnalysisPrompt(String resume) {
        return """
                Eres un analista de ventas. Dado el siguiente resumen de días de ventas, genera un análisis ejecutivo del desempeño del mes,
                incluyendo sugerencias de mercadeo y administrativas, posibles problemas y recomendaciones.
                
                %s
                """.formatted(resume);
    }

    private String getJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "generalResume": { "type": "string" },
                    "performance": { "type": "array", "items": { "type": "string" } },
                    "tendencies": { "type": "array", "items": { "type": "string" } },
                    "problems": { "type": "array", "items": { "type": "string" } },
                    "recommendations": { "type": "array", "items": { "type": "string" } },
                    "conclusions": { "type": "string" }
                  },
                  "required": ["generalResume", "performance", "tendencies", "problems", "recommendations", "conclusions"],
                  "additionalProperties": false
                }
                """;
    }

    private String getMonthName(Month mes) {
        return switch (mes) {
            case JANUARY -> "enero";
            case FEBRUARY -> "febrero";
            case MARCH -> "marzo";
            case APRIL -> "abril";
            case MAY -> "mayo";
            case JUNE -> "junio";
            case JULY -> "julio";
            case AUGUST -> "agosto";
            case SEPTEMBER -> "septiembre";
            case OCTOBER -> "octubre";
            case NOVEMBER -> "noviembre";
            case DECEMBER -> "diciembre";
        };
    }

    private String getDayName(DayOfWeek dia) {
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
