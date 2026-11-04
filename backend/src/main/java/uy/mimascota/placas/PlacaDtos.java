package uy.mimascota.placas;

import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.UUID;
import uy.mimascota.mascotas.MascotaDtos.MascotaRequest;

public final class PlacaDtos {

    private PlacaDtos() {
    }

    /** Activar con una mascota que ya existe ({@code mascotaId}) o con una nueva ({@code mascota}). */
    public record ActivarRequest(UUID mascotaId, @Valid MascotaRequest mascota) {
    }

    public record AsignarRequest(UUID mascotaId) {
    }

    public record PlacaDto(String codigo, EstadoPlaca estado, UUID mascotaId, String mascotaNombre,
                           OffsetDateTime fechaAlta) {
    }
}
