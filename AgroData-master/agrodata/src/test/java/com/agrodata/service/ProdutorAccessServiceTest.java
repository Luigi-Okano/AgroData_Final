package com.agrodata.service;

import com.agrodata.model.Fazenda;
import com.agrodata.model.Produtor;
import com.agrodata.model.Role;
import com.agrodata.model.Usuario;
import com.agrodata.repository.FazendaRepository;
import com.agrodata.repository.ProdutorRepository;
import com.agrodata.repository.TalhaoRepository;
import com.agrodata.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProdutorAccessServiceTest {

    @Test
    void listsOnlyFarmsOwnedByTheAuthenticatedProducer() {
        Usuario usuario = new Usuario("luigi", "hash", Role.PRODUTOR);
        usuario.setCpf("123.456.789-00");
        Produtor produtor = new Produtor("Luigi", "12345678900");
        produtor.setIdProdutor(7L);
        Fazenda fazenda = new Fazenda(7L, 3L, "Minha fazenda", BigDecimal.TEN);

        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ProdutorRepository produtores = mock(ProdutorRepository.class);
        FazendaRepository fazendas = mock(FazendaRepository.class);
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        when(usuarios.findByUsername("luigi")).thenReturn(Optional.of(usuario));
        when(produtores.findByCpf("12345678900")).thenReturn(Optional.of(produtor));
        when(fazendas.findByIdProdutor(7L)).thenReturn(List.of(fazenda));

        ProdutorAccessService access = new ProdutorAccessService(usuarios, produtores, fazendas, talhoes);

        assertEquals(List.of(fazenda), access.fazendasAcessiveis("luigi", false));
    }

    @Test
    void hidesFarmExistenceWhenProducerTriesToAccessAnotherOwnersFarm() {
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        ProdutorRepository produtores = mock(ProdutorRepository.class);
        FazendaRepository fazendas = mock(FazendaRepository.class);
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        Usuario usuario = new Usuario("luigi", "hash", Role.PRODUTOR);
        usuario.setCpf("12345678900");
        Produtor produtor = new Produtor("Luigi", "12345678900");
        produtor.setIdProdutor(7L);
        when(usuarios.findByUsername("luigi")).thenReturn(Optional.of(usuario));
        when(produtores.findByCpf("12345678900")).thenReturn(Optional.of(produtor));
        when(fazendas.findByIdProdutor(7L)).thenReturn(List.of());

        ProdutorAccessService access = new ProdutorAccessService(usuarios, produtores, fazendas, talhoes);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> access.exigirAcessoFazenda(99L, "luigi", false));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
