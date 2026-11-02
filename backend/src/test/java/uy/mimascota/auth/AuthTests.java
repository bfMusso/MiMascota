package uy.mimascota.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import uy.mimascota.PruebasConfig;

/** Flujo completo de cuenta contra PostgreSQL real (ver MiMascotaApplicationTests). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PruebasConfig.class)
class AuthTests {

    private static final String PASSWORD = "clave-segura-1";

    @Autowired
    MockMvc mvc;

    @Autowired
    EmailsEnMemoria emails;

    @Autowired
    ObjectMapper json;

    record Sesion(String access, Cookie refresh) {
    }

    @Test
    void registro_verificacion_y_login() throws Exception {
        String email = emailNuevo();
        registrar(email).andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email));
        assertThat(emails.para(email)).hasSize(1);

        // Sin verificar no puede entrar
        login(email, PASSWORD).andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("email_no_verificado"));

        // Código incorrecto
        postJson("/api/v1/auth/verificar-email", """
                {"email":"%s","codigo":"000000"}""".formatted(email))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("codigo_invalido"));

        // Código correcto: devuelve sesión
        Sesion s = sesion(postJson("/api/v1/auth/verificar-email", """
                {"email":"%s","codigo":"%s"}""".formatted(email, emails.ultimoCodigo(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.email").value(email))
                .andExpect(cookie().httpOnly(AuthController.COOKIE_REFRESH, true)));

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + s.access()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Ana"));

        login(email, PASSWORD).andExpect(status().isOk());
        login(email, "otra-clave-mala").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("credenciales_invalidas"));

        // Email repetido, aunque cambie mayúsculas
        registrar(email.toUpperCase()).andExpect(status().isConflict());
    }

    @Test
    void codigo_se_bloquea_tras_cinco_intentos() throws Exception {
        String email = emailNuevo();
        registrar(email).andExpect(status().isCreated());
        String correcto = emails.ultimoCodigo(email);
        String incorrecto = correcto.equals("111111") ? "222222" : "111111";
        for (int i = 0; i < 5; i++) {
            postJson("/api/v1/auth/verificar-email", """
                    {"email":"%s","codigo":"%s"}""".formatted(email, incorrecto)).andExpect(status().isBadRequest());
        }
        postJson("/api/v1/auth/verificar-email", """
                {"email":"%s","codigo":"%s"}""".formatted(email, correcto)).andExpect(status().isBadRequest());
    }

    @Test
    void reenvio_respeta_un_minuto_y_no_revela_emails() throws Exception {
        String email = emailNuevo();
        registrar(email).andExpect(status().isCreated());
        postJson("/api/v1/auth/reenviar-verificacion", """
                {"email":"%s"}""".formatted(email))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.codigo").value("demasiados_envios"));
        postJson("/api/v1/auth/reenviar-verificacion", """
                {"email":"%s"}""".formatted(emailNuevo())).andExpect(status().isNoContent());
    }

    @Test
    void refresh_rota_y_detecta_reuso() throws Exception {
        Sesion s1 = cuentaVerificada(emailNuevo());

        Sesion s2 = sesion(mvc.perform(post("/api/v1/auth/refresh").cookie(s1.refresh())).andExpect(status().isOk()));
        assertThat(s2.refresh().getValue()).isNotEqualTo(s1.refresh().getValue());

        // Reusar el refresh viejo invalida todas las sesiones, incluida la nueva
        mvc.perform(post("/api/v1/auth/refresh").cookie(s1.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(s2.refresh())).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test
    void logout_invalida_el_refresh() throws Exception {
        Sesion s = cuentaVerificada(emailNuevo());
        mvc.perform(post("/api/v1/auth/logout").cookie(s.refresh()))
                .andExpect(status().isNoContent()).andExpect(cookie().maxAge(AuthController.COOKIE_REFRESH, 0));
        mvc.perform(post("/api/v1/auth/refresh").cookie(s.refresh())).andExpect(status().isUnauthorized());
    }

    @Test
    void olvide_y_reset_de_password() throws Exception {
        String email = emailNuevo();
        Sesion s = cuentaVerificada(email);

        postJson("/api/v1/auth/olvide-password", """
                {"email":"%s"}""".formatted(email)).andExpect(status().isNoContent());
        postJson("/api/v1/auth/olvide-password", """
                {"email":"%s"}""".formatted(emailNuevo())).andExpect(status().isNoContent());

        postJson("/api/v1/auth/reset-password", """
                {"email":"%s","codigo":"%s","password":"clave-nueva-123"}""".formatted(email, emails.ultimoCodigo(email)))
                .andExpect(status().isNoContent());

        login(email, PASSWORD).andExpect(status().isUnauthorized());
        login(email, "clave-nueva-123").andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").cookie(s.refresh())).andExpect(status().isUnauthorized());
    }

    @Test
    void perfil_y_cambio_de_password() throws Exception {
        String email = emailNuevo();
        Sesion s = cuentaVerificada(email);
        String auth = "Bearer " + s.access();

        mvc.perform(patch("/api/v1/me").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana María","telefono":"099 123 456","mostrarContacto":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"))
                .andExpect(jsonPath("$.mostrarContacto").value(true));

        mvc.perform(post("/api/v1/me/cambiar-password").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actual":"mala-mala","nueva":"clave-nueva-123"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/me/cambiar-password").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"actual":"%s","nueva":"clave-nueva-123"}""".formatted(PASSWORD)))
                .andExpect(status().isNoContent());
        login(email, "clave-nueva-123").andExpect(status().isOk());
    }

    @Test
    void validacion_y_token_invalido() throws Exception {
        postJson("/api/v1/auth/registro", """
                {"email":"no-es-email","password":"corta","nombre":""}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("validacion"))
                .andExpect(jsonPath("$.errors.length()").value(3));
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer token.trucho.x")).andExpect(status().isUnauthorized());
    }

    @Test
    void usuario_comun_no_entra_a_admin() throws Exception {
        Sesion s = cuentaVerificada(emailNuevo());
        mvc.perform(get("/api/v1/admin/lotes").header("Authorization", "Bearer " + s.access())).andExpect(status().isForbidden());
    }

    // --- helpers ---

    private Sesion cuentaVerificada(String email) throws Exception {
        registrar(email).andExpect(status().isCreated());
        return sesion(postJson("/api/v1/auth/verificar-email", """
                {"email":"%s","codigo":"%s"}""".formatted(email, emails.ultimoCodigo(email))).andExpect(status().isOk()));
    }

    private ResultActions registrar(String email) throws Exception {
        return postJson("/api/v1/auth/registro", """
                {"email":"%s","password":"%s","nombre":"Ana"}""".formatted(email, PASSWORD));
    }

    private ResultActions login(String email, String password) throws Exception {
        return postJson("/api/v1/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, password));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Sesion sesion(ResultActions r) throws Exception {
        MvcResult res = r.andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        return new Sesion(body.get("accessToken").asText(), res.getResponse().getCookie(AuthController.COOKIE_REFRESH));
    }

    private static String emailNuevo() {
        return "prueba-" + UUID.randomUUID() + "@test.uy";
    }
}
