package uy.mimascota.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reemplaza el envío SMTP en las pruebas y guarda los emails para leer los códigos. */
public class EmailsEnMemoria implements EnviadorEmail {

    public record Email(String para, String asunto, String texto) {
    }

    private final List<Email> enviados = new ArrayList<>();

    @Override
    public synchronized void enviar(String para, String asunto, String texto) {
        enviados.add(new Email(para, asunto, texto));
    }

    public synchronized List<Email> para(String email) {
        return enviados.stream().filter(e -> e.para().equalsIgnoreCase(email)).toList();
    }

    public synchronized String ultimoCodigo(String email) {
        List<Email> lista = para(email);
        Matcher m = Pattern.compile("\\b(\\d{6})\\b").matcher(lista.get(lista.size() - 1).texto());
        if (!m.find()) {
            throw new IllegalStateException("El email no tiene código");
        }
        return m.group(1);
    }
}
