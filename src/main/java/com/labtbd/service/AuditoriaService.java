package com.labtbd.service;

import com.labtbd.entity.AuditoriaEstadoVehiculo;
import com.labtbd.dto.AuditoriaCreateRequest;
import com.labtbd.dto.AuditoriaUpdateRequest;
import com.labtbd.repository.AuditoriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import com.labtbd.entity.EstadoVehiculo;

@Service
public class AuditoriaService {
    private final AuditoriaRepository repo;
    private final VehiculoService vehiculos;

    public AuditoriaService(AuditoriaRepository repo, VehiculoService vehiculos) {
        this.repo = repo;
        this.vehiculos = vehiculos;
    }

    public List<AuditoriaEstadoVehiculo> findAll() {
        return repo.findAll();
    }

    public AuditoriaEstadoVehiculo findById(long auditId) {
        return repo.findById(auditId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Auditoría no encontrada"));
    }

    public AuditoriaEstadoVehiculo create(AuditoriaCreateRequest request) {
        validateRequest(request.estadoAnterior(), request.estadoNuevo());
        vehiculos.findById(request.vehiculoId());
        return repo.create(request.vehiculoId(), request.estadoAnterior(),
                request.estadoNuevo(), request.operadorId(), request.fecha(),
                request.motivo().trim());
    }

    public void update(long id, AuditoriaUpdateRequest request) {
        validateRequest(request.estadoAnterior(), request.estadoNuevo());
        vehiculos.findById(request.vehiculoId());
        if (repo.update(id, request.vehiculoId(), request.estadoAnterior(),
                request.estadoNuevo(), request.operadorId(), request.fecha(),
                request.motivo().trim()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auditoría no encontrada");
        }
    }

    public void delete(long id) {
        if (repo.delete(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Auditoría no encontrada");
        }
    }

    private void validateRequest(EstadoVehiculo previous, EstadoVehiculo next) {
        if (next != EstadoVehiculo.EN_MANTENCION && next != EstadoVehiculo.DADO_DE_BAJA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La auditoría solo admite cambios a mantención o baja");
        }
        if (next == previous) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estado anterior y nuevo deben ser distintos");
        }
    }

    public List<AuditoriaEstadoVehiculo> findByVehiculoId(long vehiculoId) {
        vehiculos.findById(vehiculoId);
        return repo.findByVehiculoId(vehiculoId);
    }
}