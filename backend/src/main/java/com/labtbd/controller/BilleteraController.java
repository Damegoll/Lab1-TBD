package com.labtbd.controller;

import com.labtbd.dto.BilleteraAjusteRequest;
import com.labtbd.dto.BilleteraCreateRequest;
import com.labtbd.dto.BilleteraUpdateRequest;
import com.labtbd.dto.BilleteraResponse;
import com.labtbd.dto.MovimientoBilleteraResponse;
import com.labtbd.entity.TipoMovimientoBilletera;
import com.labtbd.service.BilleteraService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billeteras")
public class BilleteraController {
    private final BilleteraService service;

    public BilleteraController(BilleteraService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public BilleteraResponse mine(Authentication authentication) {
        return BilleteraResponse.desde(service.findByUsername(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public List<BilleteraResponse> findAll() {
        return service.findAll().stream().map(BilleteraResponse::desde).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public BilleteraResponse findById(@PathVariable long id) {
        return BilleteraResponse.desde(service.findById(id));
    }

    @GetMapping("/{id}/movimientos")
    public List<MovimientoBilleteraResponse> findMovimientos(
            @PathVariable long id, Authentication authentication) {
        boolean staff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERADOR")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        return service.findMovimientos(id, authentication.getName(), staff);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BilleteraResponse create(@Valid @RequestBody BilleteraCreateRequest request) {
        return BilleteraResponse.desde(service.create(request.usuarioId()));
    }

    @PatchMapping("/{id}/movimientos/{tipo}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public BilleteraResponse adjust(
            @PathVariable long id,
            @PathVariable TipoMovimientoBilletera tipo,
            @Valid @RequestBody BilleteraAjusteRequest request,
            Authentication authentication
    ) {
        return BilleteraResponse.desde(
                service.adjust(id, request, tipo, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERADOR', 'ADMIN')")
    public BilleteraResponse update(
            @PathVariable long id,
            @Valid @RequestBody BilleteraUpdateRequest request,
            Authentication authentication
    ) {
        return BilleteraResponse.desde(
                service.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable long id) {
        service.deactivate(id);
    }
}
