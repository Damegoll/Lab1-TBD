package com.labtbd.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MovimientoBilleteraResponse(
        long id,
        long billeteraId,
        String tipo,
        BigDecimal monto,
        String motivo,
        String operador,
        OffsetDateTime creadoEn
) {}
