package com.labtbd.service;

import com.labtbd.dto.CambiarEstadoRequest;
import com.labtbd.dto.EstacionDisponibilidadResponse;
import com.labtbd.dto.VehiculoCreateRequest;
import com.labtbd.dto.VehiculoUpdateRequest;
import com.labtbd.entity.EstadoVehiculo;
import com.labtbd.entity.Vehiculo;
import com.labtbd.repository.VehiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class VehiculoService {
    private final VehiculoRepository repo;
    private final JdbcTemplate jdbcTemplate;

    public VehiculoService(VehiculoRepository repo, JdbcTemplate jdbcTemplate) {
        this.repo = repo;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Vehiculo> findAll() {
        return repo.findAll();
    }

    public Vehiculo findById(long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
    }

    public Vehiculo create(VehiculoCreateRequest request) {
        if (repo.existsByCode(request.codigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El código del vehículo ya existe");
        }
        return repo.create(new VehiculoRepository.VehiculoCreateRequestData(
                request.codigo(), request.tipo(), request.bateria(), request.estacionId()));
    }

    public void update(long id, VehiculoUpdateRequest request) {
        Vehiculo current = findById(id);
        if (request.tipo() != null
                && request.tipo() != current.tipo()
                && repo.hasTripHistory(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede cambiar el tipo de un vehículo con viajes asociados");
        }
        if (request.estacionId() != null && repo.hasActiveTrip(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede reubicar un vehículo con un viaje activo");
        }
        if (repo.update(id, request.codigo(), request.bateria(),
                request.tipo(), request.estacionId()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Vehículo no encontrado o dado de baja");
        }
    }

    @Transactional
    public void changeState(long id, CambiarEstadoRequest request, String operador) {
        if (request.estado() != EstadoVehiculo.EN_MANTENCION
                && request.estado() != EstadoVehiculo.DADO_DE_BAJA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este endpoint solo permite enviar a mantención o dar de baja");
        }
        if (repo.changeState(id, request.estado(), operador, request.motivo()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Vehículo no encontrado");
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void refrescarVistaDemanda() {
        jdbcTemplate.execute(
                "REFRESH MATERIALIZED VIEW CONCURRENTLY mv_demanda_estacion_horaria");
    }

    public List<EstacionDisponibilidadResponse> obtenerEstacionesDisponibles() {
        return jdbcTemplate.query("""
                SELECT
                    e.id AS estacion_id,
                    e.nombre AS estacion_nombre,
                    v.tipo AS tipo_vehiculo,
                    COUNT(v.id) AS cantidad,
                    ROUND(AVG(v.bateria), 2)::double precision AS bateria_promedio
                FROM estaciones e
                JOIN vehiculos v ON e.id = v.estacion_id
                WHERE v.estado = 'DISPONIBLE' AND e.activa = TRUE
                GROUP BY e.id, e.nombre, v.tipo
                ORDER BY e.nombre, v.tipo
                """,
                (rs, rowNum) -> new EstacionDisponibilidadResponse(
                        rs.getLong("estacion_id"),
                        rs.getString("estacion_nombre"),
                        rs.getString("tipo_vehiculo"),
                        rs.getLong("cantidad"),
                        rs.getDouble("bateria_promedio")));
    }
}
