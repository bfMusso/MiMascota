package uy.mimascota.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uy.mimascota.usuarios.UsuarioDto;

/** Cuerpos de request y response del módulo de autenticación. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegistroRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password,
            @NotBlank @Size(max = 100) String nombre,
            @Size(max = 30) String telefono) {
    }

    public record RegistroResponse(String email) {
    }

    public record VerificarEmailRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "debe tener 6 dígitos") String codigo) {
    }

    public record EmailRequest(@NotBlank @Email String email) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "debe tener 6 dígitos") String codigo,
            @NotBlank @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres") String password) {
    }

    public record SesionResponse(String accessToken, String tokenType, long expiraEn, UsuarioDto usuario) {
    }
}
