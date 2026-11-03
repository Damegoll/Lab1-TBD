package com.labtbd.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.labtbd.dto.TarifaResponse;
import com.labtbd.dto.TarifaRequest;
import com.labtbd.service.TarifaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tarifas")
public class TarifaController {
    private final TarifaService service;
    public TarifaController(TarifaService service) {this.service = service;}

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public List<TarifaResponse> findAll() {
        return service.findAll().stream().map(TarifaResponse::desde).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public TarifaResponse findById(@PathVariable long id) {
        return TarifaResponse.desde(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public TarifaResponse create(@Valid @RequestBody TarifaRequest r){
        return TarifaResponse.desde(service.create(r));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public TarifaResponse update(@PathVariable long id, @Valid @RequestBody TarifaRequest r) {
        return TarifaResponse.desde(service.update(id, r));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable long id){
        service.deactivate(id);
    }
}
