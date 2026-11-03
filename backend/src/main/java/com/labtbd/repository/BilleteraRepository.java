package com.labtbd.repository;

import com.labtbd.dto.MovimientoBilleteraResponse;
import com.labtbd.entity.Billetera;
import com.labtbd.entity.TipoMovimientoBilletera;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BilleteraRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Billetera> mapper = (rs, rowNum) -> new Billetera(
            rs.getLong("id"), rs.getLong("usuario_id"),
            rs.getBigDecimal("saldo"), rs.getBoolean("activa"));

    public BilleteraRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Billetera> findAll() {
        return jdbc.query("""
                SELECT id, usuario_id, saldo, activa
                FROM billeteras ORDER BY id
                """, mapper);
    }

    public Optional<Billetera> findById(long id) {
        return jdbc.query("""
                SELECT id, usuario_id, saldo, activa
                FROM billeteras WHERE id = ?
                """, mapper, id).stream().findFirst();
    }

    public Optional<Billetera> lockById(long id) {
        return jdbc.query("""
                SELECT id, usuario_id, saldo, activa
                FROM billeteras WHERE id = ? FOR UPDATE
                """, mapper, id).stream().findFirst();
    }

    public Optional<Billetera> findByUsuarioId(long usuarioId) {
        return jdbc.query("""
                SELECT id, usuario_id, saldo, activa
                FROM billeteras WHERE usuario_id = ?
                """, mapper, usuarioId).stream().findFirst();
    }

    public List<MovimientoBilleteraResponse> findMovimientos(long walletId) {
        return jdbc.query("""
                SELECT m.id, m.billetera_id, m.tipo, m.monto, m.motivo,
                       COALESCE(u.username, 'SISTEMA') AS operador, m.creado_en
                FROM movimientos_billetera m
                LEFT JOIN usuarios u ON u.id = m.operador_id
                WHERE m.billetera_id = ?
                ORDER BY m.creado_en DESC, m.id DESC
                """, (rs, rowNum) -> new MovimientoBilleteraResponse(
                rs.getLong("id"), rs.getLong("billetera_id"), rs.getString("tipo"),
                rs.getBigDecimal("monto"), rs.getString("motivo"),
                rs.getString("operador"),
                rs.getObject("creado_en", java.time.OffsetDateTime.class)), walletId);
    }

    public Billetera createForUser(long usuarioId) {
        return jdbc.queryForObject("""
                INSERT INTO billeteras (usuario_id)
                VALUES (?)
                RETURNING id, usuario_id, saldo, activa
                """, mapper, usuarioId);
    }

    public Optional<Long> findUserId(String username) {
        return jdbc.query("""
                SELECT id FROM usuarios WHERE username = ? AND activo = TRUE
                """, rs -> rs.next() ? Optional.of(rs.getLong(1)) : Optional.empty(), username);
    }

    public boolean userExists(long usuarioId) {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM usuarios WHERE id = ? AND activo = TRUE)",
                Boolean.class, usuarioId);
        return Boolean.TRUE.equals(exists);
    }

    public boolean hasActiveTrip(long walletId) {
        Boolean active = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM viajes j
                    JOIN billeteras b ON b.usuario_id = j.usuario_id
                    WHERE b.id = ? AND j.finalizado_en IS NULL
                )
                """, Boolean.class, walletId);
        return Boolean.TRUE.equals(active);
    }

    public int adjustBalance(long walletId, BigDecimal signedAmount,
                             TipoMovimientoBilletera tipo, long operadorId, String motivo) {
        int updated = jdbc.update("""
                UPDATE billeteras
                SET saldo = saldo + ?
                WHERE id = ? AND activa = TRUE
                  AND (? > 0 OR saldo + ? >= 0)
                """, signedAmount, walletId, signedAmount, signedAmount);
        if (updated == 1) {
            jdbc.update("""
                    INSERT INTO movimientos_billetera
                        (billetera_id, operador_id, tipo, monto, motivo)
                    VALUES (?, ?, ?, ?, ?)
                    """, walletId, operadorId, tipo.name(), signedAmount, motivo);
        }
        return updated;
    }

    public int deactivate(long id) {
        return jdbc.update("""
                UPDATE billeteras
                SET activa = FALSE
                WHERE id = ? AND activa = TRUE AND saldo = 0
                """, id);
    }
}
