package com.labtbd.dto;

import com.labtbd.entity.Rol;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        Rol rol
) {}
