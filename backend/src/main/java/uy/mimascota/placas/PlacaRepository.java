package uy.mimascota.placas;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlacaRepository extends JpaRepository<Placa, String> {

    List<Placa> findByUsuarioIdOrderByFechaAlta(UUID usuarioId);

    List<Placa> findByMascotaIdInOrderByFechaAlta(Collection<UUID> mascotaIds);

    List<Placa> findByLoteOrderByCodigo(String lote);

    boolean existsByLote(String lote);

    @Modifying
    @Query("update Placa p set p.mascotaId = null where p.mascotaId = :mascotaId")
    void liberarDeMascota(UUID mascotaId);

    /** Resumen por lote: nombre, fecha de alta y cantidad de placas en cada estado. */
    @Query("""
            select p.lote as lote, min(p.fechaAlta) as fechaAlta, count(p) as total,
                   sum(case when p.estado = uy.mimascota.placas.EstadoPlaca.SIN_ACTIVAR then 1 else 0 end) as sinActivar,
                   sum(case when p.estado = uy.mimascota.placas.EstadoPlaca.ACTIVA then 1 else 0 end) as activas,
                   sum(case when p.estado = uy.mimascota.placas.EstadoPlaca.BLOQUEADA then 1 else 0 end) as bloqueadas
            from Placa p group by p.lote order by min(p.fechaAlta) desc""")
    List<ResumenLote> resumenLotes();

    interface ResumenLote {
        String getLote();

        java.time.OffsetDateTime getFechaAlta();

        long getTotal();

        long getSinActivar();

        long getActivas();

        long getBloqueadas();
    }
}
