package com.labtbd.repository;

import com.labtbd.entity.OrdenTraslado;
import com.labtbd.entity.TipoVehiculo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrdenTrasladoRepository {
    private final JdbcTemplate jdbc;

    private final RowMapper<OrdenTraslado> mapper = (rs, n) -> new OrdenTraslado(
            rs.getLong("id"),
            rs.getObject("solicitud_id", UUID.class),
            rs.getLong("estacion_origen_id"),
            rs.getLong("estacion_destino_id"),
            TipoVehiculo.valueOf(rs.getString("tipo_vehiculo")),
            rs.getInt("cantidad"),
            rs.getLong("operador_id"),
            rs.getObject("creada_en", java.time.OffsetDateTime.class));

    private static final String COLUMNS = """
            id, solicitud_id, estacion_origen_id, estacion_destino_id,
            tipo_vehiculo, cantidad, operador_id, creada_en
            """;

    public OrdenTrasladoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<OrdenTraslado> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM ordenes_traslado ORDER BY id DESC", mapper);
    }

    public Optional<OrdenTraslado> findById(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ordenes_traslado WHERE id = ?",
                mapper, id).stream().findFirst();
    }

    public Optional<OrdenTraslado> findByIdForUpdate(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ordenes_traslado WHERE id = ? FOR UPDATE",
                mapper, id).stream().findFirst();
    }

    public Optional<OrdenTraslado> findBySolicitudId(UUID solicitudId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM ordenes_traslado WHERE solicitud_id = ?",
                mapper, solicitudId).stream().findFirst();
    }

    public void ejecutarRebalanceo(UUID solicitudId, long origen, long destino,
                                   String tipo, int cantidad, long operadorId) {
        jdbc.update("CALL ejecutar_rebalanceo(?, ?, ?, ?, ?, ?)",
                solicitudId, origen, destino, tipo, cantidad, operadorId);
    }

    public Optional<Long> findOperadorId(String username) {
        return jdbc.queryForList("SELECT id FROM usuarios WHERE username = ?",
                Long.class, username).stream().findFirst();
    }

    public int updateCantidad(long id, int cantidad) {
        OrdenTraslado orden = findByIdForUpdate(id).orElse(null);
        if (orden == null) {
            return 0;
        }
        if (orden.cantidad() == cantidad) {
            return 1;
        }

        lockStations(orden.estacionOrigenId(), orden.estacionDestinoId());
        int diferencia = Math.abs(cantidad - orden.cantidad());
        long origen = cantidad > orden.cantidad()
                ? orden.estacionOrigenId() : orden.estacionDestinoId();
        long destino = cantidad > orden.cantidad()
                ? orden.estacionDestinoId() : orden.estacionOrigenId();

        if (cantidad > orden.cantidad()
                ? !canTransfer(origen, destino, orden.tipoVehiculo(), diferencia)
                : !canReverseTransfer(origen, destino, orden.tipoVehiculo(), diferencia)) {
            return -1;
        }

        if (moveAvailableVehicles(origen, destino, orden.tipoVehiculo(), diferencia) != diferencia) {
            return -1;
        }
        return jdbc.update("UPDATE ordenes_traslado SET cantidad = ? WHERE id = ?", cantidad, id);
    }

    public int deleteAndReverse(long id) {
        OrdenTraslado orden = findByIdForUpdate(id).orElse(null);
        if (orden == null) {
            return 0;
        }

        lockStations(orden.estacionOrigenId(), orden.estacionDestinoId());
        if (!canReverseTransfer(orden.estacionDestinoId(), orden.estacionOrigenId(),
                orden.tipoVehiculo(), orden.cantidad())) {
            return -1;
        }
        if (moveAvailableVehicles(orden.estacionDestinoId(), orden.estacionOrigenId(),
                orden.tipoVehiculo(), orden.cantidad()) != orden.cantidad()) {
            return -1;
        }
        return jdbc.update("DELETE FROM ordenes_traslado WHERE id = ?", id);
    }

    private void lockStations(long origen, long destino) {
        jdbc.queryForList("""
                SELECT id FROM estaciones
                WHERE id IN (?, ?)
                ORDER BY id
                FOR UPDATE
                """, Long.class, origen, destino);
    }

    private boolean canTransfer(long origen, long destino, TipoVehiculo tipo, int cantidad) {
        int objetivoOrigen = stationObjective(origen, tipo);
        int objetivoDestino = stationObjective(destino, tipo);
        int disponiblesOrigen = countVehicles(origen, tipo, true);
        int disponiblesDestino = countVehicles(destino, tipo, true);
        int totalDestino = countVehicles(destino, null, false);
        int capacidadDestino = stationCapacity(destino);

        return disponiblesOrigen - objetivoOrigen >= cantidad
                && objetivoDestino - disponiblesDestino >= cantidad
                && capacidadDestino - totalDestino >= cantidad;
    }

    private boolean canReverseTransfer(long origen, long destino, TipoVehiculo tipo, int cantidad) {
        return countVehicles(origen, tipo, true) >= cantidad
                && stationCapacity(destino) - countVehicles(destino, null, false) >= cantidad;
    }

    private int stationObjective(long stationId, TipoVehiculo tipo) {
        String column = tipo == TipoVehiculo.BICICLETA
                ? "objetivo_bicicletas" : "objetivo_scooters";
        Integer objective = jdbc.queryForObject(
                "SELECT COALESCE((SELECT " + column
                        + " FROM estaciones WHERE id = ? AND activa = TRUE), -1)",
                Integer.class, stationId);
        return objective == null ? -1 : objective;
    }

    private int stationCapacity(long stationId) {
        Integer capacity = jdbc.queryForObject("""
                SELECT COALESCE(
                    (SELECT capacidad FROM estaciones WHERE id = ? AND activa = TRUE), 0)
                """, Integer.class, stationId);
        return capacity == null ? 0 : capacity;
    }

    private int countVehicles(long stationId, TipoVehiculo tipo, boolean availableOnly) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM vehiculos WHERE estacion_id = ?");
        if (tipo != null) {
            sql.append(" AND tipo = ?");
        }
        if (availableOnly) {
            sql.append(" AND estado = 'DISPONIBLE'");
        }
        Integer count = tipo == null
                ? jdbc.queryForObject(sql.toString(), Integer.class, stationId)
                : jdbc.queryForObject(sql.toString(), Integer.class, stationId, tipo.name());
        return count == null ? 0 : count;
    }

    private int moveAvailableVehicles(
            long origen, long destino, TipoVehiculo tipo, int cantidad) {
        return jdbc.update("""
                WITH seleccion AS (
                    SELECT id
                    FROM vehiculos
                    WHERE estacion_id = ? AND tipo = ? AND estado = 'DISPONIBLE'
                    ORDER BY id
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE vehiculos
                SET estacion_id = ?
                WHERE id IN (SELECT id FROM seleccion)
                """, origen, tipo.name(), cantidad, destino);
    }
}