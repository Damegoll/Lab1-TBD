package com.labtbd.dto;

import com.labtbd.entity.EstadoVehiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record AuditoriaCreateRequest(
        @NotNull Long vehiculoId,
        EstadoVehiculo estadoAnterior,
        @NotNull EstadoVehiculo estadoNuevo,
        @NotNull Long operadorId,
        @NotNull OffsetDateTime fecha,
        @NotBlank @Size(max = 500) String motivo
) {}
