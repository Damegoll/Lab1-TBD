package com.labtbd.dto;

import com.labtbd.entity.EstadoVehiculo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarEstadoRequest(
    @NotNull
    EstadoVehiculo estado,

    @NotBlank
    @Size(max=500)
    String motivo
) {}