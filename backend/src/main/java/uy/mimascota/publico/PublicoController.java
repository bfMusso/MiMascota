package uy.mimascota.publico;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uy.mimascota.escaneos.EscaneoService;
import uy.mimascota.placas.CodigoPlaca;

/** Lo que se abre al escanear la placa: sin sesión, con límite de pedidos por IP. */
@RestController
@RequestMapping("/api/v1/public/placas")
public class PublicoController {

    private final EscaneoService escaneos;
    private final LimitePorIp limite;

    public PublicoController(EscaneoService escaneos, LimitePorIp limite) {
        this.escaneos = escaneos;
        this.limite = limite;
    }

    public record EscaneoRequest(
            @DecimalMin("-90") @DecimalMax("90") Double lat,
            @DecimalMin("-180") @DecimalMax("180") Double lng,
            @Size(max = 500) String mensaje,
            @Size(max = 100) String contacto) {
    }

    @GetMapping("/{codigo}")
    public FichaPublica ver(@PathVariable String codigo, HttpServletRequest req) {
        limite.controlar("ver:" + LimitePorIp.ip(req), 60, Duration.ofMinutes(1));
        return escaneos.ficha(codigo);
    }

    @PostMapping("/{codigo}/escaneos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void escanear(@PathVariable String codigo, @Valid @RequestBody(required = false) EscaneoRequest body,
                         HttpServletRequest req) {
        String ip = LimitePorIp.ip(req);
        limite.controlar("escaneo:" + ip, 20, Duration.ofHours(1));
        limite.controlar("escaneo:" + ip + ":" + CodigoPlaca.normalizar(codigo), 5, Duration.ofMinutes(10));
        EscaneoRequest b = body == null ? new EscaneoRequest(null, null, null, null) : body;
        escaneos.registrar(codigo, new EscaneoService.Datos(b.lat(), b.lng(), b.mensaje(), b.contacto()));
    }
}
