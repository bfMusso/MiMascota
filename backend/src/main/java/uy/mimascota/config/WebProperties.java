package uy.mimascota.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Dirección pública de la web (para armar la URL de cada placa). */
@ConfigurationProperties(prefix = "mimascota.web")
public record WebProperties(String url) {

    public WebProperties {
        url = url == null || url.isBlank() ? "http://localhost:5180" : url.replaceAll("/+$", "");
    }
}
