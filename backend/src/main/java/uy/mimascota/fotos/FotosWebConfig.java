package uy.mimascota.fotos;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve /fotos/** desde la carpeta de fotos. En el servidor las sirve Nginx directo,
 * pero así también funcionan en modo desarrollo.
 */
@Configuration
public class FotosWebConfig implements WebMvcConfigurer {

    private final FotoService fotos;

    public FotosWebConfig(FotoService fotos) {
        this.fotos = fotos;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/fotos/**").addResourceLocations(fotos.carpeta().toUri().toString() + "/");
    }
}
