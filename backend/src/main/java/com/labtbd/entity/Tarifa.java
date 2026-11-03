package com.labtbd.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Tarifa(
    long id,
    TipoVehiculo tipoVehiculo,
    BigDecimal precioPorMinuto,
    OffsetDateTime vigenteDesde,
    OffsetDateTime vigenteHasta,
    boolean activa
) {}