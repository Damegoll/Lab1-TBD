package com.labtbd.dto;

import jakarta.validation.constraints.Min;

public record OrdenTrasladoUpdateRequest(
        @Min(1) int cantidad
) {}
