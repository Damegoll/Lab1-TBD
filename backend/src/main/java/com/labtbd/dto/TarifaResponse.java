package com.labtbd.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.labtbd.entity.TipoVehiculo;
import com.labtbd.entity.Tarifa;

public record TarifaResponse(
    long id,
    TipoVehiculo tipoVehiculo,
    BigDecimal precioPorMinuto,
    OffsetDateTime vigenteDesde,
    OffsetDateTime vigenteHasta,
    boolean activa
) {
    public static TarifaResponse desde(Tarifa t){
        return new TarifaResponse(t.id(),t.tipoVehiculo(),t.precioPorMinuto(),t.vigenteDesde(),
    t.vigenteHasta(),t.activa());
    }
}