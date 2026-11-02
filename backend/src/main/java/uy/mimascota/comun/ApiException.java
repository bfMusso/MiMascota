package uy.mimascota.comun;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio que se devuelve al cliente como Problem Details.
 * {@code codigo} es un identificador estable para que la web y la app distingan casos.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String detalle) {
        super(detalle);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
