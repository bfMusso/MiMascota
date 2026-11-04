package uy.mimascota.escaneos;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uy.mimascota.escaneos.EscaneoService.EscaneoDto;
import uy.mimascota.escaneos.EscaneoService.Pagina;

/** Historial de escaneos de una mascota, con la ubicación exacta (solo para sus responsables). */
@RestController
public class EscaneosController {

    private final EscaneoService service;

    public EscaneosController(EscaneoService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/mascotas/{id}/escaneos")
    public Pagina<EscaneoDto> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.deMascota(UUID.fromString(jwt.getSubject()), id, page, size);
    }
}
