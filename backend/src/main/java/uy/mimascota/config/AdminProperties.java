package uy.mimascota.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credenciales del primer administrador, tomadas de ADMIN_EMAIL y ADMIN_PASSWORD. */
@ConfigurationProperties(prefix = "mimascota.admin")
public record AdminProperties(String email, String password) {
}
