package com.labtbd.dto;

import jakarta.validation.constraints.NotNull;

public record ViajeIniciarRequest(
        @NotNull Long vehiculoId
) {}
