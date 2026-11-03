package com.labtbd.service;

import com.labtbd.dto.ViajeFinalizarRequest;
import com.labtbd.dto.ViajeIniciarRequest;
import com.labtbd.entity.Viaje;
import com.labtbd.repository.ViajeRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ViajeService {
    private final ViajeRepository repository;
    private final UsuarioService usuarios;

    public ViajeService(ViajeRepository repository, UsuarioService usuarios) {
        this.repository = repository;
        this.usuarios = usuarios;
    }

    public List<Viaje> findAll() {
        return repository.findAll();
    }

    public List<Viaje> findMine(String username) {
        long usuarioId = usuarios.findByUsername(username).id();
        return repository.findByUsuarioId(usuarioId);
    }

    public Viaje findById(long id, String username, boolean staff) {
        if (staff) {
            return repository.findById(id).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
        }
        long usuarioId = usuarios.findByUsername(username).id();
        return repository.findByIdAndUsuarioId(id, usuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
    }

    @Transactional
    public Viaje iniciar(ViajeIniciarRequest request, String username) {
        long usuarioId = usuarios.findByUsername(username).id();
        return repository.iniciar(usuarioId, request.vehiculoId());
    }

    @Transactional
    public Viaje finalizar(long id, ViajeFinalizarRequest request, String username) {
        Viaje viaje = findById(id, username, false);
        if (viaje.finalizadoEn() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El viaje ya está finalizado");
        }
        repository.finalizar(id, request.estacionDestinoId());
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
    }

    @Transactional
    public void cancelar(long id, String username) {
        Viaje viaje = findById(id, username, false);
        if (viaje.finalizadoEn() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede cancelar un viaje activo");
        }
        repository.cancelar(id, viaje.usuarioId());
    }
}
