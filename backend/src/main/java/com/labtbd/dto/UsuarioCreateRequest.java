package com.labtbd.dto;

import com.labtbd.entity.Rol;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
    @NotBlank 
    @Size(max=50)
    String username,

    @NotBlank 
    @Size(max=50)
    String nombre,

    @NotBlank
    @Email  
    @Size(max=254)
    String email,

    @NotBlank 
    @Size(min=12, max=72)
    String password,

    @NotNull
    Rol rol
) {}