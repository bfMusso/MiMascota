package uy.mimascota.mascotas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MascotaDtos {

    private MascotaDtos() {
    }

    /**
     * Ficha completa para crear o editar. Al editar se manda la ficha entera:
     * un campo opcional vacío o en null se borra. {@code perdida} y {@code camposPublicos}
     * en null quedan como estaban.
     */
    public record MascotaRequest(
            @NotBlank @Size(max = 60) String nombre,
            @NotNull Especie especie,
            @Size(max = 60) String raza,
            Sexo sexo,
            @PastOrPresent(message = "no puede ser una fecha futura") LocalDate fechaNacimiento,
            @Size(max = 60) String color,
            @Size(max = 1000) String infoMedica,
            @Size(max = 1000) String observaciones,
            Boolean perdida,
            Set<CampoPublico> camposPublicos) {
    }

    public record MascotaDto(
            UUID id,
            String nombre,
            Especie especie,
            String raza,
            Sexo sexo,
            LocalDate fechaNacimiento,
            String color,
            String fotoUrl,
            String infoMedica,
            String observaciones,
            boolean perdida,
            Set<CampoPublico> camposPublicos,
            RolMascota rol,
            List<String> placas) {

        static MascotaDto de(Mascota m, RolMascota rol, List<String> placas) {
            return new MascotaDto(m.getId(), m.getNombre(), m.getEspecie(), m.getRaza(), m.getSexo(),
                    m.getFechaNacimiento(), m.getColor(), m.getFoto() == null ? null : "/fotos/" + m.getFoto(),
                    m.getInfoMedica(), m.getObservaciones(), m.isPerdida(), m.getCamposPublicos(), rol, placas);
        }
    }
}
