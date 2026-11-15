package com.agrodata.service;

import com.agrodata.dto.FazendaRequestDTO;
import com.agrodata.model.Fazenda;
import com.agrodata.model.Localizacao;
import com.agrodata.model.Produtor;
import com.agrodata.model.Role;
import com.agrodata.model.Usuario;
import com.agrodata.repository.FazendaRepository;
import com.agrodata.repository.LocalizacaoRepository;
import com.agrodata.repository.ProdutorRepository;
import com.agrodata.repository.TalhaoRepository;
import com.agrodata.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FazendaServiceTest {

    @Test
    void createsFarmLinkedToTheAuthenticatedUsersProducerRecord() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ProdutorRepository produtores = mock(ProdutorRepository.class);
        LocalizacaoRepository localizacoes = mock(LocalizacaoRepository.class);
        FazendaRepository fazendas = mock(FazendaRepository.class);
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        Usuario usuario = new Usuario("luigi", "hash", Role.PRODUTOR);
        usuario.setNomeCompleto("Luigi Produtor");
        usuario.setCpf("12345678900");
        when(usuarios.findByUsername("luigi")).thenReturn(Optional.of(usuario));
        when(produtores.save(any(Produtor.class))).thenAnswer(invocation -> {
            Produtor produtor = invocation.getArgument(0);
            produtor.setIdProdutor(15L);
            return produtor;
        });
        when(localizacoes.save(any(Localizacao.class))).thenAnswer(invocation -> {
            Localizacao localizacao = invocation.getArgument(0);
            localizacao.setIdLocalizacao(27L);
            return localizacao;
        });
        when(fazendas.save(any(Fazenda.class))).thenAnswer(invocation -> {
            Fazenda fazenda = invocation.getArgument(0);
            fazenda.setIdFazenda(38L);
            return fazenda;
        });

        FazendaRequestDTO dto = new FazendaRequestDTO();
        dto.setNome("  Fazenda   Boa Vista  ");
        dto.setAreaTotal(new BigDecimal("125.50"));
        dto.setRua("  Rua   Inexistente  ");
        dto.setNumero(" S/N ");
        dto.setBairro(" Bairro   Rural ");
        dto.setCep("00000-000");
        dto.setCidade(" Cidade   Inexistente ");
        dto.setEstado("sp");
        dto.setLatitude(-23.5505);
        dto.setLongitude(-46.6333);

        FazendaService service = new FazendaService(
                usuarios, produtores, localizacoes, fazendas, talhoes);
        var result = service.cadastrar("luigi", dto);

        verify(produtores).save(any(Produtor.class));
        ArgumentCaptor<Localizacao> localizacaoCaptor = ArgumentCaptor.forClass(Localizacao.class);
        verify(localizacoes).save(localizacaoCaptor.capture());
        ArgumentCaptor<Fazenda> fazendaCaptor = ArgumentCaptor.forClass(Fazenda.class);
        verify(fazendas).save(fazendaCaptor.capture());
        assertEquals("Rua Inexistente", localizacaoCaptor.getValue().getRua());
        assertEquals("S/N", localizacaoCaptor.getValue().getNumero());
        assertEquals("Bairro Rural", localizacaoCaptor.getValue().getBairro());
        assertEquals("00000000", localizacaoCaptor.getValue().getCep());
        assertEquals("Cidade Inexistente", localizacaoCaptor.getValue().getCidade());
        assertEquals("SP", localizacaoCaptor.getValue().getEstado());
        assertEquals(-23.5505, localizacaoCaptor.getValue().getLatitude());
        assertEquals(-46.6333, localizacaoCaptor.getValue().getLongitude());
        assertEquals(15L, fazendaCaptor.getValue().getIdProdutor());
        assertEquals(27L, fazendaCaptor.getValue().getIdLocalizacao());
        assertEquals(38L, result.idFazenda());
        assertEquals("Fazenda Boa Vista", result.nome());
        assertEquals(new BigDecimal("125.50"), result.areaDisponivel());
    }

    @Test
    void refusesDeletingFarmWhilePlotsExist() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ProdutorRepository produtores = mock(ProdutorRepository.class);
        LocalizacaoRepository localizacoes = mock(LocalizacaoRepository.class);
        FazendaRepository fazendas = mock(FazendaRepository.class);
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        when(fazendas.findById(38L)).thenReturn(Optional.of(new Fazenda()));
        when(talhoes.findByIdFazenda(38L)).thenReturn(List.of(new com.agrodata.model.Talhao()));

        FazendaService service = new FazendaService(usuarios, produtores, localizacoes, fazendas, talhoes);
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.excluir(38L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(fazendas, never()).delete(any(Fazenda.class));
    }

    @Test
    void refusesShrinkingFarmBelowTheAreaUsedByItsPlots() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ProdutorRepository produtores = mock(ProdutorRepository.class);
        LocalizacaoRepository localizacoes = mock(LocalizacaoRepository.class);
        FazendaRepository fazendas = mock(FazendaRepository.class);
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        Fazenda fazenda = new Fazenda(15L, 27L, "Fazenda", new BigDecimal("100.00"));
        when(fazendas.findById(38L)).thenReturn(Optional.of(fazenda));
        when(talhoes.somarAreaPorFazenda(38L)).thenReturn(new BigDecimal("75.00"));
        FazendaRequestDTO dto = new FazendaRequestDTO();
        dto.setNome("Fazenda");
        dto.setAreaTotal(new BigDecimal("70.00"));
        FazendaService service = new FazendaService(usuarios, produtores, localizacoes, fazendas, talhoes);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.atualizar(38L, dto));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(fazendas, never()).save(any(Fazenda.class));
    }
}
