package com.kalapa.heuristik.interfaces.controller;

import com.kalapa.heuristik.application.service.VentasPorPeriodoService;
import com.kalapa.heuristik.interfaces.dto.ResumenVentasEmbeddingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/embedding")
@RequiredArgsConstructor
public class EmbeddingTestController {

    private final VentasPorPeriodoService ventasPorPeriodoService;

    @GetMapping("/resumen-mes/{mes}")
    public ResumenVentasEmbeddingDto generarEmbeddingResumenMes(@PathVariable String mes) {
        return ventasPorPeriodoService.generarEmbedding(mes);
    }
}
