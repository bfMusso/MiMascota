package uy.mimascota.publico;

import com.fasterxml.jackson.annotation.JsonInclude;
import uy.mimascota.mascotas.Especie;
import uy.mimascota.mascotas.Sexo;
import uy.mimascota.placas.EstadoPlaca;

/**
 * Lo que ve quien escanea una placa. Solo trae los datos que el dueño eligió mostrar;
 * los demás no vienen en la respuesta. Nunca incluye email ni dirección.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FichaPublica(String codigo, EstadoPlaca estado, Mascota mascota) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Mascota(
            String nombre,
            Especie especie,
            boolean perdida,
            String fotoUrl,
            String raza,
            String color,
            Sexo sexo,
            Boolean castrada,
            Integer edadMeses,
            String infoMedica,
            String observaciones,
            Contacto contacto) {
    }

    public record Contacto(String nombre, String telefono) {
    }
}
