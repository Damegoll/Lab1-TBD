package com.labtbd.dto;

import com.labtbd.entity.Viaje;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ViajeResponse(
        long id,
        long usuarioId,
        long vehiculoId,
        Long estacionOrigenId,
        Long estacionDestinoId,
        Long tarifaId,
        BigDecimal precioPorMinutoAplicado,
        OffsetDateTime iniciadoEn,
        OffsetDateTime finalizadoEn,
        BigDecimal costo,
        boolean cancelado
) {
    public static ViajeResponse desde(Viaje v) {
        return new ViajeResponse(v.id(), v.usuarioId(), v.vehiculoId(),
                v.estacionOrigenId(), v.estacionDestinoId(), v.tarifaId(),
                v.precioPorMinutoAplicado(), v.iniciadoEn(), v.finalizadoEn(),
                v.costo(), v.cancelado());
    }
}
