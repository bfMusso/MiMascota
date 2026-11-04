package uy.mimascota.escaneos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Cada vez que alguien abre la ficha pública de una placa o deja un mensaje. */
@Entity
@Table(name = "escaneo")
public class Escaneo {

    @Id
    private UUID id;

    @Column(name = "placa_codigo", nullable = false)
    private String placaCodigo;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    private Double lat;

    private Double lng;

    private String mensaje;

    private String contacto;

    @Column(nullable = false)
    private boolean notificado;

    protected Escaneo() {
    }

    public Escaneo(String placaCodigo, Double lat, Double lng, String mensaje, String contacto) {
        this.id = UUID.randomUUID();
        this.placaCodigo = placaCodigo;
        this.fecha = OffsetDateTime.now();
        this.lat = lat;
        this.lng = lng;
        this.mensaje = limpio(mensaje);
        this.contacto = limpio(contacto);
    }

    private static String limpio(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public void marcarNotificado() {
        this.notificado = true;
    }

    public boolean tieneUbicacion() {
        return lat != null && lng != null;
    }

    public UUID getId() {
        return id;
    }

    public String getPlacaCodigo() {
        return placaCodigo;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getContacto() {
        return contacto;
    }

    public boolean isNotificado() {
        return notificado;
    }
}
