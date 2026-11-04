package uy.mimascota.escaneos;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EscaneoRepository extends JpaRepository<Escaneo, UUID> {

    boolean existsByPlacaCodigoAndNotificadoTrueAndFechaAfter(String placaCodigo, OffsetDateTime desde);

    /** Escaneos de las placas que hoy están asignadas a la mascota, del más nuevo al más viejo. */
    @Query("""
            select e from Escaneo e
            where e.placaCodigo in (select p.codigo from Placa p where p.mascotaId = :mascotaId)
            order by e.fecha desc""")
    Page<Escaneo> deMascota(UUID mascotaId, Pageable pageable);
}
