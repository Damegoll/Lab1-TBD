package com.labtbd.controller;

import com.labtbd.dto.OrdenTrasladoResponse;
import com.labtbd.dto.OrdenTrasladoUpdateRequest;
import com.labtbd.dto.RebalanceoRequest;
import com.labtbd.service.OrdenTrasladoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ordenes-traslado")
public class OrdenTrasladoController {
    private final OrdenTrasladoService service;
    public OrdenTrasladoController(OrdenTrasladoService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public List<OrdenTrasladoResponse> findAll() {
        return service.findAll().stream().map(OrdenTrasladoResponse::desde).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    public OrdenTrasladoResponse findById(@PathVariable long id) {
        return OrdenTrasladoResponse.desde(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenTrasladoResponse create(@Valid @RequestBody RebalanceoRequest r,
                                        Authentication auth) {
        return OrdenTrasladoResponse.desde(service.rebalancear(r, auth.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable long id,
                       @Valid @RequestBody OrdenTrasladoUpdateRequest request) {
        service.updateCantidad(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR','ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.deleteAndReverse(id);
    }
}