package com.labtbd.service;

import com.labtbd.dto.EstacionRequest;
import com.labtbd.entity.Estacion;
import com.labtbd.repository.EstacionRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EstacionService {
    private final EstacionRepository repository;

    public EstacionService(EstacionRepository repository) {
        this.repository = repository;
    }

    public List<Estacion> findAll() {
        return repository.findAll();
    }

    public Estacion findById(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Estación no encontrada"));
    }

    public Estacion create(EstacionRequest request) {
        validarObjetivos(request);
        return repository.create(data(request));
    }

    public void update(long id, EstacionRequest request) {
        validarObjetivos(request);
        Estacion estacion = findById(id);
        if (!estacion.activa()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Estación no encontrada o inactiva");
        }
        if (repository.update(id, data(request)) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La capacidad no admite el inventario actual");
        }
    }

    @Transactional
    public void deactivate(long id) {
        findById(id);
        if (repository.hasVehicleOrTripReferences(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede desactivar una estación con vehículos o viajes asociados");
        }
        if (repository.deactivate(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Estación no encontrada o inactiva");
        }
    }

    private void validarObjetivos(EstacionRequest request) {
        if ((long) request.objetivoBicicletas() + request.objetivoScooters() > request.capacidad()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La suma de los objetivos por tipo no puede superar la capacidad");
        }
    }

    private EstacionRepository.EstacionRequestData data(EstacionRequest request) {
        return new EstacionRepository.EstacionRequestData(request.nombre().trim(),
                request.capacidad(), request.objetivoBicicletas(), request.objetivoScooters());
    }
}
