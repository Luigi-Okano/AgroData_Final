package com.agrodata.service;

import com.agrodata.dto.TalhaoUpdateDTO;
import com.agrodata.model.Fazenda;
import com.agrodata.model.Talhao;
import com.agrodata.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TalhaoServiceTest {

    @Test
    void updatesPlotWithoutChangingItsFarmAndChecksAvailableArea() {
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        FazendaRepository fazendas = mock(FazendaRepository.class);
        ProdutorAccessService produtorAccess = mock(ProdutorAccessService.class);
        Talhao talhao = new Talhao(12L, "Talhão antigo", new BigDecimal("3.00"), "Arenoso");
        talhao.setIdTalhao(23L);
        Fazenda fazenda = new Fazenda(4L, 5L, "Fazenda", new BigDecimal("10.00"));
        fazenda.setIdFazenda(12L);
        when(talhoes.findById(23L)).thenReturn(Optional.of(talhao));
        when(fazendas.findById(12L)).thenReturn(Optional.of(fazenda));
        when(talhoes.somarAreaPorFazenda(12L)).thenReturn(new BigDecimal("8.00"));
        TalhaoUpdateDTO dto = new TalhaoUpdateDTO();
        dto.setNome("  Talhão   novo ");
        dto.setArea(new BigDecimal("4.50"));
        dto.setTipoSolo(" Argiloso ");

        service(talhoes, fazendas, mock(CulturaRepository.class), produtorAccess)
                .atualizar(23L, dto, "produtor", false);

        verify(produtorAccess).exigirAcessoFazenda(12L, "produtor", false);
        verify(talhoes).save(talhao);
        assertEquals(12L, talhao.getIdFazenda());
        assertEquals("Talhão novo", talhao.getNome());
        assertEquals(new BigDecimal("4.50"), talhao.getArea());
        assertEquals("Argiloso", talhao.getTipoSolo());
    }

    @Test
    void refusesDeletingPlotWhileDependentRecordsExist() {
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        Talhao talhao = new Talhao(12L, "Talhão", BigDecimal.ONE, null);
        when(talhoes.findById(23L)).thenReturn(Optional.of(talhao));
        when(talhoes.possuiRegistrosDependentes(23L)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service(talhoes, mock(FazendaRepository.class), mock(CulturaRepository.class),
                        mock(ProdutorAccessService.class)).excluir(23L));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(talhoes, never()).delete(any(Talhao.class));
    }

    @Test
    void refusesPlotAreaLargerThanRemainingFarmArea() {
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        FazendaRepository fazendas = mock(FazendaRepository.class);
        Talhao talhao = new Talhao(12L, "Talhão", new BigDecimal("3.00"), null);
        Fazenda fazenda = new Fazenda(4L, 5L, "Fazenda", new BigDecimal("10.00"));
        fazenda.setIdFazenda(12L);
        when(talhoes.findById(23L)).thenReturn(Optional.of(talhao));
        when(fazendas.findById(12L)).thenReturn(Optional.of(fazenda));
        when(talhoes.somarAreaPorFazenda(12L)).thenReturn(new BigDecimal("8.00"));
        TalhaoUpdateDTO dto = new TalhaoUpdateDTO();
        dto.setNome("Talhão");
        dto.setArea(new BigDecimal("6.00"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service(talhoes, fazendas, mock(CulturaRepository.class),
                        mock(ProdutorAccessService.class)).atualizar(23L, dto, "produtor", false));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(talhoes, never()).save(any(Talhao.class));
    }

    private TalhaoService service(TalhaoRepository talhoes, FazendaRepository fazendas,
                                 CulturaRepository culturas, ProdutorAccessService produtorAccess) {
        return new TalhaoService(talhoes, fazendas, culturas, mock(SensorRepository.class),
                mock(LeituraSimuladaService.class), mock(IrrigacaoService.class),
                mock(TransactionTemplate.class), produtorAccess);
    }
}
