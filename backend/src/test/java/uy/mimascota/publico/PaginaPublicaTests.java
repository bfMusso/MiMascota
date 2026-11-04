package uy.mimascota.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uy.mimascota.PruebasConfig;
import uy.mimascota.auth.EmailsEnMemoria;

/** Ficha pública, escaneos y avisos al dueño. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PruebasConfig.class)
class PaginaPublicaTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    EmailsEnMemoria emails;

    @Autowired
    ObjectMapper json;

    record Dueno(String email, String token) {
    }

    @Test
    void ficha_segun_estado_y_campos_publicos() throws Exception {
        List<String> codigos = lote(3);
        String libre = codigos.get(0);

        // Sin activar
        publico(get("/api/v1/public/placas/" + libre.toLowerCase())).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SIN_ACTIVAR"))
                .andExpect(jsonPath("$.mascota").doesNotExist());
        // Inexistente
        publico(get("/api/v1/public/placas/ZZZZZZZZ")).andExpect(status().isNotFound());
        // No se puede escanear una sin activar
        publico(post("/api/v1/public/placas/" + libre + "/escaneos")).andExpect(status().isNotFound());

        Dueno ana = dueno("099 111 222");
        String codigo = codigos.get(1);
        String id = activar(ana, codigo, """
                {"nombre":"Toby","especie":"PERRO","raza":"Mestizo","color":"Marrón","sexo":"MACHO","castrada":true,
                 "infoMedica":"Alérgico","fechaNacimiento":"2020-01-15"}""");

        // Por defecto: foto, raza y color. Nunca email.
        publico(get("/api/v1/public/placas/" + codigo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.mascota.nombre").value("Toby"))
                .andExpect(jsonPath("$.mascota.especie").value("PERRO"))
                .andExpect(jsonPath("$.mascota.raza").value("Mestizo"))
                .andExpect(jsonPath("$.mascota.sexo").doesNotExist())
                .andExpect(jsonPath("$.mascota.infoMedica").doesNotExist())
                .andExpect(jsonPath("$.mascota.contacto").doesNotExist())
                .andExpect(jsonPath("$..email").doesNotExist());

        // El dueño elige mostrar más cosas
        con(patch("/api/v1/mascotas/" + id), ana.token(), """
                {"nombre":"Toby","especie":"PERRO","raza":"Mestizo","color":"Marrón","sexo":"MACHO","castrada":true,
                 "infoMedica":"Alérgico","fechaNacimiento":"2020-01-15",
                 "camposPublicos":["SEXO","EDAD","INFO_MEDICA","CONTACTO"]}""").andExpect(status().isOk());
        publico(get("/api/v1/public/placas/" + codigo))
                .andExpect(jsonPath("$.mascota.raza").doesNotExist())
                .andExpect(jsonPath("$.mascota.sexo").value("MACHO"))
                .andExpect(jsonPath("$.mascota.castrada").value(true))
                .andExpect(jsonPath("$.mascota.edadMeses").isNumber())
                .andExpect(jsonPath("$.mascota.infoMedica").value("Alérgico"))
                .andExpect(jsonPath("$.mascota.contacto.nombre").value("Ana"))
                .andExpect(jsonPath("$.mascota.contacto.telefono").value("099 111 222"));
    }

    @Test
    void escaneo_avisa_al_dueno_y_queda_en_el_historial() throws Exception {
        String codigo = lote(1).get(0);
        Dueno ana = dueno(null);
        String id = activar(ana, codigo, """
                {"nombre":"Luna","especie":"GATO"}""");
        int antes = emails.para(ana.email()).size();

        // Primer escaneo sin datos: avisa. Segundo dentro de la hora: no.
        publico(post("/api/v1/public/placas/" + codigo + "/escaneos")).andExpect(status().isAccepted());
        publico(post("/api/v1/public/placas/" + codigo + "/escaneos")).andExpect(status().isAccepted());
        assertThat(emails.para(ana.email())).hasSize(antes + 1);
        assertThat(ultimo(ana).asunto()).isEqualTo("Escanearon la placa de Luna");

        // Con mensaje, avisa siempre, con ubicación
        publico(post("/api/v1/public/placas/" + codigo + "/escaneos"), """
                {"lat":-34.9011,"lng":-56.1645,"mensaje":"La tengo yo, está bien","contacto":"099 555 444"}""")
                .andExpect(status().isAccepted());
        assertThat(emails.para(ana.email())).hasSize(antes + 2);
        EmailsEnMemoria.Email e = ultimo(ana);
        assertThat(e.asunto()).isEqualTo("Te dejaron un mensaje por Luna");
        assertThat(e.texto()).contains("La tengo yo, está bien", "099 555 444",
                "https://www.google.com/maps?q=-34.9011,-56.1645");

        // Perdida: avisa en cada escaneo
        con(patch("/api/v1/mascotas/" + id), ana.token(), """
                {"nombre":"Luna","especie":"GATO","perdida":true}""").andExpect(status().isOk());
        publico(post("/api/v1/public/placas/" + codigo + "/escaneos")).andExpect(status().isAccepted());
        assertThat(ultimo(ana).asunto()).isEqualTo("¡Alguien encontró a Luna!");
        publico(get("/api/v1/public/placas/" + codigo)).andExpect(jsonPath("$.mascota.perdida").value(true));

        // Validación
        publico(post("/api/v1/public/placas/" + codigo + "/escaneos"), """
                {"lat":100,"mensaje":"%s"}""".formatted("x".repeat(501))).andExpect(status().isBadRequest());

        // Historial: solo para el dueño, del más nuevo al más viejo
        con(get("/api/v1/mascotas/" + id + "/escaneos"), ana.token(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[1].mensaje").value("La tengo yo, está bien"))
                .andExpect(jsonPath("$.content[1].lat").value(-34.9011));
        con(get("/api/v1/mascotas/" + id + "/escaneos"), dueno(null).token(), null).andExpect(status().isNotFound());
    }

    @Test
    void limite_de_escaneos_por_ip() throws Exception {
        String codigo = lote(1).get(0);
        activar(dueno(null), codigo, """
                {"nombre":"Rocco","especie":"PERRO"}""");
        String ip = "10.9." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250);
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/public/placas/" + codigo + "/escaneos").header("X-Real-IP", ip))
                    .andExpect(status().isAccepted());
        }
        mvc.perform(post("/api/v1/public/placas/" + codigo + "/escaneos").header("X-Real-IP", ip))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("demasiadas_solicitudes"));
    }

    // --- helpers ---

    /** Cada pedido público viene de una IP distinta, para no chocar con el límite. */
    private ResultActions publico(MockHttpServletRequestBuilder req) throws Exception {
        return publico(req, null);
    }

    private ResultActions publico(MockHttpServletRequestBuilder req, String body) throws Exception {
        req.header("X-Real-IP", "10.0." + (int) (Math.random() * 250) + "." + (int) (Math.random() * 250));
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(req);
    }

    private ResultActions con(MockHttpServletRequestBuilder req, String token, String body) throws Exception {
        req.header("Authorization", "Bearer " + token);
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mvc.perform(req);
    }

    private JsonNode cuerpo(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private EmailsEnMemoria.Email ultimo(Dueno d) {
        List<EmailsEnMemoria.Email> l = emails.para(d.email());
        return l.get(l.size() - 1);
    }

    private List<String> lote(int cantidad) throws Exception {
        String admin = cuerpo(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"admin-pruebas@test.uy","password":"admin-pruebas-123"}"""))).get("accessToken").asText();
        JsonNode lote = cuerpo(con(post("/api/v1/admin/lotes"), admin, "{\"cantidad\":%d}".formatted(cantidad)));
        return java.util.stream.StreamSupport.stream(lote.get("codigos").spliterator(), false).map(JsonNode::asText).toList();
    }

    private String activar(Dueno d, String codigo, String mascota) throws Exception {
        String mascotaId = cuerpo(con(post("/api/v1/placas/" + codigo + "/activar"), d.token(), """
                {"mascota":%s}""".formatted(mascota)).andExpect(status().isOk())).get("mascotaId").asText();
        return mascotaId;
    }

    private Dueno dueno(String telefono) throws Exception {
        String email = "dueno-" + UUID.randomUUID() + "@test.uy";
        String tel = telefono == null ? "null" : "\"" + telefono + "\"";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"clave-segura-1","nombre":"Ana López","telefono":%s}""".formatted(email, tel)))
                .andExpect(status().isCreated());
        String token = cuerpo(mvc.perform(post("/api/v1/auth/verificar-email").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","codigo":"%s"}""".formatted(email, emails.ultimoCodigo(email)))))
                .get("accessToken").asText();
        return new Dueno(email, token);
    }
}
