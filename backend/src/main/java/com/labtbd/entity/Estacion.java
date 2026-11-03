package com.labtbd.entity;

public record Estacion(
    long id,
    String nombre,
    boolean activa,
    int capacidad,
    int objetivoBicicletas,
    int objetivoScooters
) {}