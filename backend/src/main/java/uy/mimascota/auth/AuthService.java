package uy.mimascota.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.mimascota.auth.AuthDtos.RegistroRequest;
import uy.mimascota.comun.ApiException;
import uy.mimascota.usuarios.Rol;
import uy.mimascota.usuarios.Usuario;
import uy.mimascota.usuarios.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final CodigoService codigos;
    private final TokenService tokens;
    private final EnviadorEmail email;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, CodigoService codigos,
                       TokenService tokens, EnviadorEmail email) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.codigos = codigos;
        this.tokens = tokens;
        this.email = email;
    }

    @Transactional
    public Usuario registrar(RegistroRequest req) {
        if (usuarios.findByEmail(req.email()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "email_registrado", "Ya existe una cuenta con ese email");
        }
        Usuario u = usuarios.save(new Usuario(req.email(), passwordEncoder.encode(req.password()), req.nombre(),
                req.telefono(), Rol.USER));
        enviarCodigo(u, TipoCodigo.VERIFICAR_EMAIL);
        return u;
    }

    @Transactional
    public TokenService.Tokens verificarEmail(String emailUsuario, String codigo) {
        Usuario u = usuarios.findByEmail(emailUsuario).orElseThrow(CodigoService::invalido);
        if (u.isEmailVerificado()) {
            throw new ApiException(HttpStatus.CONFLICT, "email_ya_verificado", "El email ya está verificado. Iniciá sesión.");
        }
        codigos.consumir(u.getId(), TipoCodigo.VERIFICAR_EMAIL, codigo);
        u.marcarEmailVerificado();
        return tokens.emitir(u);
    }

    /** No revela si el email existe: si no corresponde enviar, no hace nada. */
    @Transactional
    public void reenviarVerificacion(String emailUsuario) {
        usuarios.findByEmail(emailUsuario)
                .filter(u -> !u.isEmailVerificado())
                .ifPresent(u -> {
                    if (!codigos.puedeEnviar(u.getId(), TipoCodigo.VERIFICAR_EMAIL)) {
                        throw demasiadosEnvios();
                    }
                    enviarCodigo(u, TipoCodigo.VERIFICAR_EMAIL);
                });
    }

    @Transactional
    public TokenService.Tokens login(String emailUsuario, String password) {
        Usuario u = usuarios.findByEmail(emailUsuario)
                .filter(x -> passwordEncoder.matches(password, x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "credenciales_invalidas",
                        "Email o contraseña incorrectos"));
        if (!u.isEmailVerificado()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "email_no_verificado",
                    "Tenés que verificar tu email antes de iniciar sesión");
        }
        return tokens.emitir(u);
    }

    /** Siempre responde igual, exista o no el email, para no revelar qué cuentas existen. */
    @Transactional
    public void olvidePassword(String emailUsuario) {
        usuarios.findByEmail(emailUsuario)
                .filter(u -> codigos.puedeEnviar(u.getId(), TipoCodigo.RESET))
                .ifPresent(u -> enviarCodigo(u, TipoCodigo.RESET));
    }

    @Transactional
    public void resetPassword(String emailUsuario, String codigo, String nuevaPassword) {
        Usuario u = usuarios.findByEmail(emailUsuario).orElseThrow(CodigoService::invalido);
        codigos.consumir(u.getId(), TipoCodigo.RESET, codigo);
        u.cambiarPassword(passwordEncoder.encode(nuevaPassword));
        // Quien pudo leer el código controla el email: lo damos por verificado.
        u.marcarEmailVerificado();
        tokens.revocarTodos(u);
    }

    private void enviarCodigo(Usuario u, TipoCodigo tipo) {
        String codigo = codigos.generar(u.getId(), tipo);
        String asunto;
        String motivo;
        if (tipo == TipoCodigo.RESET) {
            asunto = "Tu código para cambiar la contraseña de MiMascota";
            motivo = "Pediste cambiar tu contraseña.";
        } else {
            asunto = "Tu código para verificar tu cuenta de MiMascota";
            motivo = "Gracias por registrarte en MiMascota.";
        }
        email.enviar(u.getEmail(), asunto, """
                Hola %s,

                %s Tu código es:

                %s

                Vence en 10 minutos. Si no fuiste vos, ignorá este mensaje.
                """.formatted(u.getNombre(), motivo, codigo));
    }

    private static ApiException demasiadosEnvios() {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "demasiados_envios",
                "Esperá un minuto antes de pedir otro código");
    }
}
