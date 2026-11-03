package com.labtbd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public record EstacionRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Min(1) int capacidad,
        @Min(0) int objetivoBicicletas,
        @Min(0) int objetivoScooters
) {}
