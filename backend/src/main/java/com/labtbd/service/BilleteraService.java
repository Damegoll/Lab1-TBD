package com.labtbd.service;

import com.labtbd.dto.BilleteraAjusteRequest;
import com.labtbd.dto.BilleteraUpdateRequest;
import com.labtbd.dto.MovimientoBilleteraResponse;
import com.labtbd.entity.Billetera;
import com.labtbd.entity.TipoMovimientoBilletera;
import com.labtbd.repository.BilleteraRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BilleteraService {
    private final BilleteraRepository repository;

    public BilleteraService(BilleteraRepository repository) {
        this.repository = repository;
    }

    public List<Billetera> findAll() {
        return repository.findAll();
    }

    public Billetera findById(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Billetera no encontrada"));
    }

    public Billetera findByUsername(String username) {
        long usuarioId = repository.findUserId(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return repository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Billetera no encontrada"));
    }

    public List<MovimientoBilleteraResponse> findMovimientos(
            long walletId, String username, boolean staff) {
        Billetera wallet = findById(walletId);
        if (!staff) {
            long usuarioId = repository.findUserId(username).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
            if (wallet.usuarioId() != usuarioId) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Billetera no encontrada");
            }
        }
        return repository.findMovimientos(walletId);
    }

    @Transactional
    public Billetera create(long usuarioId) {
        if (!repository.userExists(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        var existing = repository.findByUsuarioId(usuarioId);
        if (existing.isPresent()) {
            if (!existing.get().activa()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El usuario ya tiene una billetera inactiva");
            }
            return existing.get();
        }
        return repository.createForUser(usuarioId);
    }

    @Transactional
    public Billetera adjust(long id, BilleteraAjusteRequest request,
                            TipoMovimientoBilletera tipo, String username) {
        Billetera wallet = findById(id);
        long operadorId = repository.findUserId(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operador no encontrado"));
        BigDecimal signedAmount = tipo == TipoMovimientoBilletera.ABONO
                ? request.monto() : request.monto().negate();

        if (repository.adjustBalance(id, signedAmount, tipo, operadorId, request.motivo().trim()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    wallet.activa()
                            ? "El cargo dejaría saldo negativo"
                            : "La billetera está inactiva");
        }
        return findById(id);
    }

    @Transactional
    public Billetera update(long id, BilleteraUpdateRequest request, String username) {
        Billetera wallet = repository.lockById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Billetera no encontrada"));
        if (!wallet.activa()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede modificar una billetera inactiva");
        }

        BigDecimal difference = request.saldo().subtract(wallet.saldo());
        if (difference.signum() == 0) {
            return wallet;
        }

        long operatorId = repository.findUserId(username).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Operador no encontrado"));
        TipoMovimientoBilletera movementType = difference.signum() > 0
                ? TipoMovimientoBilletera.ABONO : TipoMovimientoBilletera.CARGO;
        if (repository.adjustBalance(
                id, difference, movementType, operatorId, request.motivo().trim()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El saldo solicitado no es válido para la billetera");
        }
        return findById(id);
    }

    @Transactional
    public void deactivate(long id) {
        repository.lockById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Billetera no encontrada"));
        Billetera wallet = findById(id);
        if (repository.hasActiveTrip(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede desactivar una billetera con un viaje activo");
        }
        if (wallet.saldo().compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El saldo debe ser cero antes de desactivar la billetera");
        }
        if (repository.deactivate(id) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La billetera ya está inactiva");
        }
    }
}
