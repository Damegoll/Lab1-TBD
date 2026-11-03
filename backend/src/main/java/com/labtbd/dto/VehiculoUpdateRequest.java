package com.labtbd.dto;

import com.labtbd.entity.TipoVehiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record VehiculoUpdateRequest(
    @NotBlank
    @Size(max=40)
    String codigo,

    @Min(0)
    @Max(100)
    int bateria,

    TipoVehiculo tipo,

    @Positive
    Long estacionId
) {}