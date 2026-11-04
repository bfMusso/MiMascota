package uy.mimascota.escaneos;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;
import uy.mimascota.auth.EnviadorEmail;
import uy.mimascota.config.WebProperties;
import uy.mimascota.mascotas.Mascota;
import uy.mimascota.usuarios.Usuario;

/** Arma y manda el email al dueño cuando escanean una de sus placas. */
@Component
public class AvisoEscaneo {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm", Locale.of("es", "UY"))
            .withZone(ZoneId.of("America/Montevideo"));

    private final EnviadorEmail email;
    private final WebProperties web;

    public AvisoEscaneo(EnviadorEmail email, WebProperties web) {
        this.email = email;
        this.web = web;
    }

    /** {@code mascota} puede ser null si la placa está activa pero libre. */
    public void enviar(Usuario dueno, Escaneo e, Mascota mascota) {
        String quien = mascota != null ? mascota.getNombre() : "tu placa " + e.getPlacaCodigo();
        String asunto = mascota != null && mascota.isPerdida()
                ? "¡Alguien encontró a " + mascota.getNombre() + "!"
                : e.getMensaje() != null
                        ? "Te dejaron un mensaje por " + quien
                        : "Escanearon la placa de " + quien;

        StringBuilder t = new StringBuilder();
        t.append("Hola ").append(primerNombre(dueno.getNombre())).append(",\n\n");
        t.append("El ").append(HORA.format(e.getFecha())).append(" alguien escaneó ")
                .append(mascota != null ? "la placa de " + mascota.getNombre() : "tu placa " + e.getPlacaCodigo())
                .append(".\n\n");
        if (e.getMensaje() != null) {
            t.append("Su mensaje:\n\"").append(e.getMensaje()).append("\"\n\n");
        }
        if (e.getContacto() != null) {
            t.append("Para contactarlo: ").append(e.getContacto()).append("\n\n");
        }
        if (e.tieneUbicacion()) {
            t.append("Dónde estaba: https://www.google.com/maps?q=").append(e.getLat()).append(',').append(e.getLng())
                    .append("\n\n");
        } else {
            t.append("No compartió su ubicación.\n\n");
        }
        if (mascota != null) {
            t.append("Ver todos los escaneos: ").append(web.url()).append("/app/mascotas/").append(mascota.getId())
                    .append("\n\n");
        }
        t.append("MiMascota");
        email.enviar(dueno.getEmail(), asunto, t.toString());
    }

    private static String primerNombre(String nombre) {
        return nombre.trim().split("\\s+")[0];
    }
}
