package uy.mimascota.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uy.mimascota.auth.AuthDtos.EmailRequest;
import uy.mimascota.auth.AuthDtos.LoginRequest;
import uy.mimascota.auth.AuthDtos.RegistroRequest;
import uy.mimascota.auth.AuthDtos.RegistroResponse;
import uy.mimascota.auth.AuthDtos.ResetPasswordRequest;
import uy.mimascota.auth.AuthDtos.SesionResponse;
import uy.mimascota.auth.AuthDtos.VerificarEmailRequest;
import uy.mimascota.config.AuthProperties;
import uy.mimascota.usuarios.UsuarioDto;

/**
 * Registro, verificación de email, login y sesión.
 * El access token va en el body; el refresh token, en una cookie httpOnly limitada a /api/v1/auth.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String COOKIE_REFRESH = "mm_refresh";
    private static final String PATH_COOKIE = "/api/v1/auth";

    private final AuthService auth;
    private final TokenService tokens;
    private final AuthProperties props;

    public AuthController(AuthService auth, TokenService tokens, AuthProperties props) {
        this.auth = auth;
        this.tokens = tokens;
        this.props = props;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registro(@Valid @RequestBody RegistroRequest req) {
        return new RegistroResponse(auth.registrar(req).getEmail());
    }

    @PostMapping("/verificar-email")
    public SesionResponse verificarEmail(@Valid @RequestBody VerificarEmailRequest req, HttpServletResponse res) {
        return sesion(auth.verificarEmail(req.email(), req.codigo()), res);
    }

    @PostMapping("/reenviar-verificacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarVerificacion(@Valid @RequestBody EmailRequest req) {
        auth.reenviarVerificacion(req.email());
    }

    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest req, HttpServletResponse res) {
        return sesion(auth.login(req.email(), req.password()), res);
    }

    @PostMapping("/refresh")
    public SesionResponse refresh(@CookieValue(name = COOKIE_REFRESH, required = false) String refresh,
                                  HttpServletResponse res) {
        return sesion(tokens.rotar(refresh), res);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name = COOKIE_REFRESH, required = false) String refresh, HttpServletResponse res) {
        tokens.revocar(refresh);
        escribirCookie(res, "", Duration.ZERO);
    }

    @PostMapping("/olvide-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void olvidePassword(@Valid @RequestBody EmailRequest req) {
        auth.olvidePassword(req.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        auth.resetPassword(req.email(), req.codigo(), req.password());
    }

    private SesionResponse sesion(TokenService.Tokens t, HttpServletResponse res) {
        escribirCookie(res, t.refreshToken(), props.refreshTtl());
        return new SesionResponse(t.accessToken(), "Bearer", t.expiraEnSegundos(), UsuarioDto.de(t.usuario()));
    }

    private void escribirCookie(HttpServletResponse res, String valor, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_REFRESH, valor)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Strict")
                .path(PATH_COOKIE)
                .maxAge(maxAge)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
