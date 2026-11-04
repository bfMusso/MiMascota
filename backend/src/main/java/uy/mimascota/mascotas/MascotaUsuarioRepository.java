package uy.mimascota.mascotas;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MascotaUsuarioRepository extends JpaRepository<MascotaUsuario, MascotaUsuario.Clave> {

    Optional<MascotaUsuario> findByMascotaIdAndUsuarioId(UUID mascotaId, UUID usuarioId);
}
