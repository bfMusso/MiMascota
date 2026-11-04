package uy.mimascota.mascotas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

/** Quién es responsable de cada mascota. En el MVP solo hay titular; el co-responsable llega en fase 2. */
@Entity
@Table(name = "mascota_usuario")
@IdClass(MascotaUsuario.Clave.class)
public class MascotaUsuario {

    public record Clave(UUID mascotaId, UUID usuarioId) implements Serializable {
        public Clave() {
            this(null, null);
        }
    }

    @Id
    @Column(name = "mascota_id")
    private UUID mascotaId;

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolMascota rol;

    protected MascotaUsuario() {
    }

    public MascotaUsuario(UUID mascotaId, UUID usuarioId, RolMascota rol) {
        this.mascotaId = mascotaId;
        this.usuarioId = usuarioId;
        this.rol = rol;
    }

    public UUID getMascotaId() {
        return mascotaId;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public RolMascota getRol() {
        return rol;
    }
}
