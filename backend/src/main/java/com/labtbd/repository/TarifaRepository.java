package com.labtbd.repository;

import com.labtbd.entity.Tarifa;
import com.labtbd.entity.TipoVehiculo;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class TarifaRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Tarifa> mapper = (rs, n) -> new Tarifa(
            rs.getLong("id"), TipoVehiculo.valueOf(rs.getString("tipo_vehiculo")),
            rs.getBigDecimal("precio_por_minuto"),
            rs.getObject("vigente_desde", java.time.OffsetDateTime.class),
            rs.getObject("vigente_hasta", java.time.OffsetDateTime.class),
            rs.getBoolean("activa"));

    public TarifaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public List<Tarifa> findAll() {
        return jdbc.query("SELECT * FROM tarifas ORDER BY vigente_desde DESC", mapper);
    }
    public Optional<Tarifa> findById(long id) {
        return jdbc.query("SELECT * FROM tarifas WHERE id = ?", mapper, id)
                .stream().findFirst();
    }

    public int update(long id, TarifaRequestData d) {
        return jdbc.update("""
                UPDATE tarifas
                SET tipo_vehiculo = ?, precio_por_minuto = ?,
                    vigente_desde = ?, vigente_hasta = ?
                WHERE id = ? AND activa = TRUE
                """, d.tipo().name(), d.precio(), d.desde(), d.hasta(), id);
    }

    public Tarifa create(TarifaRequestData d) {
        return jdbc.queryForObject("""
                INSERT INTO tarifas (tipo_vehiculo, precio_por_minuto,
                                     vigente_desde, vigente_hasta, activa)
                VALUES (?, ?, ?, ?, TRUE)
                RETURNING *
                """, mapper, d.tipo().name(), d.precio(), d.desde(), d.hasta());
    }
    public int deactivate(long id) {
        return jdbc.update("UPDATE tarifas SET activa = FALSE WHERE id = ?", id);
    }
    public record TarifaRequestData(TipoVehiculo tipo,
            java.math.BigDecimal precio, java.time.OffsetDateTime desde,
            java.time.OffsetDateTime hasta) {}
}