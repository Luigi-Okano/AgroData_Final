package com.agrodata.service;

import com.agrodata.dto.AdminProdutorDTO;
import com.agrodata.dto.IrrigacaoStatusDTO;
import com.agrodata.model.*;
import com.agrodata.repository.*;
import com.agrodata.util.Cpf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminProdutorService {

    private static final Logger log = LoggerFactory.getLogger(AdminProdutorService.class);

    private final ProdutorRepository produtores;
    private final FazendaRepository fazendas;
    private final TalhaoRepository talhoes;
    private final LocalizacaoRepository localizacoes;
    private final TelefoneProdutorRepository telefones;
    private final EmailProdutorRepository emails;
    private final CulturaRepository culturas;
    private final IrrigacaoService irrigacao;

    public AdminProdutorService(ProdutorRepository produtores, FazendaRepository fazendas, TalhaoRepository talhoes,
                                LocalizacaoRepository localizacoes, TelefoneProdutorRepository telefones,
                                EmailProdutorRepository emails, CulturaRepository culturas, IrrigacaoService irrigacao) {
        this.produtores = produtores;
        this.fazendas = fazendas;
        this.talhoes = talhoes;
        this.localizacoes = localizacoes;
        this.telefones = telefones;
        this.emails = emails;
        this.culturas = culturas;
        this.irrigacao = irrigacao;
    }

    public List<AdminProdutorDTO> listar(String admin) {
        log.info("AUDITORIA: admin '{}' consultou produtores, fazendas e talhões", admin);

        Map<Long, String> situacaoPorTalhao = irrigacao.listarStatus().stream()
                .collect(Collectors.toMap(IrrigacaoStatusDTO::idTalhao, IrrigacaoStatusDTO::situacao));
        Map<Long, String> nomeCultura = culturas.findAll().stream()
                .collect(Collectors.toMap(Cultura::getIdCultura, Cultura::getNome));
        Map<Long, Localizacao> locais = localizacoes.findAll().stream()
                .collect(Collectors.toMap(Localizacao::getIdLocalizacao, l -> l));
        Map<Long, List<Talhao>> talhoesPorFazenda = talhoes.findAll().stream()
                .collect(Collectors.groupingBy(Talhao::getIdFazenda));
        Map<Long, List<Fazenda>> fazendasPorProdutor = fazendas.findAll().stream()
                .collect(Collectors.groupingBy(Fazenda::getIdProdutor));
        Map<Long, List<String>> fones = telefones.findAll().stream().collect(Collectors.groupingBy(
                TelefoneProdutor::getIdProdutor, Collectors.mapping(TelefoneProdutor::getTelefone, Collectors.toList())));
        Map<Long, List<String>> mails = emails.findAll().stream().collect(Collectors.groupingBy(
                EmailProdutor::getIdProdutor, Collectors.mapping(EmailProdutor::getEmail, Collectors.toList())));

        List<AdminProdutorDTO> resultado = new ArrayList<>();
        for (Produtor p : produtores.findAll()) {
            List<AdminProdutorDTO.Fazenda> fazendasDto = new ArrayList<>();
            int totalTalhoes = 0, paraRegar = 0;

            for (Fazenda f : fazendasPorProdutor.getOrDefault(p.getIdProdutor(), List.of())) {
                List<AdminProdutorDTO.Talhao> talhoesDto = new ArrayList<>();
                for (Talhao t : talhoesPorFazenda.getOrDefault(f.getIdFazenda(), List.of())) {
                    String situacao = situacaoPorTalhao.getOrDefault(t.getIdTalhao(), "SEM_DADOS");
                    if ("REGAR".equals(situacao)) paraRegar++;
                    talhoesDto.add(new AdminProdutorDTO.Talhao(t.getIdTalhao(), t.getNome(),
                            t.getIdCultura() != null ? nomeCultura.get(t.getIdCultura()) : null,
                            t.getTipoSolo(), situacao));
                }
                totalTalhoes += talhoesDto.size();
                Localizacao loc = locais.get(f.getIdLocalizacao());
                fazendasDto.add(new AdminProdutorDTO.Fazenda(f.getIdFazenda(), f.getNome(),
                        loc != null ? loc.getCidade() : null, loc != null ? loc.getEstado() : null,
                        talhoesDto.size(), talhoesDto));
            }
            resultado.add(new AdminProdutorDTO(p.getIdProdutor(), p.getNome(), Cpf.formatar(p.getCpf()),
                    fones.getOrDefault(p.getIdProdutor(), List.of()), mails.getOrDefault(p.getIdProdutor(), List.of()),
                    fazendasDto.size(), totalTalhoes, paraRegar, fazendasDto));
        }
        resultado.sort(Comparator.comparing(AdminProdutorDTO::nome, String.CASE_INSENSITIVE_ORDER));
        return resultado;
    }
}
