package com.labtbd.controller;

import com.labtbd.dto.ViajeFinalizarRequest;
import com.labtbd.dto.ViajeIniciarRequest;
import com.labtbd.dto.ViajeResponse;
import com.labtbd.service.ViajeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/viajes")
public class ViajeController {
    private final ViajeService service;

    public ViajeController(ViajeService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public List<ViajeResponse> findMine(Authentication authentication) {
        return service.findMine(authentication.getName()).stream()
                .map(ViajeResponse::desde).toList();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public List<ViajeResponse> findAll() {
        return service.findAll().stream().map(ViajeResponse::desde).toList();
    }

    @GetMapping("/{id}")
    public ViajeResponse findById(
            @PathVariable long id,
            Authentication authentication
    ) {
        boolean staff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERADOR")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        return ViajeResponse.desde(service.findById(id, authentication.getName(), staff));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ViajeResponse start(
            @Valid @RequestBody ViajeIniciarRequest request,
            Authentication authentication
    ) {
        return ViajeResponse.desde(service.iniciar(request, authentication.getName()));
    }

    @PostMapping("/{id}/finalizar")
    public ViajeResponse finish(
            @PathVariable long id,
            @Valid @RequestBody ViajeFinalizarRequest request,
            Authentication authentication
    ) {
        return ViajeResponse.desde(
                service.finalizar(id, request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ViajeResponse update(
            @PathVariable long id,
            @Valid @RequestBody ViajeFinalizarRequest request,
            Authentication authentication
    ) {
        return ViajeResponse.desde(
                service.finalizar(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable long id, Authentication authentication) {
        service.cancelar(id, authentication.getName());
    }
}
