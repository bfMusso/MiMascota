package uy.mimascota.placas;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uy.mimascota.comun.ApiException;
import uy.mimascota.mascotas.Mascota;
import uy.mimascota.mascotas.MascotaRepository;
import uy.mimascota.mascotas.MascotaService;
import uy.mimascota.placas.PlacaDtos.ActivarRequest;
import uy.mimascota.placas.PlacaDtos.PlacaDto;

/** Placas del dueño: activarlas con el código impreso y asignarlas a sus mascotas. */
@Service
public class PlacaService {

    private final PlacaRepository placas;
    private final MascotaService mascotaService;
    private final MascotaRepository mascotas;

    public PlacaService(PlacaRepository placas, MascotaService mascotaService, MascotaRepository mascotas) {
        this.placas = placas;
        this.mascotaService = mascotaService;
        this.mascotas = mascotas;
    }

    @Transactional
    public PlacaDto activar(UUID usuarioId, String codigoIngresado, ActivarRequest req) {
        if ((req.mascotaId() == null) == (req.mascota() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "validacion",
                    "Elegí una de tus mascotas o cargá una nueva");
        }
        Placa placa = placas.findById(CodigoPlaca.normalizar(codigoIngresado))
                .filter(p -> p.getEstado() != EstadoPlaca.BLOQUEADA)
                .orElseThrow(PlacaService::noEncontrada);
        if (placa.getEstado() == EstadoPlaca.ACTIVA) {
            throw new ApiException(HttpStatus.CONFLICT, "placa_ya_activa", "Esa placa ya está activada");
        }
        Mascota mascota = req.mascotaId() != null
                ? mascotaService.propia(usuarioId, req.mascotaId())
                : mascotaService.crear(usuarioId, req.mascota());
        placa.activar(usuarioId, mascota.getId());
        return dto(placa, mascota.getNombre());
    }

    @Transactional(readOnly = true)
    public List<PlacaDto> mias(UUID usuarioId) {
        List<Placa> lista = placas.findByUsuarioIdOrderByFechaAlta(usuarioId);
        Map<UUID, String> nombres = mascotas.findAllById(lista.stream().map(Placa::getMascotaId)
                        .filter(java.util.Objects::nonNull).toList()).stream()
                .collect(Collectors.toMap(Mascota::getId, Mascota::getNombre, (a, b) -> a));
        return lista.stream().map(p -> dto(p, nombres.get(p.getMascotaId()))).toList();
    }

    /** Pasa la placa a otra mascota propia, o la libera si {@code mascotaId} es null. */
    @Transactional
    public PlacaDto asignar(UUID usuarioId, String codigo, UUID mascotaId) {
        Placa placa = placas.findById(CodigoPlaca.normalizar(codigo))
                .filter(p -> usuarioId.equals(p.getUsuarioId()))
                .orElseThrow(PlacaService::noEncontrada);
        String nombre = null;
        if (mascotaId != null) {
            nombre = mascotaService.propia(usuarioId, mascotaId).getNombre();
        }
        placa.asignarMascota(mascotaId);
        return dto(placa, nombre);
    }

    private static PlacaDto dto(Placa p, String mascotaNombre) {
        return new PlacaDto(p.getCodigo(), p.getEstado(), p.getMascotaId(), mascotaNombre, p.getFechaAlta());
    }

    static ApiException noEncontrada() {
        return new ApiException(HttpStatus.NOT_FOUND, "placa_no_encontrada",
                "No encontramos una placa con ese código. Revisá que esté bien escrito.");
    }
}
