package com.labtbd.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.labtbd.entity.OrdenTraslado;
import com.labtbd.entity.TipoVehiculo;

public record OrdenTrasladoResponse(
    long id,
    UUID solicitudId,
    long estacionOrigenId,
    long estacionDestinoId,
    TipoVehiculo tipoVehiculo,
    int cantidad,
    long operadorId,
    OffsetDateTime creadaEn
) {
    public static OrdenTrasladoResponse desde(OrdenTraslado o){
        return new OrdenTrasladoResponse(o.id(),o.solicitudId(),o.estacionOrigenId(),
    o.estacionDestinoId(),o.tipoVehiculo(),o.cantidad(),o.operadorId(),o.creadaEn());
    }
}