package com.labtbd.dto;

import java.time.OffsetDateTime;

import com.labtbd.entity.AuditoriaEstadoVehiculo;
import com.labtbd.entity.EstadoVehiculo;

public record AuditoriaResponse(
    long id,
    long vehiculoId,
    EstadoVehiculo estadoAnterior,
    EstadoVehiculo estadoNuevo,
    String operador,
    OffsetDateTime fecha,
    String motivo
) {
    public static AuditoriaResponse desde(AuditoriaEstadoVehiculo a){
        return new AuditoriaResponse(a.id(),a.vehiculoId(),a.estadoAnterior(),
    a.estadoNuevo(),a.operador(),a.fecha(),a.motivo());
    }
}