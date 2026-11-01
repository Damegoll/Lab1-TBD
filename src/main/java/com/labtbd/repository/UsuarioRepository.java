package com.labtbd.repository;

import com.labtbd.entity.Rol;
import com.labtbd.entity.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UsuarioRepository {

    private final JdbcTemplate jdbc;

    private final RowMapper<Usuario> mapper = (rs, rowNum) -> new Usuario(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("nombre"),
            rs.getString("email"),
            rs.getString("password_hash"),
            Rol.valueOf(rs.getString("rol")),
            rs.getBoolean("activo")
    );

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Usuario> findAll() {
        return jdbc.query("""
                SELECT id, username, nombre, email, password_hash, rol, activo
                FROM usuarios
                ORDER BY id
                """, mapper);
    }

    public Optional<Usuario> findById(long id) {
        return jdbc.query("""
                SELECT id, username, nombre, email, password_hash, rol, activo
                FROM usuarios
                WHERE id = ?
                """, mapper, id).stream().findFirst();
    }

    public Optional<Usuario> findByUsername(String username) {
        return jdbc.query("""
                SELECT id, username, nombre, email, password_hash, rol, activo
                FROM usuarios
                WHERE username = ?
                """, mapper, username).stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM usuarios WHERE username = ?
                )
                """, Boolean.class, username);

        return Boolean.TRUE.equals(exists);
    }

    public Usuario create(
            String username,
            String nombre,
            String email,
            String passwordHash,
            Rol rol
    ) {
        return jdbc.queryForObject("""
                INSERT INTO usuarios (username, nombre, email, password_hash, rol)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id, username, nombre, email, password_hash, rol, activo
                """,
                mapper,
                username,
                nombre,
                email,
                passwordHash,
                rol.name()
        );
    }

    public int update(long id, String nombre, String email, Rol rol, String passwordHash) {
        return jdbc.update("""
                UPDATE usuarios
                SET nombre = ?, email = ?, rol = ?,
                    password_hash = COALESCE(?, password_hash)
                WHERE id = ? AND activo = TRUE
                """, nombre, email, rol.name(), passwordHash, id);
    }

    public int deactivate(long id) {
        return jdbc.update("""
                UPDATE usuarios
                SET activo = FALSE
                WHERE id = ? AND activo = TRUE
                """, id);
    }
}