package com.labtbd.entity;

import java.math.BigDecimal;

public record Billetera(
    long id,
    long usuarioId,
    BigDecimal saldo,
    boolean activa
) {}