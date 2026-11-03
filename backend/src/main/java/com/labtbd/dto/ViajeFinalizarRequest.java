package com.labtbd.dto;

import jakarta.validation.constraints.NotNull;

public record ViajeFinalizarRequest(
        @NotNull Long estacionDestinoId
) {}
