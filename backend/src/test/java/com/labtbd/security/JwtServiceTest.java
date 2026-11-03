package com.labtbd.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

    private static final String SECRET = "a-long-development-secret-for-unit-tests-123456";

    @Test
    void generatesAndValidatesTokenForSubject() {
        JwtService jwtService = new JwtService(SECRET, 60);
        UserDetails user = User.withUsername("alice")
                .password("not-used-here")
                .authorities("ROLE_USUARIO")
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtService issuer = new JwtService(SECRET, 60);
        JwtService verifier = new JwtService("a-different-development-secret-for-tests-123456", 60);
        UserDetails user = User.withUsername("alice")
                .password("not-used-here")
                .authorities("ROLE_USUARIO")
                .build();

        String token = issuer.generateToken(user);

        assertThatThrownBy(() -> verifier.extractUsername(token))
                .isInstanceOf(JwtException.class);
    }
}
