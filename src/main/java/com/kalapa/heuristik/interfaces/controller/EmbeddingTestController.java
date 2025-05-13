package com.kalapa.heuristik.interfaces.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kalapa.heuristik.application.service.SalesSummaryAiService;
import com.kalapa.heuristik.interfaces.dto.SalesAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/sales")
@RequiredArgsConstructor
public class EmbeddingTestController {

    private final SalesSummaryAiService salesSummaryAiService;

    @PostMapping("/embeddings")
    public String generateMonthlyEmbeddings(@RequestParam("month") String month) {
        return salesSummaryAiService.generateEmbedding(month);
    }

    @GetMapping("/analysis")
    public SalesAnalysisResponse getMonthlySalesAnalysis(@RequestParam("month") String month) throws JsonProcessingException {
        return salesSummaryAiService.generateAISummary(month);
    }
}
