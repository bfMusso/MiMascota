package uy.mimascota.placas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uy.mimascota.comun.ApiException;
import uy.mimascota.config.WebProperties;

/** Admin: genera lotes de códigos para imprimir placas y los exporta en CSV. */
@RestController
@RequestMapping("/api/v1/admin/lotes")
public class LoteController {

    private static final DateTimeFormatter NOMBRE_AUTO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final PlacaRepository placas;
    private final WebProperties web;

    public LoteController(PlacaRepository placas, WebProperties web) {
        this.placas = placas;
        this.web = web;
    }

    public record NuevoLoteRequest(
            @Min(1) @Max(500) int cantidad,
            @Pattern(regexp = "[A-Za-z0-9_-]{1,40}", message = "solo letras, números, - y _ (hasta 40)") String nombre) {
    }

    public record LoteCreado(String lote, int cantidad, List<String> codigos) {
    }

    public record PlacaLoteDto(String codigo, EstadoPlaca estado, String url, boolean conMascota) {
    }

    public record LoteDto(String lote, java.time.OffsetDateTime fechaAlta, long total, long sinActivar, long activas,
                          long bloqueadas) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public LoteCreado crear(@Valid @RequestBody NuevoLoteRequest req) {
        boolean automatico = req.nombre() == null || req.nombre().isBlank();
        String lote = automatico ? nombreAutomatico() : req.nombre().trim();
        if (!automatico && placas.existsByLote(lote)) {
            throw new ApiException(HttpStatus.CONFLICT, "lote_existente", "Ya hay un lote con ese nombre");
        }
        Set<String> nuevos = new HashSet<>();
        while (nuevos.size() < req.cantidad()) {
            String c = CodigoPlaca.nuevo();
            if (!placas.existsById(c)) {
                nuevos.add(c);
            }
        }
        List<String> codigos = new ArrayList<>(nuevos);
        codigos.sort(null);
        placas.saveAll(codigos.stream().map(c -> new Placa(c, lote)).toList());
        return new LoteCreado(lote, codigos.size(), codigos);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<LoteDto> listar() {
        return placas.resumenLotes().stream()
                .map(r -> new LoteDto(r.getLote(), r.getFechaAlta(), r.getTotal(), r.getSinActivar(), r.getActivas(),
                        r.getBloqueadas()))
                .toList();
    }

    /** Placas del lote, para verlas en pantalla (un lote tiene como máximo 500). */
    @GetMapping("/{lote}/placas")
    @Transactional(readOnly = true)
    public List<PlacaLoteDto> placasDelLote(@PathVariable String lote) {
        return delLote(lote).stream()
                .map(p -> new PlacaLoteDto(p.getCodigo(), p.getEstado(), url(p), p.getMascotaId() != null))
                .toList();
    }

    /** PDF para imprimir: un QR por placa con su código. */
    @GetMapping("/{lote}/export.pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportarPdf(@PathVariable String lote) {
        byte[] pdf = LotePdf.generar(lote, delLote(lote), web.url());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + lote + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** CSV con el código y la URL de cada placa, para generar los QR y grabar los NFC. */
    @GetMapping("/{lote}/export")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportar(@PathVariable String lote) {
        List<Placa> lista = delLote(lote);
        StringBuilder csv = new StringBuilder("codigo,url,estado\n");
        lista.forEach(p -> csv.append(p.getCodigo()).append(',').append(url(p))
                .append(',').append(p.getEstado()).append('\n'));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + lote + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** LOTE-fecha-hora; si ya hay uno creado en el mismo segundo, le agrega -2, -3... */
    private String nombreAutomatico() {
        String base = "LOTE-" + LocalDateTime.now().format(NOMBRE_AUTO);
        String nombre = base;
        for (int i = 2; placas.existsByLote(nombre); i++) {
            nombre = base + "-" + i;
        }
        return nombre;
    }

    private List<Placa> delLote(String lote) {
        List<Placa> lista = placas.findByLoteOrderByCodigo(lote);
        if (lista.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "lote_no_encontrado", "No existe ese lote");
        }
        return lista;
    }

    private String url(Placa p) {
        return web.url() + "/p/" + p.getCodigo();
    }
}
