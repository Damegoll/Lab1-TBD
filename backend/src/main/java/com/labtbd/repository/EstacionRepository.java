package com.labtbd.repository;

import com.labtbd.entity.Estacion;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class EstacionRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Estacion> mapper = (rs, rowNum) -> new Estacion(
            rs.getLong("id"), rs.getString("nombre"), rs.getBoolean("activa"),
            rs.getInt("capacidad"), rs.getInt("objetivo_bicicletas"),
            rs.getInt("objetivo_scooters"));

    public EstacionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Estacion> findAll() {
        return jdbc.query("""
                SELECT id, nombre, activa, capacidad, objetivo_bicicletas, objetivo_scooters
                FROM estaciones ORDER BY id
                """, mapper);
    }

    public Optional<Estacion> findById(long id) {
        return jdbc.query("""
                SELECT id, nombre, activa, capacidad, objetivo_bicicletas, objetivo_scooters
                FROM estaciones WHERE id = ?
                """, mapper, id).stream().findFirst();
    }

    public Estacion create(EstacionRequestData d) {
        return jdbc.queryForObject("""
                INSERT INTO estaciones
                    (nombre, capacidad, objetivo_bicicletas, objetivo_scooters)
                VALUES (?, ?, ?, ?)
                RETURNING id, nombre, activa, capacidad, objetivo_bicicletas, objetivo_scooters
                """, mapper, d.nombre(), d.capacidad(), d.objetivoBicicletas(),
                d.objetivoScooters());
    }

    public int update(long id, EstacionRequestData d) {
        return jdbc.update("""
                UPDATE estaciones
                SET nombre = ?, capacidad = ?, objetivo_bicicletas = ?, objetivo_scooters = ?
                WHERE id = ? AND activa = TRUE
                  AND ? >= (SELECT COUNT(*) FROM vehiculos v WHERE v.estacion_id = estaciones.id)
                """, d.nombre(), d.capacidad(), d.objetivoBicicletas(),
                d.objetivoScooters(), id, d.capacidad());
    }

    public boolean hasVehicleOrTripReferences(long id) {
        jdbc.queryForList("SELECT id FROM estaciones WHERE id = ? FOR UPDATE", Long.class, id);
        Boolean referenced = jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM vehiculos WHERE estacion_id = ?)
                    OR EXISTS (SELECT 1 FROM viajes
                               WHERE finalizado_en IS NULL
                                 AND (estacion_origen_id = ? OR estacion_destino_id = ?))
                """, Boolean.class, id, id, id);
        return Boolean.TRUE.equals(referenced);
    }

    public int deactivate(long id) {
        return jdbc.update("""
                UPDATE estaciones SET activa = FALSE
                WHERE id = ? AND activa = TRUE
                """, id);
    }

    public record EstacionRequestData(
            String nombre, int capacidad, int objetivoBicicletas, int objetivoScooters) {}
}
