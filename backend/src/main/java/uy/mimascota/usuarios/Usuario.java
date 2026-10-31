package uy.mimascota.usuarios;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String nombre;

    private String telefono;

    @Column(name = "mostrar_contacto", nullable = false)
    private boolean mostrarContacto;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol = Rol.USER;

    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private OffsetDateTime fechaAlta;

    protected Usuario() {
    }

    public Usuario(String email, String passwordHash, String nombre, Rol rol) {
        this.id = UUID.randomUUID();
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.rol = rol;
        this.fechaAlta = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public boolean isMostrarContacto() {
        return mostrarContacto;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public Rol getRol() {
        return rol;
    }

    public OffsetDateTime getFechaAlta() {
        return fechaAlta;
    }

    public void marcarEmailVerificado() {
        this.emailVerificado = true;
    }
}
