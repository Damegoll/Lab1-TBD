package com.labtbd.dto;

import jakarta.validation.constraints.NotNull;

public record BilleteraCreateRequest(
        @NotNull Long usuarioId
) {}
