package com.agrodata.service;

import com.agrodata.dto.FazendaDTO;
import com.agrodata.dto.TalhaoRequestDTO;
import com.agrodata.dto.TalhaoResponseDTO;
import com.agrodata.dto.TalhaoUpdateDTO;
import com.agrodata.model.*;
import com.agrodata.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TalhaoService {

    private static final Logger log = LoggerFactory.getLogger(TalhaoService.class);

    private final TalhaoRepository talhaoRepository;
    private final FazendaRepository fazendaRepository;
    private final CulturaRepository culturaRepository;
    private final SensorRepository sensorRepository;
    private final LeituraSimuladaService leituraSimuladaService;
    private final IrrigacaoService irrigacaoService;
    private final TransactionTemplate tx;
    private final ProdutorAccessService produtorAccess;

    public TalhaoService(TalhaoRepository talhaoRepository, FazendaRepository fazendaRepository,
                         CulturaRepository culturaRepository, SensorRepository sensorRepository,
                         LeituraSimuladaService leituraSimuladaService, IrrigacaoService irrigacaoService,
                         TransactionTemplate tx, ProdutorAccessService produtorAccess) {
        this.talhaoRepository = talhaoRepository;
        this.fazendaRepository = fazendaRepository;
        this.culturaRepository = culturaRepository;
        this.sensorRepository = sensorRepository;
        this.leituraSimuladaService = leituraSimuladaService;
        this.irrigacaoService = irrigacaoService;
        this.tx = tx;
        this.produtorAccess = produtorAccess;
    }

    public List<FazendaDTO> listarFazendas(String username, boolean admin) {
        return produtorAccess.fazendasAcessiveis(username, admin).stream().map(f -> {
            BigDecimal usada = talhaoRepository.somarAreaPorFazenda(f.getIdFazenda());
            return new FazendaDTO(f.getIdFazenda(), f.getNome(), f.getAreaTotal(),
                    f.getAreaTotal().subtract(usada).max(BigDecimal.ZERO),
                    talhaoRepository.findByIdFazenda(f.getIdFazenda()).size());
        }).toList();
    }

    /**
     * Cadastra o talhão, cria um sensor e gera as leituras iniciais.
     * Tudo é gravado numa única transação; a avaliação de irrigação roda depois do commit.
     */
    public TalhaoResponseDTO cadastrar(TalhaoRequestDTO dto, String username, boolean admin) {
        record Resultado(Talhao talhao, Sensor sensor, List<LeituraSensor> leituras) {}

        Resultado r = tx.execute(status -> {
            produtorAccess.exigirAcessoFazenda(dto.getIdFazenda(), username, admin);
            Fazenda fazenda = fazendaRepository.findById(dto.getIdFazenda())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fazenda não encontrada."));
            Cultura cultura = null;
            if (dto.getIdCultura() != null) {
                cultura = culturaRepository.findById(dto.getIdCultura())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cultura não encontrada."));
            }
            BigDecimal disponivel = fazenda.getAreaTotal().subtract(talhaoRepository.somarAreaPorFazenda(fazenda.getIdFazenda()));
            if (dto.getArea().compareTo(disponivel) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A área excede o espaço disponível na fazenda (" + disponivel.max(BigDecimal.ZERO) + " ha).");
            }

            Talhao talhao = new Talhao(fazenda.getIdFazenda(), dto.getNome().trim(), dto.getArea(),
                    dto.getTipoSolo() == null || dto.getTipoSolo().isBlank() ? null : dto.getTipoSolo().trim());
            talhao.setIdCultura(dto.getIdCultura());
            talhao = talhaoRepository.save(talhao);

            Sensor sensor = sensorRepository.save(new Sensor(talhao.getIdTalhao(),
                    "Umidade e Temperatura do Solo", LocalDate.now(), "ATIVO"));
            List<LeituraSensor> leituras = leituraSimuladaService.gerarLeiturasIniciais(sensor, cultura);
            return new Resultado(talhao, sensor, leituras);
        });

        if (!r.leituras().isEmpty()) {
            try {
                // Última leitura (a mais recente) decide se já nasce com comando de irrigação
                irrigacaoService.avaliarLeitura(r.leituras().get(r.leituras().size() - 1));
            } catch (Exception e) {
                log.error("Falha ao avaliar irrigação do talhão {}", r.talhao().getIdTalhao(), e);
            }
        }

        Talhao t = r.talhao();
        return new TalhaoResponseDTO(t.getIdTalhao(), t.getIdFazenda(), t.getNome(), t.getArea(), t.getTipoSolo(),
                t.getIdCultura(), r.sensor() != null ? r.sensor().getIdSensor() : null, r.leituras().size());
    }

    @Transactional
    public void atualizar(Long idTalhao, TalhaoUpdateDTO dto, String username, boolean admin) {
        Talhao talhao = talhaoRepository.findById(idTalhao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Talhão não encontrado."));
        produtorAccess.exigirAcessoFazenda(talhao.getIdFazenda(), username, admin);
        Fazenda fazenda = fazendaRepository.findById(talhao.getIdFazenda())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fazenda não encontrada."));
        if (dto.getIdCultura() != null) {
            culturaRepository.findById(dto.getIdCultura())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cultura não encontrada."));
        }

        BigDecimal areaOcupada = talhaoRepository.somarAreaPorFazenda(fazenda.getIdFazenda());
        BigDecimal areaDisponivel = fazenda.getAreaTotal().subtract(areaOcupada).add(talhao.getArea());
        if (dto.getArea().compareTo(areaDisponivel) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A área excede o espaço disponível na fazenda (" + areaDisponivel.max(BigDecimal.ZERO) + " ha).");
        }

        talhao.setNome(dto.getNome().trim().replaceAll("\\s+", " "));
        talhao.setArea(dto.getArea());
        talhao.setTipoSolo(dto.getTipoSolo() == null || dto.getTipoSolo().isBlank()
                ? null : dto.getTipoSolo().trim());
        talhao.setIdCultura(dto.getIdCultura());
        talhaoRepository.save(talhao);
    }

    @Transactional
    public void excluir(Long idTalhao) {
        Talhao talhao = talhaoRepository.findById(idTalhao)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Talhão não encontrado."));
        if (talhaoRepository.possuiRegistrosDependentes(idTalhao)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exclua primeiro os sensores, comandos de irrigação, dados climáticos, consumo de água e produção vinculados ao talhão.");
        }
        talhaoRepository.delete(talhao);
    }

}
