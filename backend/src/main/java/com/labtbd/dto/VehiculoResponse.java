package com.labtbd.dto;

import com.labtbd.entity.EstadoVehiculo;
import com.labtbd.entity.TipoVehiculo;
import com.labtbd.entity.Vehiculo;

public record VehiculoResponse(
    long id,
    String codigo,
    TipoVehiculo tipo,
    EstadoVehiculo estado,
    int bateria,
    Long estacionId
) {
    public static VehiculoResponse desde(Vehiculo v){
        return new VehiculoResponse(v.id(), v.codigo(), v.tipo(),
         v.estado(), v.bateria(), v.estacionId());
    }
}