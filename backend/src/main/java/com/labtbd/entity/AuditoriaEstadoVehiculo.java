package com.labtbd.entity;

import java.time.OffsetDateTime;

public record AuditoriaEstadoVehiculo(
    long id,
    long vehiculoId,
    EstadoVehiculo estadoAnterior,
    EstadoVehiculo estadoNuevo,
    String operador,
    OffsetDateTime fecha,
    String motivo
) {}