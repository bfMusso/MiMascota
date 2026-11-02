package uy.mimascota.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpEnviadorEmail implements EnviadorEmail {

    private static final Logger log = LoggerFactory.getLogger(SmtpEnviadorEmail.class);

    private final JavaMailSender mailSender;
    private final String remitente;

    public SmtpEnviadorEmail(JavaMailSender mailSender, @Value("${mimascota.mail.from}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String para, String asunto, String texto) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remitente);
        msg.setTo(para);
        msg.setSubject(asunto);
        msg.setText(texto);
        try {
            mailSender.send(msg);
        } catch (MailException e) {
            // No cortamos el flujo: el usuario puede pedir el reenvío del código.
            log.error("No se pudo enviar el email a {}: {}", para, e.getMessage());
        }
    }
}
