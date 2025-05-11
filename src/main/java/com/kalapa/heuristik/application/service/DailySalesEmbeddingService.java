package com.kalapa.heuristik.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalapa.heuristik.domain.entities.DailySalesSummary;
import com.kalapa.heuristik.domain.repository.DailySalesSummaryRepository;
import com.kalapa.heuristik.interfaces.dto.SalesAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
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
public class DailySalesEmbeddingService {

    private final DailySalesSummaryRepository dailySalesSummaryRepository;
    private final VectorStore vectorStore;
    private final OpenAiChatModel openAiChatModel;

    public String generateEmbedding(String month) {
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("MM-yyyy"));
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<DailySalesSummary> summaries = dailySalesSummaryRepository.findByDayBetween(start, end);

        List<Document> documents = summaries.stream().map(summary -> {
            String text = generateResume(summary);
            Map<String, Object> map = new HashMap<>();
            map.put("day", summary.getDay().toString());
            map.put("weekday", diaEnEspanol(summary.getDay().getDayOfWeek()));
            map.put("month", String.format("%02d", summary.getDay().getMonthValue()));
            map.put("monthName", mesEnEspanol(summary.getDay().getMonth()));
            map.put("dayRating", summary.getDayRating());
            map.put("topProduct", summary.getTopProduct());
            map.put("totalSold", summary.getTotalSold());
            map.put("topCategory", summary.getTopCategory());
            map.put("topPaymentMethod", summary.getTopPaymentMethod());
            map.put("peakTimeSlot", summary.getPeakTimeSlot());
            map.put("totalAmount", summary.getTotalAmount());
            map.put("transactionCount", summary.getTransactionCount());
            return new Document(text, map);
        }).collect(Collectors.toList());

        vectorStore.add(documents);
        return "Finaliza procedimiento";
    }

    public List<Document> generateMessage(String prompt) {
        return vectorStore.similaritySearch(SearchRequest.builder().query(prompt).topK(5).build());
    }

    public SalesAnalysisResponse generateAISummary(String month) throws JsonProcessingException {
        YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("MM-yyyy"));
        String filter = "month == '" + mesEnEspanol(yearMonth.getMonth()) + "'";
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        SearchRequest request = SearchRequest.builder()
                .query("Resumen del mes")
                .topK(yearMonth.lengthOfMonth())
                .filterExpression(b.eq("monthName", mesEnEspanol(yearMonth.getMonth())).build())
                .topK(yearMonth.lengthOfMonth())
                .build();
        List<Document> documents = vectorStore.similaritySearch(request);

        String resume = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        log.info("Total documentos encontrados: {}", documents.size());

        String prompt = """
                Eres un analista de ventas. Dado el siguiente resumen de días de ventas, genera un análisis ejecutivo del desempeño del mes. 
                Devuelve únicamente un JSON con la siguiente estructura, en español, y sin ningún texto adicional fuera del JSON.
                
                Las llaves deben ser exactamente: ["generalResume", "performance", "tendencies", "problems", "recomendations", "conclusions"].
                Cada una debe contener texto o un arreglo de frases si corresponde, según consideres adecuado.
                
                Resumen de ventas:
                %s
                """.formatted(resume);

        log.info(prompt);

        String responseFromOpenAi = openAiChatModel.call(prompt);
        String rawJson = responseFromOpenAi
                .replace("```json", "")
                .replace("```", "")
                .trim();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(rawJson, SalesAnalysisResponse.class);
    }

    private String generateResume(DailySalesSummary r) {
        DayOfWeek dayOfWeek = r.getDay().getDayOfWeek();
        int dayOfMonth = r.getDay().getDayOfMonth();
        Month month = r.getDay().getMonth();
        int year = r.getDay().getYear();

        String diaNombre = diaEnEspanol(dayOfWeek);
        String mesNombre = mesEnEspanol(month);

        String fechaFormateada = String.format("%s, %02d de %s de %d", diaNombre, dayOfMonth, mesNombre, year);

        return String.format(
                "El día %s hubo %d transacciones por un monto total de %.2f. " +
                        "Este día fue clasificado como '%s'. El producto más vendido fue '%s' con %d unidades. " +
                        "El horario pico fue '%s', el método de pago más usado fue '%s' y la categoría más vendida fue '%s'.",
                fechaFormateada,
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

    private String mesEnEspanol(Month mes) {
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

    private String diaEnEspanol(DayOfWeek dia) {
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
