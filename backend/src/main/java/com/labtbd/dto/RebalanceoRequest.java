package com.labtbd.dto;

import com.labtbd.entity.TipoVehiculo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RebalanceoRequest(
    @NotNull
    Long estacionOrigenId,

    @NotNull
    Long estacionDestinoId,

    @NotNull
    TipoVehiculo tipoVehiculo,

    @Min(1)
    int cantidad
) {}