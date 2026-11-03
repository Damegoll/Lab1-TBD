package com.labtbd.service;

import com.labtbd.dto.AuthResponse;
import com.labtbd.dto.LoginRequest;
import com.labtbd.dto.RegistroRequest;
import com.labtbd.entity.Rol;
import com.labtbd.entity.Usuario;
import com.labtbd.repository.UsuarioRepository;
import com.labtbd.security.JdbcUserDetailsService;
import com.labtbd.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcUserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JdbcUserDetailsService userDetailsService,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegistroRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El username ya está en uso");
        }

        Usuario usuario;
        try {
            usuario = usuarioRepository.create(
                    request.username(),
                    request.nombre(),
                    request.email(),
                    passwordEncoder.encode(request.password()),
                    Rol.USUARIO
            );
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El username o email ya está en uso");
        }
        return issueToken(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(request.username());
        } catch (UsernameNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        return issueToken(usuario);
    }

    private AuthResponse issueToken(Usuario usuario) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.username());
        return new AuthResponse(
                jwtService.generateToken(userDetails),
                "Bearer",
                usuario.username(),
                usuario.rol()
        );
    }
}
