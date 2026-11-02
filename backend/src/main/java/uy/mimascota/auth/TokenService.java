package uy.mimascota.auth;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.mimascota.comun.ApiException;
import uy.mimascota.comun.Hash;
import uy.mimascota.config.AuthProperties;
import uy.mimascota.usuarios.Usuario;
import uy.mimascota.usuarios.UsuarioRepository;

/** Emite access tokens (JWT) y administra los refresh tokens rotativos. */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties props;
    private final RefreshTokenRepository refreshTokens;
    private final UsuarioRepository usuarios;

    public TokenService(JwtEncoder jwtEncoder, AuthProperties props, RefreshTokenRepository refreshTokens,
                        UsuarioRepository usuarios) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
        this.refreshTokens = refreshTokens;
        this.usuarios = usuarios;
    }

    public record Tokens(String accessToken, long expiraEnSegundos, String refreshToken, Usuario usuario) {
    }

    @Transactional
    public Tokens emitir(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("mimascota")
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(props.accessTtl()))
                .claim("email", usuario.getEmail())
                .claim("rol", List.of(usuario.getRol().name()))
                .build();
        String access = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        String refresh = Hash.tokenAleatorio();
        refreshTokens.save(new RefreshToken(usuario.getId(), Hash.sha256(refresh), OffsetDateTime.now().plus(props.refreshTtl())));
        return new Tokens(access, props.accessTtl().toSeconds(), refresh, usuario);
    }

    /**
     * Cambia un refresh token por uno nuevo. Si llega un token ya revocado, se asume que fue robado
     * y se cierran todas las sesiones del usuario.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Tokens rotar(String refreshToken) {
        RefreshToken actual = buscar(refreshToken);
        if (actual.isRevocado()) {
            refreshTokens.revocarTodos(actual.getUsuarioId());
            throw sesionInvalida();
        }
        if (actual.vencido()) {
            throw sesionInvalida();
        }
        actual.revocar();
        Usuario usuario = usuarios.findById(actual.getUsuarioId()).orElseThrow(TokenService::sesionInvalida);
        return emitir(usuario);
    }

    @Transactional
    public void revocar(String refreshToken) {
        if (refreshToken != null) {
            refreshTokens.findByTokenHash(Hash.sha256(refreshToken)).ifPresent(RefreshToken::revocar);
        }
    }

    @Transactional
    public void revocarTodos(Usuario usuario) {
        refreshTokens.revocarTodos(usuario.getId());
    }

    private RefreshToken buscar(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw sesionInvalida();
        }
        return refreshTokens.findByTokenHash(Hash.sha256(refreshToken)).orElseThrow(TokenService::sesionInvalida);
    }

    static ApiException sesionInvalida() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "sesion_invalida", "La sesión venció. Iniciá sesión de nuevo.");
    }
}
