package uy.mimascota.placas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Placa física (NFC + QR) identificada por su código. Nace SIN_ACTIVAR en un lote;
 * al activarla queda del usuario aunque después se libere de la mascota.
 */
@Entity
@Table(name = "placa")
public class Placa {

    @Id
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPlaca estado = EstadoPlaca.SIN_ACTIVAR;

    @Column(nullable = false)
    private String lote;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "mascota_id")
    private UUID mascotaId;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private OffsetDateTime fechaAlta;

    protected Placa() {
    }

    public Placa(String codigo, String lote) {
        this.codigo = codigo;
        this.lote = lote;
        this.fechaAlta = OffsetDateTime.now();
    }

    public void activar(UUID usuarioId, UUID mascotaId) {
        this.estado = EstadoPlaca.ACTIVA;
        this.usuarioId = usuarioId;
        this.mascotaId = mascotaId;
    }

    public void asignarMascota(UUID mascotaId) {
        this.mascotaId = mascotaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public EstadoPlaca getEstado() {
        return estado;
    }

    public String getLote() {
        return lote;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getMascotaId() {
        return mascotaId;
    }

    public OffsetDateTime getFechaAlta() {
        return fechaAlta;
    }
}
