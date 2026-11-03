package com.labtbd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehiculoRetirarRequest(
        @NotBlank @Size(max = 500) String motivo
) {}
