package com.labtbd.dto;

public record ResumenMensualResponse(
        Integer anio,
        Integer mes,
        Long cantidadViajes,
        Double minutosTotales,
        Double gastoTotal
) {}