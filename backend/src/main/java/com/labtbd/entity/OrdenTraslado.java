package com.labtbd.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrdenTraslado(
    long id,
    UUID solicitudId,
    long estacionOrigenId,
    long estacionDestinoId,
    TipoVehiculo tipoVehiculo,
    int cantidad,
    long operadorId,
    OffsetDateTime creadaEn
) {}
