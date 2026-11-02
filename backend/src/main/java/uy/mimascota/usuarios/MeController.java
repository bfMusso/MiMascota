package uy.mimascota.usuarios;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uy.mimascota.auth.TokenService;
import uy.mimascota.comun.ApiException;

/** Perfil del usuario logueado. */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;

    public MeController(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    public record ActualizarPerfilRequest(
            @Size(min = 1, max = 100) String nombre,
            @Size(max = 30) String telefono,
            Boolean mostrarContacto) {
    }

    public record CambiarPasswordRequest(
            @NotBlank String actual,
            @NotBlank @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String nueva) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public UsuarioDto ver(@AuthenticationPrincipal Jwt jwt) {
        return UsuarioDto.de(actual(jwt));
    }

    @PatchMapping
    @Transactional
    public UsuarioDto actualizar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ActualizarPerfilRequest req) {
        Usuario u = actual(jwt);
        u.actualizarPerfil(req.nombre(), req.telefono(), req.mostrarContacto());
        return UsuarioDto.de(u);
    }

    /** Cambia la contraseña y cierra las demás sesiones (sus refresh tokens dejan de servir). */
    @PostMapping("/cambiar-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void cambiarPassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CambiarPasswordRequest req) {
        Usuario u = actual(jwt);
        if (!passwordEncoder.matches(req.actual(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "password_incorrecta", "La contraseña actual no es correcta");
        }
        u.cambiarPassword(passwordEncoder.encode(req.nueva()));
        tokens.revocarTodos(u);
    }

    private Usuario actual(Jwt jwt) {
        return usuarios.findById(UUID.fromString(jwt.getSubject()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "sesion_invalida", "La sesión no es válida"));
    }
}
