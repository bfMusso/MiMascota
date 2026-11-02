package uy.mimascota.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "codigo_verificacion")
public class CodigoVerificacion {

    public static final int MAX_INTENTOS = 5;

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCodigo tipo;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(nullable = false)
    private OffsetDateTime expira;

    @Column(nullable = false)
    private short intentos;

    @Column(nullable = false)
    private boolean usado;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private OffsetDateTime fechaAlta;

    protected CodigoVerificacion() {
    }

    public CodigoVerificacion(UUID usuarioId, TipoCodigo tipo, String codigoHash, OffsetDateTime expira) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.codigoHash = codigoHash;
        this.expira = expira;
        this.fechaAlta = OffsetDateTime.now();
    }

    public String getCodigoHash() {
        return codigoHash;
    }

    /** Usable si no se usó, no venció y no agotó los intentos. */
    public boolean vigente() {
        return !usado && intentos < MAX_INTENTOS && expira.isAfter(OffsetDateTime.now());
    }

    public void registrarIntentoFallido() {
        intentos++;
    }

    public void marcarUsado() {
        usado = true;
    }

    public void invalidar() {
        usado = true;
    }
}
