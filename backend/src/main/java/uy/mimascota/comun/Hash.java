package uy.mimascota.comun;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Utilidades para tokens aleatorios. Los tokens se guardan solo como hash SHA-256. */
public final class Hash {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Hash() {
    }

    public static String tokenAleatorio() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String codigoNumerico(int digitos) {
        int max = (int) Math.pow(10, digitos);
        return String.format("%0" + digitos + "d", RANDOM.nextInt(max));
    }

    public static String sha256(String valor) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
