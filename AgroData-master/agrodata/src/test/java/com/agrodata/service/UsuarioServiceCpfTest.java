package com.agrodata.service;

import com.agrodata.dto.PerfilResponseDTO;
import com.agrodata.dto.PerfilUpdateDTO;
import com.agrodata.model.EmailProdutor;
import com.agrodata.model.Produtor;
import com.agrodata.model.Role;
import com.agrodata.model.TelefoneProdutor;
import com.agrodata.model.Usuario;
import com.agrodata.repository.EmailProdutorRepository;
import com.agrodata.repository.ProdutorRepository;
import com.agrodata.repository.TelefoneProdutorRepository;
import com.agrodata.repository.UsuarioRepository;
import com.agrodata.security.CpfFingerprintService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceCpfTest {

    private UsuarioRepository repository;
    private ProdutorRepository produtorRepository;
    private EmailProdutorRepository emailRepository;
    private TelefoneProdutorRepository telefoneRepository;
    private CpfFingerprintService fingerprintService;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        produtorRepository = mock(ProdutorRepository.class);
        emailRepository = mock(EmailProdutorRepository.class);
        telefoneRepository = mock(TelefoneProdutorRepository.class);
        fingerprintService = new CpfFingerprintService(Base64.getEncoder().encodeToString(new byte[32]));
        service = new UsuarioService(repository, produtorRepository, emailRepository, telefoneRepository,
                mock(PasswordEncoder.class), fingerprintService);
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
    void profileEditsNormalizeAndSynchronizePrimaryContacts() {
        String cpf = "12345678900";
        Usuario usuario = new Usuario("produtor", "hash", Role.PRODUTOR);
        usuario.setCpf(cpf);
        usuario.setNomeCompleto("Nome anterior");
        usuario.setEmail("anterior@example.com");
        usuario.setTelefone("(11) 99999-0000");
        Produtor produtor = new Produtor("Nome anterior", cpf);
        produtor.setIdProdutor(7L);
        EmailProdutor email = new EmailProdutor(7L, "anterior@example.com", "Principal");
        TelefoneProdutor telefone = new TelefoneProdutor(7L, "11999990000", "Celular");
        when(repository.findByUsername("produtor")).thenReturn(Optional.of(usuario));
        when(repository.save(usuario)).thenReturn(usuario);
        when(produtorRepository.findByCpf(cpf)).thenReturn(Optional.of(produtor));
        when(emailRepository.findByIdProdutor(7L)).thenReturn(List.of(email));
        when(telefoneRepository.findByIdProdutor(7L)).thenReturn(List.of(telefone));
        PerfilUpdateDTO dto = new PerfilUpdateDTO();
        dto.setNomeCompleto("  Nome   atualizado ");
        dto.setEmail(" NOVO@EXAMPLE.COM ");
        dto.setTelefone("(11) 98888-7777");

        service.atualizarMeusDados("produtor", dto);

        assertEquals("Nome atualizado", usuario.getNomeCompleto());
        assertEquals("novo@example.com", usuario.getEmail());
        assertEquals("(11) 98888-7777", usuario.getTelefone());
        assertEquals("Nome atualizado", produtor.getNome());
        assertEquals("novo@example.com", email.getEmail());
        assertEquals("11988887777", telefone.getTelefone());
        verify(emailRepository).save(email);
        verify(telefoneRepository).save(telefone);
    }

    @Test
    void profileEditsCreatePrimaryContactsWhenNoneExist() {
        String cpf = "12345678900";
        Usuario usuario = new Usuario("produtor", "hash", Role.PRODUTOR);
        usuario.setCpf(cpf);
        usuario.setEmail("novo@example.com");
        usuario.setTelefone("(11) 98888-7777");
        Produtor produtor = new Produtor("Nome", cpf);
        produtor.setIdProdutor(8L);
        when(repository.findByUsername("produtor")).thenReturn(Optional.of(usuario));
        when(repository.save(usuario)).thenReturn(usuario);
        when(produtorRepository.findByCpf(cpf)).thenReturn(Optional.of(produtor));
        when(emailRepository.findByIdProdutor(8L)).thenReturn(List.of());
        when(telefoneRepository.findByIdProdutor(8L)).thenReturn(List.of());
        PerfilUpdateDTO dto = new PerfilUpdateDTO();
        dto.setEmail("novo@example.com");
        dto.setTelefone("(11) 98888-7777");

        service.atualizarMeusDados("produtor", dto);

        var emailCaptor = ArgumentCaptor.forClass(EmailProdutor.class);
        var telefoneCaptor = ArgumentCaptor.forClass(TelefoneProdutor.class);
        verify(emailRepository).save(emailCaptor.capture());
        verify(telefoneRepository).save(telefoneCaptor.capture());
        assertEquals("Principal", emailCaptor.getValue().getTipo());
        assertEquals("novo@example.com", emailCaptor.getValue().getEmail());
        assertEquals("Celular", telefoneCaptor.getValue().getTipo());
        assertEquals("11988887777", telefoneCaptor.getValue().getTelefone());
    }

    @Test
    void profileUpdateEmailValidationMatchesTheNormalizedSchema() {
        PerfilUpdateDTO dto = new PerfilUpdateDTO();
        dto.setEmail("ana@example.COM.BR");
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(dto).isEmpty());

            dto.setEmail("");
            assertTrue(validator.validate(dto).isEmpty());

            dto.setEmail("ana@example.org");
            assertTrue(validator.validate(dto).stream()
                    .anyMatch(violation -> violation.getPropertyPath().toString().equals("email")));
        }
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
