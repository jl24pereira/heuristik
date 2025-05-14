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
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
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

    private static final ObjectMapper JSON = new ObjectMapper();

    /**
     * Genera y almacena embeddings vectoriales de ventas diarias para un mes específico.
     */
    public String generateEmbedding(String month) {
        YearMonth yearMonth = parseMonthStrict(month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<DailySalesSummary> summaries = dailySalesSummaryRepository.findByDayBetween(start, end);

        List<Document> documents = summaries.stream()
                .filter(summary -> !existsVectorForDay(summary.getDay()))
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
        YearMonth yearMonth = parseMonthStrict(month);
        String monthName = getMonthName(yearMonth.getMonth());

        SearchRequest request = SearchRequest.builder()
                .query("Resumen del mes: " + monthName)
                .topK(yearMonth.lengthOfMonth())
                .filterExpression(new FilterExpressionBuilder().eq("monthName", monthName).build())
                .build();

        List<Document> documents = vectorStore.similaritySearch(request);
        if (documents.isEmpty()) {
            throw new IllegalStateException("No se encontraron embeddings para " + month);
        }

        Map<String, Object> metricsMap = computeMetricsFromMetadata(documents);
        String metricsJson = JSON.writeValueAsString(metricsMap);

        List<String> dailyNarratives = documents.stream()
                .map(Document::getText)
                .toList();

        String promptBody = buildPrompt(metricsJson, dailyNarratives);
        Prompt prompt = new Prompt(promptBody,
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .temperature(0.2)
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

    private String buildPrompt(String metricsJson, List<String> narratives) {
        String prompt = """
                <system>
                Eres un analista de ventas senior. Razona paso a paso internamente,
                pero devuelve SOLO un JSON válido conforme al esquema.
                </system>
                
                <user>
                ### Datos numéricos del mes (garantizados correctos)
                %s
                
                ### Detalle narrativo por día (no modifiques este bloque)
                %s
                
                ### Tareas
                1. Verifica que los totales diarios sumen el monto y transacciones
                   indicados en los datos numéricos.
                2. Identifica tendencias, retos y oportunidades.
                3. Propón acciones de mercadeo y administrativas (máx. 3 por sección).
                4. Completa las claves del esquema JSON.
                
                Devuelve ÚNICAMENTE el JSON.
                </user>
                """.formatted(metricsJson, String.join("\n", narratives));
        log.info(prompt);
        return prompt;
    }

    private Map<String, Object> computeMetricsFromMetadata(List<Document> docs) {
        int txTotal = 0;
        double amtTotal = 0d;
        Map<String, Long> ratingDist = new HashMap<>();
        Map<String, Long> paymentFreq = new HashMap<>();
        Map<String, Long> categoryFreq = new HashMap<>();

        for (Document d : docs) {
            Map<String, Object> m = d.getMetadata();
            txTotal += ((Number) m.get("transactionCount")).intValue();
            amtTotal += ((Number) m.get("totalAmount")).doubleValue();

            ratingDist.merge((String) m.get("dayRating"), 1L, Long::sum);
            paymentFreq.merge((String) m.get("topPaymentMethod"), 1L, Long::sum);
            categoryFreq.merge((String) m.get("topCategory"), 1L, Long::sum);
        }

        double avgTicket = txTotal == 0 ? 0 : amtTotal / txTotal;

        return Map.of(
                "transactionTotal", txTotal,
                "amountTotal", amtTotal,
                "avgTicket", avgTicket,
                "ratingDistribution", ratingDist,
                "topPaymentMethods", topN(paymentFreq, 3),
                "topCategories", topN(categoryFreq, 3)
        );
    }

    private List<String> topN(Map<String, Long> freq, int n) {
        return freq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }

    private String getJsonSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "generalResume":    { "type": "string" },
                    "performance":      { "type": "array", "items": { "type": "string" } },
                    "tendencies":       { "type": "array", "items": { "type": "string" } },
                    "problems":         { "type": "array", "items": { "type": "string" } },
                    "recommendations":  { "type": "array", "items": { "type": "string" } },
                    "conclusions":      { "type": "string" }
                  },
                  "required": [
                    "generalResume",
                    "performance",
                    "tendencies",
                    "problems",
                    "recommendations",
                    "conclusions"
                  ],
                  "additionalProperties": false
                }
                """;
    }

    private YearMonth parseMonthStrict(String raw) {
        DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM-yyyy");
        try {
            return YearMonth.parse(raw, MONTH_FMT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Formato de mes inválido: «" + raw +
                            "». Usa el formato MM-yyyy, por ej. «01-2024».", ex);
        }
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
