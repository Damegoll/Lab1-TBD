package com.labtbd.dto;

import com.labtbd.entity.Usuario;
import com.labtbd.entity.Rol;

public record UsuarioResponse(
        long id,
        String username,
        String nombre,
        String email,
        Rol rol,
        boolean activo
) {
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.id(),
                usuario.username(),
                usuario.nombre(),
                usuario.email(),
                usuario.rol(),
                usuario.activo()
        );
    }
}