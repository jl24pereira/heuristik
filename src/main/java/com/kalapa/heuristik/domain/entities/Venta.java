package com.kalapa.heuristik.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "venta")
public class Venta {
    @Id
    @Column(name = "idventa", nullable = false)
    private Long id;

    @Column(name = "fecha")
    private Instant fecha;

    @Column(name = "tipoventa", length = 3)
    private String tipoventa;

    @Column(name = "total", precision = 8, scale = 2)
    private BigDecimal total;

    @Column(name = "gravado", precision = 8, scale = 2)
    private BigDecimal gravado;

    @Column(name = "exento", precision = 8, scale = 2)
    private BigDecimal exento;

    @Column(name = "nosujeto", precision = 8, scale = 2)
    private BigDecimal nosujeto;

    @OneToMany(mappedBy = "idventa")
    private Set<DetalleVenta> detalleVentas = new LinkedHashSet<>();

}