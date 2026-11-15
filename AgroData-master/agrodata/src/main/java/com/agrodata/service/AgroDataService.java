package com.agrodata.service;

import com.agrodata.dto.ClimaRequestDTO;
import com.agrodata.dto.LeituraRequestDTO;
import com.agrodata.dto.SensorRequestDTO;
import com.agrodata.dto.SensorResponseDTO;
import com.agrodata.dto.SensorUpdateDTO;
import com.agrodata.model.Fazenda;
import com.agrodata.model.LeituraSensor;
import com.agrodata.model.Sensor;
import com.agrodata.model.Talhao;
import com.agrodata.repository.FazendaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.agrodata.repository.LeituraRepository;
import com.agrodata.repository.SensorRepository;
import com.agrodata.repository.TalhaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgroDataService {

    private final LeituraRepository leituraRepository;
    private final FazendaRepository fazendaRepository;
    private final SensorRepository sensorRepository;
    private final TalhaoRepository talhaoRepository;
    private final IrrigacaoService irrigacaoService;
    private final LeituraSimuladaService leituraSimuladaService;
    private final ProdutorAccessService produtorAccess;
    private static final Logger log = LoggerFactory.getLogger(AgroDataService.class);

    public AgroDataService(LeituraRepository leituraRepository,
                           FazendaRepository fazendaRepository,
                           SensorRepository sensorRepository,
                           TalhaoRepository talhaoRepository,
                           IrrigacaoService irrigacaoService,
                           LeituraSimuladaService leituraSimuladaService,
                           ProdutorAccessService produtorAccess) {
        this.irrigacaoService = irrigacaoService;
        this.leituraSimuladaService = leituraSimuladaService;
        this.leituraRepository = leituraRepository;
        this.fazendaRepository = fazendaRepository;
        this.sensorRepository = sensorRepository;
        this.talhaoRepository = talhaoRepository;
        this.produtorAccess = produtorAccess;
    }

    public List<Talhao> listarTalhoes(String username, boolean admin) {
        return produtorAccess.talhoesAcessiveis(username, admin);
    }

    public List<SensorResponseDTO> listarSensores(String username, boolean admin) {
        List<Long> idsTalhoes = produtorAccess.idsTalhoesAcessiveis(username, admin);
        return sensorRepository.findAll().stream()
                .filter(sensor -> idsTalhoes.contains(sensor.getIdTalhao()))
                .map(this::toSensorResponse)
                .collect(Collectors.toList());
    }

    public SensorResponseDTO salvarSensor(SensorRequestDTO dto, String username, boolean admin) {
        produtorAccess.exigirAcessoTalhao(dto.getIdTalhao(), username, admin);
        Sensor sensor = new Sensor(
                dto.getIdTalhao(),
                dto.getTipoSensor(),
                dto.getDataInstalacao(),
                dto.getStatus()
        );
        Sensor salvo = sensorRepository.save(sensor);
        List<LeituraSensor> leituras = leituraSimuladaService.gerarLeiturasIniciais(salvo);
        if (!leituras.isEmpty()) {
            try {
                irrigacaoService.avaliarLeitura(leituras.get(leituras.size() - 1));
            } catch (Exception e) {
                log.error("Falha ao avaliar irrigação do sensor {}", salvo.getIdSensor(), e);
            }
        }
        return toSensorResponse(salvo);
    }

    @Transactional
    public SensorResponseDTO atualizarSensor(Long idSensor, SensorUpdateDTO dto, String username, boolean admin) {
        Sensor sensor = sensorRepository.findById(idSensor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sensor não encontrado."));
        produtorAccess.exigirAcessoTalhao(sensor.getIdTalhao(), username, admin);
        sensor.setTipoSensor(dto.getTipoSensor().trim());
        sensor.setDataInstalacao(dto.getDataInstalacao());
        sensor.setStatus(dto.getStatus().trim().toUpperCase(Locale.ROOT));
        return toSensorResponse(sensorRepository.save(sensor));
    }

    @Transactional
    public ExclusaoSensorResultado excluirSensor(Long idSensor) {
        if (!sensorRepository.existsById(idSensor)) {
            return ExclusaoSensorResultado.NAO_ENCONTRADO;
        }
        if (leituraRepository.existsByIdSensor(idSensor)) {
            return ExclusaoSensorResultado.POSSUI_LEITURAS;
        }
        sensorRepository.deleteById(idSensor);
        return ExclusaoSensorResultado.EXCLUIDO;
    }

    public enum ExclusaoSensorResultado {
        EXCLUIDO,
        NAO_ENCONTRADO,
        POSSUI_LEITURAS
    }

    private SensorResponseDTO toSensorResponse(Sensor sensor) {
        return new SensorResponseDTO(
                sensor.getIdSensor(),
                sensor.getIdTalhao(),
                sensor.getTipoSensor(),
                sensor.getDataInstalacao(),
                sensor.getStatus()
        );
    }

    public LeituraSensor registrarLeitura(LeituraRequestDTO dto, String username, boolean admin) {
        exigirAcessoSensor(dto.getIdSensor(), username, admin);
        LocalDateTime dataHora = dto.getDataHora() != null ? dto.getDataHora() : LocalDateTime.now();
        LeituraSensor leitura = new LeituraSensor(
            dto.getIdSensor(),
            dataHora,
            dto.getUmidadeSolo(),
            dto.getTemperatura()
        );
        LeituraSensor salva = leituraRepository.save(leitura);
        try {
            irrigacaoService.avaliarLeitura(salva);
        } catch (Exception e) {
            // Falha na avaliação/notificação nunca deve impedir o registro da leitura
            log.error("Falha ao avaliar irrigação da leitura {}", salva.getIdLeitura(), e);
        }
        return salva;
    }

    public boolean excluirLeitura(Long idLeitura) {
        if (!leituraRepository.existsById(idLeitura)) {
            return false;
        }
        leituraRepository.deleteById(idLeitura);
        return true;
    }

    public Map<String, Object> analisarIrrigacaoComClima(Long idSensor, ClimaRequestDTO clima,
                                                          String username, boolean admin) {
        exigirAcessoSensor(idSensor, username, admin);
        List<LeituraSensor> leituras = leituraRepository.findByIdSensorOrderByDataHoraDesc(idSensor);
        Map<String, Object> resposta = new HashMap<>();

        if (leituras.isEmpty()) {
            resposta.put("status", "SEM_DADOS");
            resposta.put("mensagem", "Nenhuma leitura encontrada para este sensor.");
            return resposta;
        }

        LeituraSensor ultimaLeitura = leituras.get(0);
        double umidadeSolo = ultimaLeitura.getUmidadeSolo();

        resposta.put("umidadeSoloAtual", umidadeSolo);
        resposta.put("temperaturaSoloAtual", ultimaLeitura.getTemperatura());
        resposta.put("probabilidadeChuva", clima.getProbabilidadeChuva());

        if (umidadeSolo < 30.0) {
            if (clima.getProbabilidadeChuva() != null && clima.getProbabilidadeChuva() > 60.0) {
                resposta.put("recomendacao", "AGUARDAR_CHUVA");
                resposta.put("statusAlerta", "ATENÇÃO");
                resposta.put("mensagem", "Solo seco (" + umidadeSolo + "%), mas há alta probabilidade de chuva (" + clima.getProbabilidadeChuva() + "%). Irrigação suspensa para poupar água.");
            } else {
                resposta.put("recomendacao", "IRRIGAR_AGORA");
                resposta.put("statusAlerta", "CRÍTICO");
                resposta.put("mensagem", "Solo seco (" + umidadeSolo + "%) e baixa probabilidade de chuva. Acione a irrigação imediatamente!");
            }
        } else {
            resposta.put("recomendacao", "SOLO_UMIDO");
            resposta.put("statusAlerta", "NORMAL");
            resposta.put("mensagem", "Umidade do solo adequada (" + umidadeSolo + "%). Nenhuma ação necessária.");
        }

        return resposta;
    }

    public List<Fazenda> listarFazendasPorProdutor(Long idProdutor) {
        return fazendaRepository.findByIdProdutor(idProdutor);
    }

    public List<LeituraSensor> obterHistoricoLeituras(Long idSensor, String username, boolean admin) {
        exigirAcessoSensor(idSensor, username, admin);
        return leituraRepository.findByIdSensorOrderByDataHoraDesc(idSensor);
    }

    private void exigirAcessoSensor(Long idSensor, String username, boolean admin) {
        Sensor sensor = sensorRepository.findById(idSensor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sensor não encontrado."));
        produtorAccess.exigirAcessoTalhao(sensor.getIdTalhao(), username, admin);
    }
}
