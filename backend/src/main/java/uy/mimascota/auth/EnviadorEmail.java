package uy.mimascota.auth;

/** Envío de emails transaccionales. En pruebas se reemplaza por una implementación en memoria. */
public interface EnviadorEmail {

    void enviar(String para, String asunto, String texto);
}
