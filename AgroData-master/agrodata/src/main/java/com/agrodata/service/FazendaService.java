package com.agrodata.service;

import com.agrodata.dto.FazendaDTO;
import com.agrodata.dto.FazendaDetalheDTO;
import com.agrodata.dto.FazendaRequestDTO;
import com.agrodata.model.Fazenda;
import com.agrodata.model.Localizacao;
import com.agrodata.model.Produtor;
import com.agrodata.model.Usuario;
import com.agrodata.repository.FazendaRepository;
import com.agrodata.repository.LocalizacaoRepository;
import com.agrodata.repository.ProdutorRepository;
import com.agrodata.repository.TalhaoRepository;
import com.agrodata.repository.UsuarioRepository;
import com.agrodata.util.Cpf;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class FazendaService {

    private final UsuarioRepository usuarios;
    private final ProdutorRepository produtores;
    private final LocalizacaoRepository localizacoes;
    private final FazendaRepository fazendas;
    private final TalhaoRepository talhoes;

    public FazendaService(UsuarioRepository usuarios, ProdutorRepository produtores,
                          LocalizacaoRepository localizacoes, FazendaRepository fazendas,
                          TalhaoRepository talhoes) {
        this.usuarios = usuarios;
        this.produtores = produtores;
        this.localizacoes = localizacoes;
        this.fazendas = fazendas;
        this.talhoes = talhoes;
    }

    @Transactional
    public FazendaDTO cadastrar(String username, FazendaRequestDTO dto) {
        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        String cpf = Cpf.somenteDigitos(usuario.getCpf());
        if (cpf.length() != 11 || usuario.getNomeCompleto() == null || usuario.getNomeCompleto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Atualize seu nome e CPF no perfil antes de cadastrar uma fazenda.");
        }

        Produtor produtor = produtores.findByCpf(cpf)
                .orElseGet(() -> produtores.save(new Produtor(usuario.getNomeCompleto().trim(), cpf)));
        Localizacao localizacao = new Localizacao(
                normalizarEspacos(dto.getRua()), normalizarEspacos(dto.getNumero()),
                normalizarEspacos(dto.getBairro()), dto.getCep().replaceAll("\\D", ""),
                normalizarEspacos(dto.getCidade()), dto.getEstado().trim().toUpperCase());
        localizacao.setLatitude(dto.getLatitude());
        localizacao.setLongitude(dto.getLongitude());
        localizacao = localizacoes.save(localizacao);
        Fazenda fazenda = fazendas.save(new Fazenda(
                produtor.getIdProdutor(), localizacao.getIdLocalizacao(),
                normalizarEspacos(dto.getNome()), dto.getAreaTotal()));
        return new FazendaDTO(fazenda.getIdFazenda(), fazenda.getNome(), fazenda.getAreaTotal(),
                fazenda.getAreaTotal(), 0);
    }

    @Transactional(readOnly = true)
    public FazendaDetalheDTO detalhar(Long idFazenda) {
        Fazenda fazenda = fazendas.findById(idFazenda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada."));
        Localizacao localizacao = localizacoes.findById(fazenda.getIdLocalizacao())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "A localização da fazenda não foi encontrada."));
        return new FazendaDetalheDTO(fazenda.getIdFazenda(), fazenda.getNome(), fazenda.getAreaTotal(),
                localizacao.getRua(), localizacao.getNumero(), localizacao.getBairro(), localizacao.getCep(),
                localizacao.getCidade(), localizacao.getEstado(), localizacao.getLatitude(), localizacao.getLongitude());
    }

    @Transactional
    public void atualizar(Long idFazenda, FazendaRequestDTO dto) {
        Fazenda fazenda = fazendas.findById(idFazenda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada."));
        BigDecimal areaOcupada = talhoes.somarAreaPorFazenda(idFazenda);
        if (dto.getAreaTotal().compareTo(areaOcupada) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A área total não pode ser menor que a área ocupada pelos talhões (" + areaOcupada + " ha).");
        }

        Localizacao localizacao = localizacoes.findById(fazenda.getIdLocalizacao())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "A localização da fazenda não foi encontrada."));
        fazenda.setNome(normalizarEspacos(dto.getNome()));
        fazenda.setAreaTotal(dto.getAreaTotal());
        localizacao.setRua(normalizarEspacos(dto.getRua()));
        localizacao.setNumero(normalizarEspacos(dto.getNumero()));
        localizacao.setBairro(normalizarEspacos(dto.getBairro()));
        localizacao.setCep(dto.getCep().replaceAll("\\D", ""));
        localizacao.setCidade(normalizarEspacos(dto.getCidade()));
        localizacao.setEstado(dto.getEstado().trim().toUpperCase());
        localizacao.setLatitude(dto.getLatitude());
        localizacao.setLongitude(dto.getLongitude());
        localizacoes.save(localizacao);
        fazendas.save(fazenda);
    }

    @Transactional
    public void excluir(Long idFazenda) {
        Fazenda fazenda = fazendas.findById(idFazenda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada."));
        if (!talhoes.findByIdFazenda(idFazenda).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exclua todos os talhões desta fazenda antes de apagá-la.");
        }
        fazendas.delete(fazenda);
    }

    private String normalizarEspacos(String valor) {
        return valor.trim().replaceAll("\\s+", " ");
    }
}
