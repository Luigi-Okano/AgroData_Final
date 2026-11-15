package com.agrodata.controller;

import com.agrodata.dto.CadastroUsuarioDTO;
import com.agrodata.dto.UsuarioResponseDTO;
import com.agrodata.model.Usuario;
import com.agrodata.repository.UsuarioRepository;
import com.agrodata.security.JwtService;
import com.agrodata.security.LoginAttemptService;
import com.agrodata.service.UsuarioService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

class AuthControllerRegistrationTest {

    private UsuarioRepository usuarios;
    private PasswordEncoder encoder;
    private UsuarioService usuarioService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        usuarios = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        usuarioService = mock(UsuarioService.class);
        controller = new AuthController(
                usuarios, encoder, mock(JwtService.class), mock(LoginAttemptService.class), usuarioService);
    }

    @Test
    void cadastroPublicoSempreCriaProdutor() {
        CadastroUsuarioDTO dto = cadastroCompleto();
        when(usuarios.existsByUsername("novo.produtor")).thenReturn(false);
        when(encoder.encode("SenhaSegura123!")).thenReturn("hash");
        when(usuarios.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = controller.cadastrar(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        UsuarioResponseDTO body = (UsuarioResponseDTO) response.getBody();
        assertEquals("novo.produtor", body.username());
        assertEquals("PRODUTOR", body.role());
        verify(usuarioService).aplicarDados(any(Usuario.class), same(dto));
    }

    @Test
    void cadastroPublicoRecusaUsernameExistente() {
        CadastroUsuarioDTO dto = new CadastroUsuarioDTO();
        dto.setUsername("existente");
        dto.setPassword("SenhaSegura123!");
        when(usuarios.existsByUsername("existente")).thenReturn(true);

        var response = controller.cadastrar(dto);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(usuarios, never()).save(any(Usuario.class));
    }

    @Test
    void cadastroExigeDadosPessoaisDoProdutor() {
        CadastroUsuarioDTO dto = cadastroCompleto();

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(dto).isEmpty());

            dto.setPassword("SenhaSegura123");
            assertTrue(validator.validate(dto).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")));
            dto.setPassword("senhasegura123!");
            assertTrue(validator.validate(dto).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")));
            dto.setPassword("SenhaSegura!");
            assertTrue(validator.validate(dto).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")));
            dto.setPassword("Senha1!a");
            assertTrue(validator.validate(dto).isEmpty());
            dto.setPassword("Senha1!");
            assertTrue(validator.validate(dto).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")));
            dto.setPassword("SenhaSegura123!");
            assertTrue(validator.validate(dto).isEmpty());

            dto.setEmail("ana@example.COM.BR");
            assertTrue(validator.validate(dto).isEmpty());

            dto.setEmail("ana@example.org");
            assertTrue(validator.validate(dto).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));

            dto.setNomeCompleto("");
            dto.setCpf("");
            dto.setEmail("");
            dto.setTelefone("");

            Set<String> invalidFields = validator.validate(dto).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .collect(Collectors.toSet());
            assertTrue(invalidFields.containsAll(Set.of("nomeCompleto", "cpf", "email", "telefone")));
        }
    }

    private static CadastroUsuarioDTO cadastroCompleto() {
        CadastroUsuarioDTO dto = new CadastroUsuarioDTO();
        dto.setUsername("novo.produtor");
        dto.setPassword("SenhaSegura123!");
        dto.setNomeCompleto("Ana Produtora");
        dto.setCpf("529.982.247-25");
        dto.setEmail("ana@example.com");
        dto.setTelefone("(11) 99999-9999");
        return dto;
    }
}
