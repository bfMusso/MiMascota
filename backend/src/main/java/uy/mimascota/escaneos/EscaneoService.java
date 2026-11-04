package uy.mimascota.escaneos;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.mimascota.comun.ApiException;
import uy.mimascota.mascotas.CampoPublico;
import uy.mimascota.mascotas.Mascota;
import uy.mimascota.mascotas.MascotaRepository;
import uy.mimascota.mascotas.MascotaService;
import uy.mimascota.mascotas.MascotaUsuario;
import uy.mimascota.mascotas.MascotaUsuarioRepository;
import uy.mimascota.mascotas.RolMascota;
import uy.mimascota.placas.CodigoPlaca;
import uy.mimascota.placas.EstadoPlaca;
import uy.mimascota.placas.Placa;
import uy.mimascota.placas.PlacaRepository;
import uy.mimascota.publico.FichaPublica;
import uy.mimascota.usuarios.Usuario;
import uy.mimascota.usuarios.UsuarioRepository;

/** Ficha pública de una placa, registro de escaneos y aviso al dueño. */
@Service
public class EscaneoService {

    /** Si la mascota no está perdida y el escaneo no trae mensaje, máximo un email por hora por placa. */
    static final Duration ESPERA_ENTRE_AVISOS = Duration.ofHours(1);

    private final PlacaRepository placas;
    private final MascotaRepository mascotas;
    private final MascotaUsuarioRepository responsables;
    private final UsuarioRepository usuarios;
    private final EscaneoRepository escaneos;
    private final MascotaService mascotaService;
    private final AvisoEscaneo aviso;

    public EscaneoService(PlacaRepository placas, MascotaRepository mascotas, MascotaUsuarioRepository responsables,
                          UsuarioRepository usuarios, EscaneoRepository escaneos, MascotaService mascotaService,
                          AvisoEscaneo aviso) {
        this.placas = placas;
        this.mascotas = mascotas;
        this.responsables = responsables;
        this.usuarios = usuarios;
        this.escaneos = escaneos;
        this.mascotaService = mascotaService;
        this.aviso = aviso;
    }

    public record Datos(Double lat, Double lng, String mensaje, String contacto) {
    }

    public record EscaneoDto(UUID id, OffsetDateTime fecha, String placa, Double lat, Double lng, String mensaje,
                             String contacto) {
    }

    public record Pagina<T>(List<T> content, int page, int size, long totalElements) {
    }

    @Transactional(readOnly = true)
    public FichaPublica ficha(String codigoIngresado) {
        Placa placa = visible(codigoIngresado);
        if (placa.getEstado() == EstadoPlaca.SIN_ACTIVAR) {
            return new FichaPublica(placa.getCodigo(), placa.getEstado(), null);
        }
        Optional<Mascota> m = Optional.ofNullable(placa.getMascotaId()).flatMap(mascotas::findById);
        return new FichaPublica(placa.getCodigo(), placa.getEstado(), m.map(this::fichaMascota).orElse(null));
    }

    @Transactional
    public void registrar(String codigoIngresado, Datos d) {
        Placa placa = visible(codigoIngresado);
        if (placa.getEstado() != EstadoPlaca.ACTIVA) {
            throw noEncontrada();
        }
        Mascota mascota = placa.getMascotaId() == null ? null : mascotas.findById(placa.getMascotaId()).orElse(null);
        Escaneo e = new Escaneo(placa.getCodigo(), d.lat(), d.lng(), d.mensaje(), d.contacto());

        boolean urgente = (mascota != null && mascota.isPerdida()) || e.getMensaje() != null || e.getContacto() != null;
        boolean avisar = urgente || !escaneos.existsByPlacaCodigoAndNotificadoTrueAndFechaAfter(
                placa.getCodigo(), OffsetDateTime.now().minus(ESPERA_ENTRE_AVISOS));
        if (avisar) {
            e.marcarNotificado();
        }
        escaneos.save(e);
        if (avisar) {
            duenos(placa, mascota).forEach(u -> aviso.enviar(u, e, mascota));
        }
    }

    @Transactional(readOnly = true)
    public Pagina<EscaneoDto> deMascota(UUID usuarioId, UUID mascotaId, int page, int size) {
        mascotaService.propia(usuarioId, mascotaId);
        Page<Escaneo> p = escaneos.deMascota(mascotaId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return new Pagina<>(p.getContent().stream()
                .map(e -> new EscaneoDto(e.getId(), e.getFecha(), e.getPlacaCodigo(), e.getLat(), e.getLng(),
                        e.getMensaje(), e.getContacto()))
                .toList(), p.getNumber(), p.getSize(), p.getTotalElements());
    }

    private FichaPublica.Mascota fichaMascota(Mascota m) {
        Set<CampoPublico> c = m.getCamposPublicos();
        FichaPublica.Contacto contacto = null;
        if (c.contains(CampoPublico.CONTACTO)) {
            contacto = titular(m.getId())
                    .filter(u -> u.getTelefono() != null)
                    .map(u -> new FichaPublica.Contacto(u.getNombre().trim().split("\\s+")[0], u.getTelefono()))
                    .orElse(null);
        }
        boolean sexo = c.contains(CampoPublico.SEXO);
        return new FichaPublica.Mascota(
                m.getNombre(),
                m.getEspecie(),
                m.isPerdida(),
                c.contains(CampoPublico.FOTO) && m.getFoto() != null ? "/fotos/" + m.getFoto() : null,
                c.contains(CampoPublico.RAZA) ? m.getRaza() : null,
                c.contains(CampoPublico.COLOR) ? m.getColor() : null,
                sexo ? m.getSexo() : null,
                sexo ? m.getCastrada() : null,
                c.contains(CampoPublico.EDAD) && m.getFechaNacimiento() != null ? meses(m.getFechaNacimiento()) : null,
                c.contains(CampoPublico.INFO_MEDICA) ? m.getInfoMedica() : null,
                c.contains(CampoPublico.OBSERVACIONES) ? m.getObservaciones() : null,
                contacto);
    }

    private Optional<Usuario> titular(UUID mascotaId) {
        return responsables.findByMascotaId(mascotaId).stream()
                .filter(r -> r.getRol() == RolMascota.TITULAR)
                .findFirst()
                .flatMap(r -> usuarios.findById(r.getUsuarioId()));
    }

    /** A quién avisar: los responsables de la mascota, o el dueño de la placa si está libre. */
    private List<Usuario> duenos(Placa placa, Mascota mascota) {
        List<UUID> ids = mascota != null
                ? responsables.findByMascotaId(mascota.getId()).stream().map(MascotaUsuario::getUsuarioId).toList()
                : placa.getUsuarioId() == null ? List.of() : List.of(placa.getUsuarioId());
        return usuarios.findAllById(ids);
    }

    private static int meses(LocalDate nacimiento) {
        Period p = Period.between(nacimiento, LocalDate.now());
        return Math.max(0, p.getYears() * 12 + p.getMonths());
    }

    private Placa visible(String codigo) {
        return placas.findById(CodigoPlaca.normalizar(codigo))
                .filter(p -> p.getEstado() != EstadoPlaca.BLOQUEADA)
                .orElseThrow(EscaneoService::noEncontrada);
    }

    private static ApiException noEncontrada() {
        return new ApiException(HttpStatus.NOT_FOUND, "placa_no_encontrada", "No encontramos esta placa");
    }
}
