package com.kalapa.heuristik.domain.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ColumnDefault("nextval('detalle_venta_iddetalleventa_seq')")
    @Column(name = "iddetalleventa", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idventa", nullable = false)
    private Venta idventa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idproducto", nullable = false)
    private Producto idproducto;

    @Column(name = "cantidad", precision = 10, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "precio", precision = 10, scale = 4)
    private BigDecimal precio;

}