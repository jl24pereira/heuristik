package com.kalapa.heuristik.interfaces.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kalapa.heuristik.application.service.DailySalesEmbeddingService;
import com.kalapa.heuristik.interfaces.dto.SalesAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/sales")
@RequiredArgsConstructor
public class EmbeddingTestController {

    private final DailySalesEmbeddingService ventasPorPeriodoService;

    @GetMapping("/generate/{mes}")
    public String generarEmbeddingResumenMes(@PathVariable String mes) {
        return ventasPorPeriodoService.generateEmbedding(mes);
    }

    @GetMapping("/prompt/{prompt}")
    public List<Document> buscar(@PathVariable String prompt) {
        return ventasPorPeriodoService.generateMessage(prompt);
    }

    @GetMapping("/summary/{month}")
    public SalesAnalysisResponse analizarPorMes(@PathVariable String month) throws JsonProcessingException {
        return ventasPorPeriodoService.generateAISummary(month);
    }
}
