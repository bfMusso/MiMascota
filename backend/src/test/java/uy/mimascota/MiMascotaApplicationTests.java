package uy.mimascota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import uy.mimascota.usuarios.Rol;
import uy.mimascota.usuarios.UsuarioRepository;

/**
 * Prueba de integración contra PostgreSQL real. Antes de correrla, levantá la base:
 * {@code docker compose -f infra/docker-compose.yml up -d db}
 */
@SpringBootTest(properties = {
        "mimascota.admin.email=admin@test.uy",
        "mimascota.admin.password=clave-de-prueba"
})
@AutoConfigureMockMvc
class MiMascotaApplicationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Test
    void migraciones_y_entidades_coinciden_y_se_crea_el_admin() {
        var admin = usuarios.findByEmail("ADMIN@test.uy");
        assertThat(admin).isPresent();
        assertThat(admin.get().getRol()).isEqualTo(Rol.ADMIN);
        assertThat(admin.get().isEmailVerificado()).isTrue();
    }

    @Test
    void health_es_publico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void rutas_privadas_piden_login() throws Exception {
        mvc.perform(get("/api/v1/mascotas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/lotes")).andExpect(status().isUnauthorized());
    }
}
