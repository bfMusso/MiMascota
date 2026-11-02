package uy.mimascota.auth;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uy.mimascota.comun.ApiException;
import uy.mimascota.comun.Hash;

/**
 * Códigos de 6 dígitos para verificar email, resetear la contraseña y (en fase 2) traspasos.
 * Vencen a los 10 minutos, admiten 5 intentos y son de un solo uso. Se guardan hasheados.
 */
@Service
public class CodigoService {

    static final Duration VIGENCIA = Duration.ofMinutes(10);
    static final Duration ESPERA_ENTRE_ENVIOS = Duration.ofMinutes(1);
    static final int MAX_ENVIOS_POR_HORA = 5;

    private final CodigoVerificacionRepository codigos;
    private final PasswordEncoder passwordEncoder;

    public CodigoService(CodigoVerificacionRepository codigos, PasswordEncoder passwordEncoder) {
        this.codigos = codigos;
        this.passwordEncoder = passwordEncoder;
    }

    /** Indica si se puede enviar un código nuevo sin superar el límite de envíos. */
    public boolean puedeEnviar(UUID usuarioId, TipoCodigo tipo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        boolean muyPronto = codigos.ultimoEnvio(usuarioId, tipo)
                .map(ultimo -> ultimo.isAfter(ahora.minus(ESPERA_ENTRE_ENVIOS)))
                .orElse(false);
        return !muyPronto && codigos.contarDesde(usuarioId, tipo, ahora.minusHours(1)) < MAX_ENVIOS_POR_HORA;
    }

    /** Genera un código nuevo, invalida los anteriores del mismo tipo y devuelve el código en claro. */
    @Transactional
    public String generar(UUID usuarioId, TipoCodigo tipo) {
        codigos.pendientes(usuarioId, tipo).forEach(CodigoVerificacion::invalidar);
        String codigo = Hash.codigoNumerico(6);
        codigos.save(new CodigoVerificacion(usuarioId, tipo, passwordEncoder.encode(codigo), OffsetDateTime.now().plus(VIGENCIA)));
        return codigo;
    }

    /**
     * Consume el código si es correcto. Un intento fallido queda registrado aunque la operación
     * que lo llama falle, por eso corre en su propia transacción.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ApiException.class)
    public void consumir(UUID usuarioId, TipoCodigo tipo, String codigo) {
        CodigoVerificacion c = codigos.findFirstByUsuarioIdAndTipoOrderByFechaAltaDesc(usuarioId, tipo)
                .filter(CodigoVerificacion::vigente)
                .orElseThrow(CodigoService::invalido);
        if (codigo == null || !passwordEncoder.matches(codigo, c.getCodigoHash())) {
            c.registrarIntentoFallido();
            throw invalido();
        }
        c.marcarUsado();
    }

    static ApiException invalido() {
        return new ApiException(HttpStatus.BAD_REQUEST, "codigo_invalido",
                "El código es incorrecto o venció. Pedí uno nuevo si hace falta.");
    }
}
