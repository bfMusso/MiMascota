package uy.mimascota.placas;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uy.mimascota.placas.PlacaDtos.ActivarRequest;
import uy.mimascota.placas.PlacaDtos.AsignarRequest;
import uy.mimascota.placas.PlacaDtos.PlacaDto;

@RestController
@RequestMapping("/api/v1/placas")
public class PlacaController {

    private final PlacaService service;

    public PlacaController(PlacaService service) {
        this.service = service;
    }

    @PostMapping("/{codigo}/activar")
    public PlacaDto activar(@AuthenticationPrincipal Jwt jwt, @PathVariable String codigo,
                            @Valid @RequestBody ActivarRequest req) {
        return service.activar(usuario(jwt), codigo, req);
    }

    @GetMapping
    public List<PlacaDto> mias(@AuthenticationPrincipal Jwt jwt) {
        return service.mias(usuario(jwt));
    }

    @PutMapping("/{codigo}/mascota")
    public PlacaDto asignar(@AuthenticationPrincipal Jwt jwt, @PathVariable String codigo,
                            @RequestBody AsignarRequest req) {
        return service.asignar(usuario(jwt), codigo, req.mascotaId());
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
