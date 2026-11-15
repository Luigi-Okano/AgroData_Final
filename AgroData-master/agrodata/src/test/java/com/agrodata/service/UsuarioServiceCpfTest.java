package com.agrodata.service;

import com.agrodata.dto.PerfilUpdateDTO;
import com.agrodata.model.Produtor;
import com.agrodata.model.Role;
import com.agrodata.model.Usuario;
import com.agrodata.repository.ProdutorRepository;
import com.agrodata.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioServiceCpfTest {

    private UsuarioRepository repository;
    private ProdutorRepository produtorRepository;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        produtorRepository = mock(ProdutorRepository.class);
        service = new UsuarioService(repository, produtorRepository, mock(PasswordEncoder.class));
    }

    @Test
    void profileUpdatesKeepCpfUnchangedAndUpdateTheLinkedProducerName() {
        String cpf = "12345678900";
        Produtor produtor = new Produtor("Nome antigo", cpf);
        when(produtorRepository.findByCpf(cpf)).thenReturn(Optional.of(produtor));
        Usuario usuario = new Usuario("produtor", "hash", Role.PRODUTOR);
        usuario.setCpf(cpf);
        PerfilUpdateDTO dto = new PerfilUpdateDTO();
        dto.setNomeCompleto("Nome atualizado");

        service.aplicarDados(usuario, dto);

        assertEquals(cpf, usuario.getCpf());
        assertEquals(cpf, produtor.getCpf());
        assertEquals("Nome atualizado", produtor.getNome());
    }
}
