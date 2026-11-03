package com.labtbd.entity;

public record Usuario(
    long id,
    String username,
    String nombre,
    String email,
    String passwordHash,
    Rol rol,
    boolean activo
) {}