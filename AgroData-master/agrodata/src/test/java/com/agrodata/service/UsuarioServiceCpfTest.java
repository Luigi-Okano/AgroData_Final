package com.agrodata.service;

import com.agrodata.dto.PerfilResponseDTO;
import com.agrodata.dto.PerfilUpdateDTO;
import com.agrodata.model.Produtor;
import com.agrodata.model.Role;
import com.agrodata.model.Usuario;
import com.agrodata.repository.ProdutorRepository;
import com.agrodata.repository.UsuarioRepository;
import com.agrodata.security.CpfFingerprintService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceCpfTest {

    private UsuarioRepository repository;
    private ProdutorRepository produtorRepository;
    private CpfFingerprintService fingerprintService;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        produtorRepository = mock(ProdutorRepository.class);
        fingerprintService = new CpfFingerprintService(Base64.getEncoder().encodeToString(new byte[32]));
        service = new UsuarioService(repository, produtorRepository, mock(PasswordEncoder.class), fingerprintService);
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

    @Test
    void registersMissingCpfOnceAndReturnsOnlyMaskedValue() {
        Usuario usuario = new Usuario("produtor", "hash", Role.PRODUTOR);
        when(repository.findByUsername("produtor")).thenReturn(Optional.of(usuario));
        when(repository.findAllByCpfFingerprintIsNull()).thenReturn(List.of());
        when(repository.save(usuario)).thenReturn(usuario);

        Usuario atualizado = service.cadastrarCpf("produtor", "123.456.789-10");

        assertEquals("12345678910", atualizado.getCpf());
        assertEquals(fingerprintService.gerar("12345678910"), atualizado.getCpfFingerprint());
        assertEquals("***.456.789-**", PerfilResponseDTO.de(atualizado).cpf());
        verify(repository).save(usuario);
    }

    @Test
    void refusesWrongLengthAndDoesNotReplaceAnExistingCpf() {
        Usuario usuario = new Usuario("produtor", "hash", Role.PRODUTOR);
        when(repository.findByUsername("produtor")).thenReturn(Optional.of(usuario));

        ResponseStatusException wrongLength = assertThrows(ResponseStatusException.class,
                () -> service.cadastrarCpf("produtor", "123.456.789-1"));
        assertEquals(HttpStatus.BAD_REQUEST, wrongLength.getStatusCode());

        usuario.setCpf("12345678910");
        ResponseStatusException existingCpf = assertThrows(ResponseStatusException.class,
                () -> service.cadastrarCpf("produtor", "123.456.789-10"));
        assertEquals(HttpStatus.CONFLICT, existingCpf.getStatusCode());
        verify(repository, never()).save(usuario);
    }

    @Test
    void refusesCpfAlreadyAssignedToAnotherAccount() {
        String cpf = "12345678910";
        Usuario existente = new Usuario("outra-conta", "hash", Role.PRODUTOR);
        existente.setCpf(cpf);
        Usuario novo = new Usuario("nova-conta", "hash", Role.PRODUTOR);
        when(repository.findAllByCpfFingerprintIsNull()).thenReturn(List.of(existente));

        ResponseStatusException duplicado = assertThrows(ResponseStatusException.class,
                () -> service.definirCpf(novo, cpf));

        assertEquals(HttpStatus.CONFLICT, duplicado.getStatusCode());
        assertNull(novo.getCpf());
    }

    @Test
    void allowsDifferentAccountsToUseTheSameName() {
        Usuario primeiro = new Usuario("primeiro", "hash", Role.PRODUTOR);
        Usuario segundo = new Usuario("segundo", "hash", Role.PRODUTOR);
        PerfilUpdateDTO dto = new PerfilUpdateDTO();
        dto.setNomeCompleto("Nome compartilhado");

        service.aplicarDados(primeiro, dto);
        service.aplicarDados(segundo, dto);

        assertEquals(primeiro.getNomeCompleto(), segundo.getNomeCompleto());
    }
}
