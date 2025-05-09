package com.kalapa.heuristik.interfaces.controller;

import com.kalapa.heuristik.application.service.DailySalesEmbeddingService;
import com.kalapa.heuristik.interfaces.dto.ResumenVentasEmbeddingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/embedding")
@RequiredArgsConstructor
public class EmbeddingTestController {

    private final DailySalesEmbeddingService ventasPorPeriodoService;

    @GetMapping("/resumen-mes/{mes}")
    public List<ResumenVentasEmbeddingDto> generarEmbeddingResumenMes(@PathVariable String mes) {
        return ventasPorPeriodoService.generateEmbedding(mes);
    }
}
