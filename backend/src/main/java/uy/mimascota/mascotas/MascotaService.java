package uy.mimascota.mascotas;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uy.mimascota.comun.ApiException;
import uy.mimascota.fotos.FotoService;
import uy.mimascota.mascotas.MascotaDtos.MascotaDto;
import uy.mimascota.mascotas.MascotaDtos.MascotaRequest;
import uy.mimascota.placas.Placa;
import uy.mimascota.placas.PlacaRepository;

/** Mascotas del usuario. Una mascota ajena responde 404, igual que una que no existe. */
@Service
public class MascotaService {

    private final MascotaRepository mascotas;
    private final MascotaUsuarioRepository responsables;
    private final PlacaRepository placas;
    private final FotoService fotos;

    public MascotaService(MascotaRepository mascotas, MascotaUsuarioRepository responsables, PlacaRepository placas,
                          FotoService fotos) {
        this.mascotas = mascotas;
        this.responsables = responsables;
        this.placas = placas;
        this.fotos = fotos;
    }

    @Transactional(readOnly = true)
    public List<MascotaDto> listar(UUID usuarioId) {
        List<Mascota> lista = mascotas.deUsuario(usuarioId);
        Map<UUID, List<String>> placasPorMascota = placas
                .findByMascotaIdInOrderByFechaAlta(lista.stream().map(Mascota::getId).toList()).stream()
                .collect(Collectors.groupingBy(Placa::getMascotaId,
                        Collectors.mapping(Placa::getCodigo, Collectors.toList())));
        return lista.stream()
                .map(m -> MascotaDto.de(m, rol(m.getId(), usuarioId), placasPorMascota.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    @Transactional
    public Mascota crear(UUID usuarioId, MascotaRequest req) {
        Mascota m = new Mascota(req.nombre(), req.especie());
        aplicar(m, req);
        mascotas.save(m);
        responsables.save(new MascotaUsuario(m.getId(), usuarioId, RolMascota.TITULAR));
        return m;
    }

    @Transactional(readOnly = true)
    public MascotaDto ver(UUID usuarioId, UUID mascotaId) {
        return dto(propia(usuarioId, mascotaId), usuarioId);
    }

    @Transactional
    public MascotaDto actualizar(UUID usuarioId, UUID mascotaId, MascotaRequest req) {
        Mascota m = propia(usuarioId, mascotaId);
        aplicar(m, req);
        return dto(m, usuarioId);
    }

    /** Solo el titular puede borrarla. Sus placas siguen siendo del usuario, libres para otra mascota. */
    @Transactional
    public void borrar(UUID usuarioId, UUID mascotaId) {
        Mascota m = propia(usuarioId, mascotaId);
        if (rol(mascotaId, usuarioId) != RolMascota.TITULAR) {
            throw new ApiException(HttpStatus.FORBIDDEN, "solo_titular", "Solo el titular puede borrar la mascota");
        }
        placas.liberarDeMascota(mascotaId);
        String foto = m.getFoto();
        mascotas.delete(m);
        fotos.borrar(foto);
    }

    @Transactional
    public MascotaDto cambiarFoto(UUID usuarioId, UUID mascotaId, MultipartFile archivo) {
        Mascota m = propia(usuarioId, mascotaId);
        String anterior = m.getFoto();
        m.setFoto(fotos.guardar(archivo));
        fotos.borrar(anterior);
        return dto(m, usuarioId);
    }

    @Transactional
    public MascotaDto quitarFoto(UUID usuarioId, UUID mascotaId) {
        Mascota m = propia(usuarioId, mascotaId);
        fotos.borrar(m.getFoto());
        m.setFoto(null);
        return dto(m, usuarioId);
    }

    /** La mascota si el usuario es responsable; si no, 404. */
    public Mascota propia(UUID usuarioId, UUID mascotaId) {
        if (responsables.findByMascotaIdAndUsuarioId(mascotaId, usuarioId).isEmpty()) {
            throw noEncontrada();
        }
        return mascotas.findById(mascotaId).orElseThrow(MascotaService::noEncontrada);
    }

    public MascotaDto dto(Mascota m, UUID usuarioId) {
        List<String> codigos = placas.findByMascotaIdInOrderByFechaAlta(List.of(m.getId())).stream()
                .map(Placa::getCodigo).toList();
        return MascotaDto.de(m, rol(m.getId(), usuarioId), codigos);
    }

    private RolMascota rol(UUID mascotaId, UUID usuarioId) {
        return responsables.findByMascotaIdAndUsuarioId(mascotaId, usuarioId).map(MascotaUsuario::getRol).orElse(null);
    }

    private static void aplicar(Mascota m, MascotaRequest req) {
        m.actualizar(req.nombre(), req.especie(), req.raza(), req.sexo(), req.fechaNacimiento(), req.color(),
                req.infoMedica(), req.observaciones());
        if (req.perdida() != null) {
            m.setPerdida(req.perdida());
        }
        if (req.camposPublicos() != null) {
            m.setCamposPublicos(req.camposPublicos());
        }
    }

    private static ApiException noEncontrada() {
        return new ApiException(HttpStatus.NOT_FOUND, "mascota_no_encontrada", "No encontramos esa mascota");
    }
}
