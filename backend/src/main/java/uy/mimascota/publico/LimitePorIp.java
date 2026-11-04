package uy.mimascota.publico;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uy.mimascota.comun.ApiException;

/**
 * Límite simple de pedidos por IP en memoria, para las páginas públicas.
 * Alcanza para un solo servidor; si algún día hay varios, pasarlo a la base o a Redis.
 */
@Component
public class LimitePorIp {

    private final Map<String, Deque<Long>> pedidos = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LimitePorIp() {
        this(Clock.systemUTC());
    }

    LimitePorIp(Clock reloj) {
        this.reloj = reloj;
    }

    /** Corta con 429 si la clave ya hizo {@code maximo} pedidos dentro de la {@code ventana}. */
    public void controlar(String clave, int maximo, Duration ventana) {
        long ahora = reloj.millis();
        long desde = ahora - ventana.toMillis();
        Deque<Long> marcas = pedidos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && marcas.peekFirst() < desde) {
                marcas.pollFirst();
            }
            if (marcas.size() >= maximo) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "demasiadas_solicitudes",
                        "Hiciste muchos pedidos seguidos. Esperá un rato y probá de nuevo.");
            }
            marcas.addLast(ahora);
        }
        if (pedidos.size() > 50_000) {
            pedidos.entrySet().removeIf(e -> e.getValue().isEmpty() || e.getValue().peekLast() < desde);
        }
    }

    /** IP del visitante. Detrás de Nginx viene en X-Real-IP. */
    public static String ip(HttpServletRequest req) {
        String real = req.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real.trim() : req.getRemoteAddr();
    }

    void limpiar() {
        pedidos.clear();
    }
}
