package edu.eci.dosw.oficioya.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final long ttlMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.ttl-minutes}") long ttlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.ttlMinutes = ttlMinutes;
    }

    public String generar(String correo, String rol) {
        return generar(correo, rol, ttlMinutes);
    }

    /**
     * Generates a token with a custom time to live, in minutes.
     * A negative value produces an already expired token, which PT-09 uses.
     */
    public String generar(String correo, String rol, long ttl) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
                .claim("role", rol)
                .issuer(issuer)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(ttl, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)   // fija HS256 sin importar el largo de la clave
                .compact();
    }

    public Claims validar(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)      // lanza JwtException si la firma es inválida o venció
                .getPayload();
    }
}