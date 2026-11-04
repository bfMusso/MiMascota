package uy.mimascota.mascotas;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uy.mimascota.mascotas.MascotaDtos.MascotaDto;
import uy.mimascota.mascotas.MascotaDtos.MascotaRequest;

@RestController
@RequestMapping("/api/v1/mascotas")
public class MascotaController {

    private final MascotaService service;

    public MascotaController(MascotaService service) {
        this.service = service;
    }

    @GetMapping
    public List<MascotaDto> listar(@AuthenticationPrincipal Jwt jwt) {
        return service.listar(usuario(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MascotaDto crear(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MascotaRequest req) {
        UUID u = usuario(jwt);
        return service.ver(u, service.crear(u, req).getId());
    }

    @GetMapping("/{id}")
    public MascotaDto ver(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.ver(usuario(jwt), id);
    }

    @PatchMapping("/{id}")
    public MascotaDto actualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                 @Valid @RequestBody MascotaRequest req) {
        return service.actualizar(usuario(jwt), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.borrar(usuario(jwt), id);
    }

    @PutMapping(path = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MascotaDto subirFoto(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @RequestParam("foto") MultipartFile foto) {
        return service.cambiarFoto(usuario(jwt), id, foto);
    }

    @DeleteMapping("/{id}/foto")
    public MascotaDto quitarFoto(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.quitarFoto(usuario(jwt), id);
    }

    static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
