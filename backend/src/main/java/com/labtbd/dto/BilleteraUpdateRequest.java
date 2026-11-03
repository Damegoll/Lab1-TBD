package com.labtbd.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BilleteraUpdateRequest(
        @NotNull @DecimalMin("0.00") BigDecimal saldo,
        @NotBlank @Size(max = 500) String motivo
) {}
