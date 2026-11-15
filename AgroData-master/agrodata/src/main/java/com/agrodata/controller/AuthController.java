package com.agrodata.controller;

import com.agrodata.dto.*;
import com.agrodata.model.Role;
import com.agrodata.model.Usuario;
import com.agrodata.repository.UsuarioRepository;
import com.agrodata.security.JwtService;
import com.agrodata.security.LoginAttemptService;
import com.agrodata.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService tentativas;
    private final UsuarioService usuarioService;
    /** Hash descartável para gastar o mesmo tempo quando o usuário não existe (evita enumeração por timing). */
    private final String hashFalso;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder,
                          JwtService jwt, LoginAttemptService tentativas, UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.tentativas = tentativas;
        this.hashFalso = encoder.encode("senha-descartavel-nao-usada");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String username = dto.getUsername().trim();

        if (tentativas.estaBloqueado(ip, username)) {
            return ResponseEntity.status(429)
                    .body(Map.of("message", "Muitas tentativas. Tente novamente em alguns minutos."));
        }

        Usuario usuario = usuarios.findByUsername(username).orElse(null);
        boolean ok = encoder.matches(dto.getPassword(), usuario != null ? usuario.getSenhaHash() : hashFalso)
                && usuario != null && usuario.isAtivo();

        if (!ok) {
            tentativas.registrarFalha(ip, username);
            // Mensagem genérica: não revela se o usuário existe
            return ResponseEntity.status(401).body(Map.of("message", "Usuário ou senha inválidos."));
        }

        tentativas.registrarSucesso(ip, username);
        return ResponseEntity.ok(new LoginResponseDTO(
                jwt.gerarToken(usuario), "Bearer", jwt.getExpiracaoSegundos(),
                usuario.getUsername(), usuario.getRole().name(), usuario.isSenhaTemporaria()));
    }

    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody CadastroUsuarioDTO dto) {
        String username = dto.getUsername().trim();
        if (usuarios.existsByUsername(username)) {
            return ResponseEntity.status(409).body(Map.of("message", "Nome de usuário já existe."));
        }

        Usuario novo = new Usuario(username, encoder.encode(dto.getPassword()), Role.PRODUTOR);
        usuarioService.definirCpf(novo, dto.getCpf());
        usuarioService.aplicarDados(novo, dto);
        novo = usuarios.save(novo);
        return ResponseEntity.status(201)
                .body(new UsuarioResponseDTO(novo.getIdUsuario(), novo.getUsername(), novo.getRole().name()));
    }

    /** Somente ADMIN (regra em SecurityConfig). */
    @PostMapping("/usuarios")
    public ResponseEntity<?> criarUsuario(@Valid @RequestBody UsuarioRequestDTO dto) {
        String username = dto.getUsername().trim();
        if (usuarios.existsByUsername(username)) {
            return ResponseEntity.status(409).body(Map.of("message", "Nome de usuário já existe."));
        }
        Usuario novo = new Usuario(username, encoder.encode(dto.getPassword()), dto.getRole());
        usuarioService.aplicarDados(novo, dto);
        novo = usuarios.save(novo);
        return ResponseEntity.status(201)
                .body(new UsuarioResponseDTO(novo.getIdUsuario(), novo.getUsername(), novo.getRole().name()));
    }
}
