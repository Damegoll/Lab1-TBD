package com.labtbd.repository;

import com.labtbd.entity.Viaje;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ViajeRepository {
    private static final String COLUMNS = """
            id, usuario_id, vehiculo_id, iniciado_en, finalizado_en, costo,
            estacion_origen_id, estacion_destino_id, tarifa_id,
            precio_por_minuto_aplicado, cancelado
            """;

    private final JdbcTemplate jdbc;
    private final RowMapper<Viaje> mapper = (rs, rowNum) -> new Viaje(
            rs.getLong("id"),
            rs.getLong("usuario_id"),
            rs.getLong("vehiculo_id"),
            rs.getObject("iniciado_en", java.time.OffsetDateTime.class),
            rs.getObject("finalizado_en", java.time.OffsetDateTime.class),
            rs.getBigDecimal("costo"),
            rs.getObject("estacion_origen_id", Long.class),
            rs.getObject("estacion_destino_id", Long.class),
            rs.getObject("tarifa_id", Long.class),
            rs.getBigDecimal("precio_por_minuto_aplicado"),
            rs.getBoolean("cancelado"));

    public ViajeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Viaje> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM viajes ORDER BY iniciado_en DESC", mapper);
    }

    public List<Viaje> findByUsuarioId(long usuarioId) {
        return jdbc.query("SELECT " + COLUMNS
                + " FROM viajes WHERE usuario_id = ? ORDER BY iniciado_en DESC",
                mapper, usuarioId);
    }

    public Optional<Viaje> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM viajes WHERE id = ?",
                mapper, id).stream().findFirst();
    }

    public Optional<Viaje> findByIdAndUsuarioId(long id, long usuarioId) {
        return jdbc.query("""
                SELECT id, usuario_id, vehiculo_id, iniciado_en, finalizado_en, costo,
                       estacion_origen_id, estacion_destino_id, tarifa_id,
                       precio_por_minuto_aplicado, cancelado
                FROM viajes WHERE id = ? AND usuario_id = ?
                """, mapper, id, usuarioId).stream().findFirst();
    }

    public Viaje iniciar(long usuarioId, long vehiculoId) {
        return jdbc.queryForObject("""
                INSERT INTO viajes (usuario_id, vehiculo_id)
                VALUES (?, ?)
                RETURNING id, usuario_id, vehiculo_id, iniciado_en, finalizado_en, costo,
                          estacion_origen_id, estacion_destino_id, tarifa_id,
                          precio_por_minuto_aplicado, cancelado
                """, mapper, usuarioId, vehiculoId);
    }

    public void finalizar(long viajeId, long estacionDestinoId) {
        jdbc.update("CALL finalizar_viaje(?, ?)", viajeId, estacionDestinoId);
    }

    public int cancelar(long viajeId, long usuarioId) {
        return jdbc.update("CALL cancelar_viaje(?, ?)", viajeId, usuarioId);
    }
}
