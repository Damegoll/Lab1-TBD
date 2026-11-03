package com.labtbd.repository;

import com.labtbd.entity.Vehiculo;
import com.labtbd.entity.TipoVehiculo;
import com.labtbd.entity.EstadoVehiculo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class VehiculoRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<Vehiculo> mapper = (rs, n) -> new Vehiculo(
            rs.getLong("id"), rs.getString("codigo"),
            TipoVehiculo.valueOf(rs.getString("tipo")),
            EstadoVehiculo.valueOf(rs.getString("estado")),
            rs.getInt("bateria"), rs.getObject("estacion_id", Long.class));

    public VehiculoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Vehiculo> findAll() {
        return jdbc.query("""
                SELECT id, codigo, tipo, estado, bateria, estacion_id
                FROM vehiculos ORDER BY id
                """, mapper);
    }

    public Optional<Vehiculo> findById(long id) {
        return jdbc.query("""
                SELECT id, codigo, tipo, estado, bateria, estacion_id
                FROM vehiculos WHERE id = ?
                """, mapper, id).stream().findFirst();
    }

    public Vehiculo create(VehiculoCreateRequestData d) {
        return jdbc.queryForObject("""
                INSERT INTO vehiculos (codigo, tipo, estado, bateria, estacion_id)
                VALUES (?, ?, 'DISPONIBLE', ?, ?)
                RETURNING id, codigo, tipo, estado, bateria, estacion_id
                """, mapper, d.codigo(), d.tipo().name(), d.bateria(), d.estacionId());
    }

    public int update(
            long id, String codigo, int bateria, TipoVehiculo tipo, Long estacionId) {
        return jdbc.update("""
                UPDATE vehiculos
                SET codigo = ?, bateria = ?, tipo = COALESCE(?, tipo),
                    estacion_id = COALESCE(?, estacion_id)
                WHERE id = ? AND estado <> 'DADO_DE_BAJA'
                """, codigo, bateria, tipo == null ? null : tipo.name(), estacionId, id);
    }

    public boolean hasActiveTrip(long vehicleId) {
        Boolean active = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM viajes WHERE vehiculo_id = ? AND finalizado_en IS NULL
                )
                """, Boolean.class, vehicleId);
        return Boolean.TRUE.equals(active);
    }

    public boolean hasTripHistory(long vehicleId) {
        Boolean hasTrips = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM viajes WHERE vehiculo_id = ?)",
                Boolean.class, vehicleId);
        return Boolean.TRUE.equals(hasTrips);
    }

    public int changeState(long id, EstadoVehiculo estado,
                           String operador, String motivo) {
        Long operadorId = jdbc.queryForObject(
                "SELECT id FROM usuarios WHERE username = ? AND activo = TRUE",
                Long.class, operador);
        if (operadorId == null) {
            throw new IllegalStateException("El operador autenticado no existe o está inactivo");
        }
        jdbc.queryForObject("SELECT set_config('app.operador', ?, true)",
                String.class, operadorId.toString());
        jdbc.queryForObject("SELECT set_config('app.motivo', ?, true)",
                String.class, motivo);
        return jdbc.update("UPDATE vehiculos SET estado = ? WHERE id = ?",
                estado.name(), id);
    }

    public boolean existsByCode(String codigo) {
        Boolean result = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM vehiculos WHERE codigo = ?)",
                Boolean.class, codigo);
        return Boolean.TRUE.equals(result);
    }

    public record VehiculoCreateRequestData(
            String codigo, TipoVehiculo tipo, int bateria, Long estacionId) {}
}