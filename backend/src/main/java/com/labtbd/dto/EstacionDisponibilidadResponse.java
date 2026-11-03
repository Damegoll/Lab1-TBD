package com.labtbd.dto;

public record EstacionDisponibilidadResponse(
        Long estacionId,
        String estacionNombre,
        String tipoVehiculo,
        Long cantidad,
        Double bateriaPromedio
) {}