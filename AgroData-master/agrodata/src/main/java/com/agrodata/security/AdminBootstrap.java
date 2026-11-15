package com.agrodata.security;

import com.agrodata.model.Role;
import com.agrodata.model.Usuario;
import com.agrodata.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Cria o primeiro ADMIN a partir de variáveis de ambiente, somente se não existir nenhum usuário. */
@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final String username;
    private final String password;

    public AdminBootstrap(UsuarioRepository repository, PasswordEncoder encoder,
                          @Value("${agrodata.security.admin.username:}") String username,
                          @Value("${agrodata.security.admin.password:}") String password) {
        this.repository = repository;
        this.encoder = encoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;
        if (username.isBlank() || password.length() < 10) {
            log.warn("Nenhum usuário cadastrado. Defina ADMIN_USERNAME e ADMIN_PASSWORD (mín. 10 caracteres) para criar o primeiro ADMIN.");
            return;
        }
        repository.save(new Usuario(username.trim(), encoder.encode(password), Role.ADMIN));
        log.info("Usuário ADMIN inicial '{}' criado.", username.trim());
    }
}
