package com.kalapa.heuristik.domain.repository;

import com.kalapa.heuristik.domain.entities.DailySalesSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailySalesSummaryRepository extends JpaRepository<DailySalesSummary, LocalDate> {

    List<DailySalesSummary> findByDayBetween(LocalDate dayStart, LocalDate dayEnd);

}