package uy.mimascota.mascotas;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MascotaRepository extends JpaRepository<Mascota, UUID> {

    @Query("""
            select m from Mascota m
            where m.id in (select mu.mascotaId from MascotaUsuario mu where mu.usuarioId = :usuarioId)
            order by m.fechaAlta""")
    List<Mascota> deUsuario(UUID usuarioId);
}
