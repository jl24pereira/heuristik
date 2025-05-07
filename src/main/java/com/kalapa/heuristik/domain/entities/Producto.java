package com.kalapa.heuristik.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "producto")
public class Producto {
    @Id
    @Column(name = "idproducto", nullable = false)
    private Long id;

    @Column(name = "barcode", length = 50)
    private String barcode;

    @Column(name = "nombre", length = Integer.MAX_VALUE)
    private String nombre;

    @Column(name = "costo", precision = 10, scale = 4)
    private BigDecimal costo;

    @Column(name = "stock", precision = 10, scale = 4)
    private BigDecimal stock;

    @Column(name = "pv1", precision = 10, scale = 4)
    private BigDecimal pv1;

    @Column(name = "pv2", precision = 10, scale = 4)
    private BigDecimal pv2;

    @Column(name = "pv3", precision = 10, scale = 4)
    private BigDecimal pv3;

}