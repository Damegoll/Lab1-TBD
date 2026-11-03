package com.labtbd.controller;

import com.labtbd.dto.EstacionDisponibilidadResponse;
import com.labtbd.dto.EstacionRequest;
import com.labtbd.entity.Estacion;
import com.labtbd.service.EstacionService;
import com.labtbd.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/estaciones")
public class EstacionController {

    private final VehiculoService vehiculoService;
    private final EstacionService estacionService;

    public EstacionController(VehiculoService vehiculoService, EstacionService estacionService) {
        this.vehiculoService = vehiculoService;
        this.estacionService = estacionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public List<Estacion> findAll() {
        return estacionService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public Estacion findById(@PathVariable long id) {
        return estacionService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Estacion create(@Valid @RequestBody EstacionRequest request) {
        return estacionService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public Estacion update(@PathVariable long id, @Valid @RequestBody EstacionRequest request) {
        estacionService.update(id, request);
        return estacionService.findById(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable long id) {
        estacionService.deactivate(id);
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('USUARIO', 'OPERADOR', 'ADMIN')")
    public List<EstacionDisponibilidadResponse> listarDisponibles() {
        return vehiculoService.obtenerEstacionesDisponibles();
    }
}