package com.kalapa.heuristik.interfaces.dto;

import java.util.List;

public record SalesAnalysisResponse(
        String generalResume,
        List<String> performance,
        List<String> tendencies,
        List<String> problems,
        List<String> recommendations,
        String conclusions
) {
}
