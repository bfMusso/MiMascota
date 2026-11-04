package uy.mimascota.placas;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Códigos de placa: 8 caracteres de un alfabeto sin los que se confunden al leerlos
 * (sin 0/O ni 1/I/L). 31^8 ≈ 8,5 × 10^11 combinaciones.
 */
public final class CodigoPlaca {

    static final String ALFABETO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    static final int LARGO = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodigoPlaca() {
    }

    public static String nuevo() {
        StringBuilder sb = new StringBuilder(LARGO);
        for (int i = 0; i < LARGO; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    /** Normaliza lo que escribe el usuario: mayúsculas, sin espacios ni guiones. */
    public static String normalizar(String codigo) {
        return codigo == null ? "" : codigo.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
