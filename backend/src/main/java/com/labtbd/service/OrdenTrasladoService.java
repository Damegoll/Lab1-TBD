package com.labtbd.service;

import com.labtbd.dto.RebalanceoRequest;
import com.labtbd.dto.OrdenTrasladoUpdateRequest;
import com.labtbd.entity.OrdenTraslado;
import com.labtbd.repository.OrdenTrasladoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
public class OrdenTrasladoService {
    private final OrdenTrasladoRepository repo;
    public OrdenTrasladoService(OrdenTrasladoRepository repo) { this.repo = repo; }

    public List<OrdenTraslado> findAll() { return repo.findAll(); }

    public OrdenTraslado findById(long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Orden de traslado no encontrada"));
    }

    @Transactional
    public void updateCantidad(long id, OrdenTrasladoUpdateRequest request) {
        int updated = repo.updateCantidad(id, request.cantidad());
        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Orden de traslado no encontrada");
        }
        if (updated < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede ajustar la orden con el inventario disponible actualmente");
        }
    }

    @Transactional
    public void deleteAndReverse(long id) {
        int deleted = repo.deleteAndReverse(id);
        if (deleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Orden de traslado no encontrada");
        }
        if (deleted < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede revertir la orden: faltan vehículos disponibles o capacidad en origen");
        }
    }

    @Transactional
    public OrdenTraslado rebalancear(RebalanceoRequest r, String username) {
        if (r.estacionOrigenId().equals(r.estacionDestinoId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La estación de origen debe ser distinta de la de destino");
        }
        long operadorId = repo.findOperadorId(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operador no encontrado"));

        UUID solicitudId = UUID.randomUUID();
        repo.ejecutarRebalanceo(solicitudId, r.estacionOrigenId(), r.estacionDestinoId(),
                r.tipoVehiculo().name(), r.cantidad(), operadorId);

        return repo.findBySolicitudId(solicitudId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "La orden no fue creada"));
    }
}