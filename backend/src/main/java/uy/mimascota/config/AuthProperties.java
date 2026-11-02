package uy.mimascota.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de autenticación.
 *
 * @param jwtSecret    clave HMAC para firmar los access tokens (JWT_SECRET, al menos 32 caracteres)
 * @param accessTtl    duración del access token
 * @param refreshTtl   duración del refresh token
 * @param cookieSecure si la cookie del refresh lleva el atributo Secure
 */
@ConfigurationProperties(prefix = "mimascota.auth")
public record AuthProperties(String jwtSecret, Duration accessTtl, Duration refreshTtl, boolean cookieSecure) {

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET tiene que estar definido y tener al menos 32 caracteres");
        }
        if (accessTtl == null) {
            accessTtl = Duration.ofMinutes(15);
        }
        if (refreshTtl == null) {
            refreshTtl = Duration.ofDays(30);
        }
    }
}
