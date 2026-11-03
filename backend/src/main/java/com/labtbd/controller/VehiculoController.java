package com.labtbd.controller;

import com.labtbd.dto.VehiculoResponse;
import com.labtbd.dto.VehiculoCreateRequest;
import com.labtbd.dto.VehiculoUpdateRequest;
import com.labtbd.dto.CambiarEstadoRequest;
import com.labtbd.dto.VehiculoRetirarRequest;
import com.labtbd.service.VehiculoService;
import com.labtbd.entity.EstadoVehiculo;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {
    private final VehiculoService service;
    public VehiculoController(VehiculoService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public List<VehiculoResponse> findAll() {
        return service.findAll().stream().map(VehiculoResponse::desde).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public VehiculoResponse findById(@PathVariable long id) {
        return VehiculoResponse.desde(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculoResponse create(@Valid @RequestBody VehiculoCreateRequest r) {
        return VehiculoResponse.desde(service.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable long id,
                       @Valid @RequestBody VehiculoUpdateRequest r) {
        service.update(id, r);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeState(@PathVariable long id,
                            @Valid @RequestBody CambiarEstadoRequest r,
                            Authentication auth) {
        service.changeState(id, r, auth.getName());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retire(@PathVariable long id,
                       @Valid @RequestBody VehiculoRetirarRequest request,
                       Authentication auth) {
        service.changeState(id,
                new CambiarEstadoRequest(EstadoVehiculo.DADO_DE_BAJA, request.motivo()),
                auth.getName());
    }
}