package uy.mimascota.placas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uy.mimascota.PruebasConfig;
import uy.mimascota.auth.EmailsEnMemoria;

/** Mascotas, activación de placas y lotes de admin, contra PostgreSQL real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PruebasConfig.class)
class PlacasYMascotasTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    EmailsEnMemoria emails;

    @Autowired
    ObjectMapper json;

    private static final String TOBY = """
            {"nombre":"Toby","especie":"PERRO","raza":"Mestizo","color":"Marrón","sexo":"MACHO"}""";

    @Test
    void crear_editar_y_borrar_mascota() throws Exception {
        String ana = usuario();
        JsonNode m = cuerpo(con(post("/api/v1/mascotas"), ana, TOBY).andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("TITULAR"))
                .andExpect(jsonPath("$.camposPublicos.length()").value(3)));
        String id = m.get("id").asText();

        con(patch("/api/v1/mascotas/" + id), ana, """
                {"nombre":"Toby","especie":"PERRO","raza":"","color":"Negro","perdida":true,"castrada":true,
                 "camposPublicos":["FOTO","CONTACTO"],"fechaNacimiento":"2020-05-01"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raza").doesNotExist())
                .andExpect(jsonPath("$.color").value("Negro"))
                .andExpect(jsonPath("$.castrada").value(true))
                .andExpect(jsonPath("$.perdida").value(true))
                .andExpect(jsonPath("$.camposPublicos.length()").value(2));

        con(get("/api/v1/mascotas"), ana, null).andExpect(jsonPath("$.length()").value(1));

        // Validación y fecha futura
        con(post("/api/v1/mascotas"), ana, """
                {"nombre":"","especie":"PERRO","fechaNacimiento":"2999-01-01"}""")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.length()").value(2));

        con(delete("/api/v1/mascotas/" + id), ana, null).andExpect(status().isNoContent());
        con(get("/api/v1/mascotas/" + id), ana, null).andExpect(status().isNotFound());
    }

    @Test
    void mascota_ajena_da_404() throws Exception {
        String ana = usuario();
        String beto = usuario();
        String id = cuerpo(con(post("/api/v1/mascotas"), ana, TOBY)).get("id").asText();
        con(get("/api/v1/mascotas/" + id), beto, null).andExpect(status().isNotFound());
        con(patch("/api/v1/mascotas/" + id), beto, TOBY).andExpect(status().isNotFound());
        con(delete("/api/v1/mascotas/" + id), beto, null).andExpect(status().isNotFound());
        con(get("/api/v1/mascotas"), beto, null).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void activar_placa_con_mascota_nueva_y_reasignar() throws Exception {
        String admin = admin();
        JsonNode lote = cuerpo(con(post("/api/v1/admin/lotes"), admin, """
                {"cantidad":3}""").andExpect(status().isCreated()));
        assertThat(lote.get("codigos")).hasSize(3);
        String codigo = lote.get("codigos").get(0).asText();
        String otro = lote.get("codigos").get(1).asText();
        assertThat(codigo).matches("[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{8}");

        String ana = usuario();
        // Se acepta el código en minúsculas y con guion
        String escrito = codigo.substring(0, 4).toLowerCase() + "-" + codigo.substring(4);
        con(post("/api/v1/placas/" + escrito + "/activar"), ana, """
                {"mascota":%s}""".formatted(TOBY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo))
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.mascotaNombre").value("Toby"));

        // Ya activa: 409, también para otro usuario
        con(post("/api/v1/placas/" + codigo + "/activar"), usuario(), """
                {"mascota":%s}""".formatted(TOBY)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("placa_ya_activa"));
        // Inexistente: 404
        con(post("/api/v1/placas/ZZZZZZZZ/activar"), ana, """
                {"mascota":%s}""".formatted(TOBY)).andExpect(status().isNotFound());
        // Sin mascota ni mascotaId: 400
        con(post("/api/v1/placas/" + otro + "/activar"), ana, "{}").andExpect(status().isBadRequest());

        JsonNode mascotas = cuerpo(con(get("/api/v1/mascotas"), ana, null));
        String tobyId = mascotas.get(0).get("id").asText();
        assertThat(mascotas.get(0).get("placas").get(0).asText()).isEqualTo(codigo);

        // Segunda placa para la misma mascota existente
        con(post("/api/v1/placas/" + otro + "/activar"), ana, """
                {"mascotaId":"%s"}""".formatted(tobyId)).andExpect(status().isOk());
        con(get("/api/v1/mascotas/" + tobyId), ana, null).andExpect(jsonPath("$.placas.length()").value(2));

        // Liberar una placa y verla en "mis placas" sin mascota
        con(put("/api/v1/placas/" + otro + "/mascota"), ana, "{\"mascotaId\":null}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.mascotaId").doesNotExist());
        con(get("/api/v1/placas"), ana, null).andExpect(jsonPath("$.length()").value(2));

        // Otro usuario no puede reasignar mis placas ni activar con mi mascota
        String beto = usuario();
        con(put("/api/v1/placas/" + otro + "/mascota"), beto, "{\"mascotaId\":null}").andExpect(status().isNotFound());
        String tercera = lote.get("codigos").get(2).asText();
        con(post("/api/v1/placas/" + tercera + "/activar"), beto, """
                {"mascotaId":"%s"}""".formatted(tobyId)).andExpect(status().isNotFound());

        // Borrar la mascota deja la placa libre pero del usuario
        con(delete("/api/v1/mascotas/" + tobyId), ana, null).andExpect(status().isNoContent());
        con(get("/api/v1/placas"), ana, null)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].mascotaId").doesNotExist());
    }

    @Test
    void lotes_admin_listado_y_csv() throws Exception {
        String admin = admin();
        String nombre = "PRUEBA-" + UUID.randomUUID().toString().substring(0, 8);
        con(post("/api/v1/admin/lotes"), admin, """
                {"cantidad":2,"nombre":"%s"}""".formatted(nombre)).andExpect(status().isCreated());
        con(post("/api/v1/admin/lotes"), admin, """
                {"cantidad":2,"nombre":"%s"}""".formatted(nombre)).andExpect(status().isConflict());
        con(post("/api/v1/admin/lotes"), admin, "{\"cantidad\":501}").andExpect(status().isBadRequest());

        con(get("/api/v1/admin/lotes"), admin, null)
                .andExpect(jsonPath("$[?(@.lote=='%s')].sinActivar".formatted(nombre)).value(2));
        con(get("/api/v1/admin/lotes/" + nombre + "/export"), admin, null)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + nombre + ".csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/p/")));

        con(get("/api/v1/admin/lotes/" + nombre + "/placas"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estado").value("SIN_ACTIVAR"))
                .andExpect(jsonPath("$[0].url").value(org.hamcrest.Matchers.containsString("/p/")));

        byte[] pdf = con(get("/api/v1/admin/lotes/" + nombre + "/export.pdf"), admin, null)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        con(get("/api/v1/admin/lotes/NO-EXISTE/export.pdf"), admin, null).andExpect(status().isNotFound());

        con(post("/api/v1/admin/lotes"), usuario(), "{\"cantidad\":1}").andExpect(status().isForbidden());
        con(get("/api/v1/admin/lotes/" + nombre + "/placas"), usuario(), null).andExpect(status().isForbidden());
    }

    @Test
    void foto_se_achica_y_se_sirve() throws Exception {
        String ana = usuario();
        String id = cuerpo(con(post("/api/v1/mascotas"), ana, TOBY)).get("id").asText();

        BufferedImage grande = new BufferedImage(1600, 1200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(grande, "png", png);
        MockMultipartFile foto = new MockMultipartFile("foto", "toby.png", "image/png", png.toByteArray());

        String url = cuerpo(mvc.perform(multipart(HttpMethod.PUT, "/api/v1/mascotas/" + id + "/foto").file(foto)
                        .header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())).get("fotoUrl").asText();
        assertThat(url).startsWith("/fotos/").endsWith(".jpg");

        byte[] servida = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        BufferedImage leida = ImageIO.read(new java.io.ByteArrayInputStream(servida));
        assertThat(leida.getWidth()).isEqualTo(800);
        assertThat(leida.getHeight()).isEqualTo(600);

        MockMultipartFile texto = new MockMultipartFile("foto", "x.txt", "text/plain", "hola".getBytes());
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/mascotas/" + id + "/foto").file(texto)
                        .header("Authorization", "Bearer " + ana))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("foto_invalida"));

        con(delete("/api/v1/mascotas/" + id + "/foto"), ana, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").doesNotExist());
        mvc.perform(get(url)).andExpect(status().isNotFound());
    }

    // --- helpers ---

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

    /** Crea una cuenta verificada y devuelve su access token. */
    private String usuario() throws Exception {
        String email = "prueba-" + UUID.randomUUID() + "@test.uy";
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"clave-segura-1","nombre":"Ana"}""".formatted(email)))
                .andExpect(status().isCreated());
        return cuerpo(mvc.perform(post("/api/v1/auth/verificar-email").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","codigo":"%s"}""".formatted(email, emails.ultimoCodigo(email)))))
                .get("accessToken").asText();
    }

    private String admin() throws Exception {
        return cuerpo(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"admin-pruebas@test.uy","password":"admin-pruebas-123"}""")).andExpect(status().isOk()))
                .get("accessToken").asText();
    }
}
