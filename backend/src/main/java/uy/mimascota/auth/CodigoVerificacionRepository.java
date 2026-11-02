package uy.mimascota.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, UUID> {

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndTipoOrderByFechaAltaDesc(UUID usuarioId, TipoCodigo tipo);

    @Query("select count(c) from CodigoVerificacion c where c.usuarioId = ?1 and c.tipo = ?2 and c.fechaAlta > ?3")
    long contarDesde(UUID usuarioId, TipoCodigo tipo, OffsetDateTime desde);

    @Query("select c.fechaAlta from CodigoVerificacion c where c.usuarioId = ?1 and c.tipo = ?2 order by c.fechaAlta desc limit 1")
    Optional<OffsetDateTime> ultimoEnvio(UUID usuarioId, TipoCodigo tipo);

    @Query("select c from CodigoVerificacion c where c.usuarioId = ?1 and c.tipo = ?2 and c.usado = false")
    List<CodigoVerificacion> pendientes(UUID usuarioId, TipoCodigo tipo);
}
