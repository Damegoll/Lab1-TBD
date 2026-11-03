package com.labtbd.dto;

import com.labtbd.entity.TipoVehiculo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VehiculoCreateRequest(
    @NotBlank
    @Size(max=40)
    String codigo,

    @NotNull
    TipoVehiculo tipo,

    @Min(0)
    @Max(100)
    int bateria,

    @NotNull
    Long estacionId
) {}
