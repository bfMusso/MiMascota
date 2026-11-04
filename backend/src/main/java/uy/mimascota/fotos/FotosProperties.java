package uy.mimascota.fotos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Carpeta donde se guardan las fotos de las mascotas. */
@ConfigurationProperties(prefix = "mimascota.fotos")
public record FotosProperties(String carpeta) {

    public FotosProperties {
        if (carpeta == null || carpeta.isBlank()) {
            carpeta = "./data/fotos";
        }
    }
}
