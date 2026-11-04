package uy.mimascota.mascotas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "mascota")
public class Mascota {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Especie especie;

    private String raza;

    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    private String color;

    private String foto;

    @Column(name = "info_medica")
    private String infoMedica;

    private String observaciones;

    @Column(nullable = false)
    private boolean perdida;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "campos_publicos", nullable = false, columnDefinition = "varchar(20)[]")
    private String[] camposPublicos = {"FOTO", "RAZA", "COLOR"};

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private OffsetDateTime fechaAlta;

    protected Mascota() {
    }

    public Mascota(String nombre, Especie especie) {
        this.id = UUID.randomUUID();
        this.nombre = nombre.trim();
        this.especie = especie;
        this.fechaAlta = OffsetDateTime.now();
    }

    public void actualizar(String nombre, Especie especie, String raza, Sexo sexo, LocalDate fechaNacimiento,
                           String color, String infoMedica, String observaciones) {
        this.nombre = nombre.trim();
        this.especie = especie;
        this.raza = limpio(raza);
        this.sexo = sexo;
        this.fechaNacimiento = fechaNacimiento;
        this.color = limpio(color);
        this.infoMedica = limpio(infoMedica);
        this.observaciones = limpio(observaciones);
    }

    public void setPerdida(boolean perdida) {
        this.perdida = perdida;
    }

    public void setCamposPublicos(Set<CampoPublico> campos) {
        this.camposPublicos = campos.stream().sorted().map(Enum::name).toArray(String[]::new);
    }

    public Set<CampoPublico> getCamposPublicos() {
        EnumSet<CampoPublico> set = EnumSet.noneOf(CampoPublico.class);
        Arrays.stream(camposPublicos).forEach(c -> set.add(CampoPublico.valueOf(c)));
        return set;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    private static String limpio(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Especie getEspecie() {
        return especie;
    }

    public String getRaza() {
        return raza;
    }

    public Sexo getSexo() {
        return sexo;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getColor() {
        return color;
    }

    public String getFoto() {
        return foto;
    }

    public String getInfoMedica() {
        return infoMedica;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public boolean isPerdida() {
        return perdida;
    }

    public OffsetDateTime getFechaAlta() {
        return fechaAlta;
    }
}
