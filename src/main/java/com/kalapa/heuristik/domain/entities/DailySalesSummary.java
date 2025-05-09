package com.kalapa.heuristik.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mapping for DB view
 */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "daily_sales_summary")
public class DailySalesSummary {
    @Id
    @Column(name = "day")
    private LocalDate day;

    @Column(name = "transaction_count")
    private Long transactionCount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "day_rating", length = Integer.MAX_VALUE)
    private String dayRating;

    @Column(name = "top_product", length = Integer.MAX_VALUE)
    private String topProduct;

    @Column(name = "total_sold")
    private Long totalSold;

    @Column(name = "peak_time_slot", length = Integer.MAX_VALUE)
    private String peakTimeSlot;

    @Column(name = "top_payment_method", length = Integer.MAX_VALUE)
    private String topPaymentMethod;

    @Column(name = "top_category", length = Integer.MAX_VALUE)
    private String topCategory;

}