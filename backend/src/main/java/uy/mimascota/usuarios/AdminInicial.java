package uy.mimascota.usuarios;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uy.mimascota.config.AdminProperties;

/**
 * Crea el primer administrador al arrancar si ADMIN_EMAIL y ADMIN_PASSWORD están definidos
 * y ese email todavía no existe. Si el usuario ya existe no lo modifica.
 */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final AdminProperties props;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public AdminInicial(AdminProperties props, UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        this.props = props;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(props.email()) || !StringUtils.hasText(props.password())) {
            log.info("ADMIN_EMAIL o ADMIN_PASSWORD sin definir: no se crea administrador inicial");
            return;
        }
        if (usuarios.findByEmail(props.email()).isPresent()) {
            return;
        }
        Usuario admin = new Usuario(props.email(), passwordEncoder.encode(props.password()), "Administrador", Rol.ADMIN);
        admin.marcarEmailVerificado();
        usuarios.save(admin);
        log.info("Administrador inicial creado: {}", admin.getEmail());
    }
}
