package com.labtbd.repository;

import com.labtbd.entity.AuditoriaEstadoVehiculo;
import com.labtbd.entity.EstadoVehiculo;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class AuditoriaRepository {
    private final JdbcTemplate jdbc;
    private static final String SELECT_COLUMNS = """
            SELECT a.id, a.vehiculo_id, a.estado_anterior, a.estado_nuevo,
                   COALESCE(u.username, 'SISTEMA') AS operador, a.fecha, a.motivo
            FROM auditoria_estado_vehiculo a
            LEFT JOIN usuarios u ON u.id = a.operador_id
            """;

    private final RowMapper<AuditoriaEstadoVehiculo> mapper = (rs, n) -> {
        String anterior = rs.getString("estado_anterior");
        return new AuditoriaEstadoVehiculo(
                rs.getLong("id"),
                rs.getLong("vehiculo_id"),
                anterior == null ? null : EstadoVehiculo.valueOf(anterior),
                EstadoVehiculo.valueOf(rs.getString("estado_nuevo")),
                rs.getString("operador"),
                rs.getObject("fecha", java.time.OffsetDateTime.class),
                rs.getString("motivo"));
    };

    public AuditoriaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<AuditoriaEstadoVehiculo> findAll() {
        return jdbc.query(SELECT_COLUMNS + " ORDER BY a.fecha DESC, a.id DESC", mapper);
    }

    public java.util.Optional<AuditoriaEstadoVehiculo> findById(long id) {
        return jdbc.query(SELECT_COLUMNS + " WHERE a.id = ?",
                mapper, id).stream().findFirst();
    }

    public AuditoriaEstadoVehiculo create(
            long vehiculoId,
            EstadoVehiculo estadoAnterior,
            EstadoVehiculo estadoNuevo,
            long operadorId,
            java.time.OffsetDateTime fecha,
            String motivo) {
        Long id = jdbc.queryForObject("""
                INSERT INTO auditoria_estado_vehiculo (
                    vehiculo_id, estado_anterior, estado_nuevo, operador_id, fecha, motivo
                )
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, vehiculoId,
                estadoAnterior == null ? null : estadoAnterior.name(),
                estadoNuevo.name(), operadorId, fecha, motivo);
        return findById(id).orElseThrow();
    }

    public int update(
            long id,
            long vehiculoId,
            EstadoVehiculo estadoAnterior,
            EstadoVehiculo estadoNuevo,
            long operadorId,
            java.time.OffsetDateTime fecha,
            String motivo) {
        return jdbc.update("""
                UPDATE auditoria_estado_vehiculo
                SET vehiculo_id = ?, estado_anterior = ?, estado_nuevo = ?,
                    operador_id = ?, fecha = ?, motivo = ?
                WHERE id = ?
                """, vehiculoId,
                estadoAnterior == null ? null : estadoAnterior.name(),
                estadoNuevo.name(), operadorId, fecha, motivo, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM auditoria_estado_vehiculo WHERE id = ?", id);
    }

    public List<AuditoriaEstadoVehiculo> findByVehiculoId(long vehiculoId) {
        return jdbc.query(SELECT_COLUMNS + " WHERE a.vehiculo_id = ? ORDER BY a.fecha DESC, a.id DESC",
                mapper, vehiculoId);
    }
}