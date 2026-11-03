package com.labtbd.controller;

import com.labtbd.dto.AuditoriaResponse;
import com.labtbd.entity.AuditoriaEstadoVehiculo;
import com.labtbd.service.AuditoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping({
        "/api/v1/vehiculos/{id}/auditoria",
        "/api/v1/auditorias"
})
public class AuditoriaController {
    private final AuditoriaService service;
    public AuditoriaController(AuditoriaService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public List<AuditoriaResponse> find(
            @PathVariable(name = "id", required = false) Long vehiculoId) {
        List<AuditoriaEstadoVehiculo> records = vehiculoId == null
                ? service.findAll() : service.findByVehiculoId(vehiculoId);
        return records.stream().map(AuditoriaResponse::desde).toList();
    }

    @GetMapping("/{auditoriaId}")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public AuditoriaResponse findById(@PathVariable long auditoriaId) {
        return AuditoriaResponse.desde(service.findById(auditoriaId));
    }
}