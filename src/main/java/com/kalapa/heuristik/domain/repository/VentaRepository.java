package com.kalapa.heuristik.domain.repository;

import com.kalapa.heuristik.domain.entities.Venta;
import com.kalapa.heuristik.interfaces.dto.VentasPorDiaDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    @Query(value = """
         SELECT
                    CAST(v.fecha AS date), 
                    COUNT(v)
                FROM Venta v
                WHERE v.fecha BETWEEN :inicio AND :fin
                GROUP BY CAST(v.fecha AS date)
                ORDER BY COUNT(v) DESC;
        """, nativeQuery = true)
    List<VentasPorDiaDto> findVentasAgrupadasPorDia(Instant inicio, Instant fin);
}