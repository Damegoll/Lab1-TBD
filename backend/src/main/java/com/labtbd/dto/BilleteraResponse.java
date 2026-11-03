package com.labtbd.dto;

import com.labtbd.entity.Billetera;
import java.math.BigDecimal;

public record BilleteraResponse(long id, long usuarioId, BigDecimal saldo, boolean activa) {
    public static BilleteraResponse desde(Billetera b) {
        return new BilleteraResponse(b.id(), b.usuarioId(), b.saldo(), b.activa());
    }
}
