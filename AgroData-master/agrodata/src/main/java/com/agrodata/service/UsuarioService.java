package com.agrodata.service;

import com.agrodata.dto.*;
import com.agrodata.model.Role;
import com.agrodata.model.Produtor;
import com.agrodata.model.Usuario;
import com.agrodata.repository.ProdutorRepository;
import com.agrodata.repository.UsuarioRepository;
import com.agrodata.security.CpfFingerprintService;
import com.agrodata.util.Cpf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final ProdutorRepository produtorRepository;
    private final PasswordEncoder encoder;
    private final CpfFingerprintService cpfFingerprintService;

    public UsuarioService(UsuarioRepository repository, ProdutorRepository produtorRepository,
                          PasswordEncoder encoder, CpfFingerprintService cpfFingerprintService) {
        this.repository = repository;
        this.produtorRepository = produtorRepository;
        this.encoder = encoder;
        this.cpfFingerprintService = cpfFingerprintService;
    }

    public Usuario porUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    public Usuario porId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    /** Aplica somente os campos preenchidos, validando cada um. */
    public void aplicarDados(Usuario u, PerfilUpdateDTO d) {
        String cpfAnterior = Cpf.somenteDigitos(u.getCpf());
        Produtor produtor = cpfAnterior.length() == 11
                ? produtorRepository.findByCpf(cpfAnterior).orElse(null)
                : null;
        if (temTexto(d.getNomeCompleto())) {
            String nome = d.getNomeCompleto().trim();
            if (nome.length() < 3) throw invalido("Nome muito curto.");
            u.setNomeCompleto(nome);
        }
        if (temTexto(d.getEmail())) {
            u.setEmail(d.getEmail().trim().toLowerCase());
        }
        if (temTexto(d.getTelefone())) {
            int digitos = Cpf.somenteDigitos(d.getTelefone()).length();
            if (digitos < 10 || digitos > 13) throw invalido("Telefone inválido (informe DDD + número).");
            u.setTelefone(d.getTelefone().trim());
        }
        if (produtor != null) {
            produtor.setCpf(Cpf.somenteDigitos(u.getCpf()));
            if (temTexto(u.getNomeCompleto())) produtor.setNome(u.getNomeCompleto().trim());
            produtorRepository.save(produtor);
        }
    }

    @Transactional
    public Usuario atualizarMeusDados(String username, PerfilUpdateDTO dto) {
        Usuario u = porUsername(username);
        aplicarDados(u, dto);
        return repository.save(u);
    }

    @Transactional
    public Usuario cadastrarCpf(String username, String cpf) {
        Usuario u = porUsername(username);
        if (temTexto(u.getCpf())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado.");
        }
        definirCpf(u, cpf);
        aplicarDados(u, new PerfilUpdateDTO());
        return repository.save(u);
    }

    public void definirCpf(Usuario usuario, String cpf) {
        String cpfNumerico = Cpf.somenteDigitos(cpf);
        if (cpfNumerico.length() != 11) throw invalido("Informe 11 dígitos para o CPF.");
        String fingerprint = cpfFingerprintService.gerar(cpfNumerico);
        boolean cpfEmUso = repository.existsByCpfFingerprint(fingerprint);
        if (!cpfEmUso) {
            for (Usuario outro : repository.findAllByCpfFingerprintIsNull()) {
                boolean mesmaConta = usuario.getIdUsuario() != null
                        && Objects.equals(outro.getIdUsuario(), usuario.getIdUsuario());
                if (!mesmaConta && temTexto(outro.getCpf())
                        && cpfNumerico.equals(Cpf.somenteDigitos(outro.getCpf()))) {
                    cpfEmUso = true;
                    break;
                }
            }
        }
        if (cpfEmUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado em outra conta.");
        }

        usuario.setCpf(cpfNumerico);
        usuario.setCpfFingerprint(fingerprint);
    }

    @Transactional
    public void trocarSenha(String username, SenhaRequestDTO dto) {
        Usuario u = porUsername(username);
        if (!encoder.matches(dto.getSenhaAtual(), u.getSenhaHash())) {
            throw invalido("Senha atual incorreta.");
        }
        if (dto.getSenhaAtual().equals(dto.getNovaSenha())) {
            throw invalido("A nova senha deve ser diferente da atual.");
        }
        u.setSenhaHash(encoder.encode(dto.getNovaSenha()));
        u.setSenhaTemporaria(false);
        repository.save(u);
    }

    // ---------- Administrador ----------

    public List<UsuarioDetalheDTO> listarTodos(String admin) {
        log.info("AUDITORIA: admin '{}' consultou os dados pessoais de todos os usuários", admin);
        return repository.findAll().stream().map(UsuarioDetalheDTO::de).toList();
    }

    public UsuarioDetalheDTO detalhar(Long id, String admin) {
        Usuario u = porId(id);
        log.info("AUDITORIA: admin '{}' consultou os dados pessoais do usuário '{}'", admin, u.getUsername());
        return UsuarioDetalheDTO.de(u);
    }

    @Transactional
    public UsuarioDetalheDTO atualizarComoAdmin(Long id, AdminUsuarioUpdateDTO dto, String admin) {
        Usuario u = porId(id);
        boolean proprioUsuario = u.getUsername().equals(admin);

        if (proprioUsuario && (Boolean.FALSE.equals(dto.getAtivo())
                || (dto.getRole() != null && dto.getRole() != Role.ADMIN))) {
            throw invalido("Você não pode desativar nem rebaixar a sua própria conta de administrador.");
        }
        aplicarDados(u, dto);
        if (dto.getRole() != null) u.setRole(dto.getRole());
        if (dto.getAtivo() != null) u.setAtivo(dto.getAtivo());
        if (temTexto(dto.getNovaSenha())) {
            if (dto.getNovaSenha().length() < 10) throw invalido("A senha deve ter entre 10 e 72 caracteres.");
            u.setSenhaHash(encoder.encode(dto.getNovaSenha()));
            // senha definida por outra pessoa => provisória (o dono precisa trocar no próximo acesso)
            u.setSenhaTemporaria(!proprioUsuario);
        }
        repository.save(u);
        log.info("AUDITORIA: admin '{}' alterou o usuário '{}'", admin, u.getUsername());
        return UsuarioDetalheDTO.de(u);
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static ResponseStatusException invalido(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}
