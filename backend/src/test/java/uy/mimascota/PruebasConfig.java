package uy.mimascota;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uy.mimascota.auth.EmailsEnMemoria;

@TestConfiguration
public class PruebasConfig {

    @Bean
    @Primary
    EmailsEnMemoria emailsEnMemoria() {
        return new EmailsEnMemoria();
    }
}
