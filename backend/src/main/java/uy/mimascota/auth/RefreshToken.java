package uy.mimascota.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private OffsetDateTime expira;

    @Column(nullable = false)
    private boolean revocado;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private OffsetDateTime fechaAlta;

    protected RefreshToken() {
    }

    public RefreshToken(UUID usuarioId, String tokenHash, OffsetDateTime expira) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expira = expira;
        this.fechaAlta = OffsetDateTime.now();
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public boolean isRevocado() {
        return revocado;
    }

    public boolean vencido() {
        return expira.isBefore(OffsetDateTime.now());
    }

    public void revocar() {
        this.revocado = true;
    }
}
