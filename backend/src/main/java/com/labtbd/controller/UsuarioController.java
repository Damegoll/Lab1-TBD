package com.labtbd.controller;

import com.labtbd.dto.ResumenMensualResponse;
import com.labtbd.dto.UsuarioCreateRequest;
import com.labtbd.dto.UsuarioResponse;
import com.labtbd.dto.UsuarioUpdateRequest;
import com.labtbd.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public UsuarioResponse me(Authentication authentication) {
        return UsuarioResponse.desde(
                service.findByUsername(authentication.getName())
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResponse> findAll() {
        return service.findAll().stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse findById(@PathVariable long id) {
        return UsuarioResponse.desde(service.findById(id));
    }

    @GetMapping("/{id}/resumen-mensual")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ResumenMensualResponse> obtenerResumenMensual(@PathVariable long id) {
        return service.obtenerResumenMensual(id);
    }

    @GetMapping("/me/resumen-mensual")
    public List<ResumenMensualResponse> obtenerMiResumenMensual(Authentication authentication) {
        long usuarioId = service.findByUsername(authentication.getName()).id();
        return service.obtenerResumenMensual(usuarioId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse create(
            @Valid @RequestBody UsuarioCreateRequest request
    ) {
        return UsuarioResponse.desde(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @PathVariable long id,
            @Valid @RequestBody UsuarioUpdateRequest request
    ) {
        service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable long id) {
        service.deactivate(id);
    }
}