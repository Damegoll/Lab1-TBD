package com.labtbd.controller;

import com.labtbd.dto.AuditoriaCreateRequest;
import com.labtbd.dto.AuditoriaResponse;
import com.labtbd.dto.AuditoriaUpdateRequest;
import com.labtbd.service.AuditoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auditorias")
@PreAuthorize("hasRole('ADMIN')")
public class AuditoriaAdminController {
    private final AuditoriaService service;

    public AuditoriaAdminController(AuditoriaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuditoriaResponse create(@Valid @RequestBody AuditoriaCreateRequest request) {
        return AuditoriaResponse.desde(service.create(request));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable long id,
                       @Valid @RequestBody AuditoriaUpdateRequest request) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        service.delete(id);
    }
}
