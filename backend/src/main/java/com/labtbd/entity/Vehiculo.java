package com.labtbd.entity;

public record Vehiculo(
    long id,
    String codigo,
    TipoVehiculo tipo,
    EstadoVehiculo estado,
    int bateria,
    Long estacionId
) {}