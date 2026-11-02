package uy.mimascota.usuarios;

import java.util.UUID;

public record UsuarioDto(UUID id, String email, String nombre, String telefono, boolean mostrarContacto, Rol rol) {

    public static UsuarioDto de(Usuario u) {
        return new UsuarioDto(u.getId(), u.getEmail(), u.getNombre(), u.getTelefono(), u.isMostrarContacto(), u.getRol());
    }
}
