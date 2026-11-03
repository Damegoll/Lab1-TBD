package com.labtbd.dto;

import com.labtbd.entity.TipoVehiculo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TarifaRequest(
    @NotNull
    TipoVehiculo tipoVehiculo,

    @NotNull
    @DecimalMin("0.00")
    BigDecimal precioPorMinuto,

    @NotNull
    OffsetDateTime vigenteDesde,
    OffsetDateTime vigenteHasta
) {}