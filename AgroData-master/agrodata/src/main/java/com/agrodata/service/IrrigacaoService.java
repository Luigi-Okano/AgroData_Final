package com.agrodata.service;

import com.agrodata.dto.ComandoIrrigacaoDTO;
import com.agrodata.dto.IrrigacaoStatusDTO;
import com.agrodata.model.*;
import com.agrodata.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IrrigacaoService {

    private static final List<StatusComando> ABERTOS = List.of(StatusComando.PENDENTE, StatusComando.ENVIADO);

    private final CulturaRepository culturaRepository;
    private final TalhaoRepository talhaoRepository;
    private final SensorRepository sensorRepository;
    private final LeituraRepository leituraRepository;
    private final ComandoIrrigacaoRepository comandoRepository;
    private final IrrigacaoNotifier notifier;
    private final FazendaRepository fazendaRepository;
    private final LocalizacaoRepository localizacaoRepository;
    private final OpenMeteoForecastService forecastService;
    private final double rainProbabilityThreshold;

    public IrrigacaoService(CulturaRepository culturaRepository, TalhaoRepository talhaoRepository,
                            SensorRepository sensorRepository, LeituraRepository leituraRepository,
                            ComandoIrrigacaoRepository comandoRepository, IrrigacaoNotifier notifier,
                            FazendaRepository fazendaRepository, LocalizacaoRepository localizacaoRepository,
                            OpenMeteoForecastService forecastService,
                            @Value("${agrodata.irrigacao.limite-chuva:65}") double rainProbabilityThreshold) {
        this.culturaRepository = culturaRepository;
        this.talhaoRepository = talhaoRepository;
        this.sensorRepository = sensorRepository;
        this.leituraRepository = leituraRepository;
        this.comandoRepository = comandoRepository;
        this.notifier = notifier;
        this.fazendaRepository = fazendaRepository;
        this.localizacaoRepository = localizacaoRepository;
        this.forecastService = forecastService;
        this.rainProbabilityThreshold = rainProbabilityThreshold;
    }

    public List<Cultura> listarCulturas() {
        return culturaRepository.findAll();
    }

    /**
     * Compara a leitura com a faixa da cultura do talhão. Abaixo do mínimo => cria um comando de
     * irrigação (um por vez por talhão) e avisa o sistema de irrigação. De volta ao normal => encerra comandos abertos.
     * Propositalmente SEM @Transactional: o comando precisa estar commitado antes do aviso assíncrono.
     */
    public void avaliarLeitura(LeituraSensor leitura) {
        Sensor sensor = sensorRepository.findById(leitura.getIdSensor()).orElse(null);
        if (sensor == null || !sensorConfiavel(sensor)) return;

        // Ignora leituras retroativas: só a mais recente do sensor decide a irrigação
        Optional<LeituraSensor> ultima = leituraRepository.findFirstByIdSensorOrderByDataHoraDesc(sensor.getIdSensor());
        if (ultima.isEmpty() || !Objects.equals(ultima.get().getIdLeitura(), leitura.getIdLeitura())) return;

        Talhao talhao = talhaoRepository.findById(sensor.getIdTalhao()).orElse(null);
        if (talhao == null) return;
        Cultura cultura = culturaDoTalhao(talhao);
        if (cultura == null) return;

        List<ComandoIrrigacao> abertos = comandoRepository.findByIdTalhaoAndStatusIn(talhao.getIdTalhao(), ABERTOS);

        if (leitura.getUmidadeSolo() < cultura.getUmidadeMinima()) {
            Optional<Double> rainProbability = probabilityOfRain(talhao);
            if (rainProbability.filter(probability -> probability >= rainProbabilityThreshold).isPresent()) {
                for (ComandoIrrigacao command : abertos) {
                    if (command.getStatus() == StatusComando.PENDENTE) {
                        command.setStatus(StatusComando.CANCELADO);
                        command.setConcluidoEm(LocalDateTime.now());
                        comandoRepository.save(command);
                    }
                }
                return;
            }
            if (abertos.isEmpty()) {
                ComandoIrrigacao cmd = comandoRepository.save(new ComandoIrrigacao(
                        talhao.getIdTalhao(), leitura.getIdLeitura(),
                        leitura.getUmidadeSolo(), cultura.getUmidadeMinima()));
                notifier.notificar(cmd, talhao.getNome(), cultura.getNome());
            }
        } else {
            for (ComandoIrrigacao c : abertos) {
                if (c.getStatus() == StatusComando.ENVIADO) {
                    c.setStatus(StatusComando.CONCLUIDO);
                    c.setConcluidoEm(LocalDateTime.now());
                } else {
                    c.setStatus(StatusComando.CANCELADO);
                }
                comandoRepository.save(c);
            }
        }
    }

    /** Situação de todos os talhões (os que precisam regar aparecem primeiro). */
    public List<IrrigacaoStatusDTO> listarStatus() {
        Map<Long, Cultura> culturas = culturaRepository.findAll().stream()
                .collect(Collectors.toMap(Cultura::getIdCultura, c -> c));
        Map<Long, StatusComando> comandosAbertos = new HashMap<>();
        for (ComandoIrrigacao c : comandoRepository.findByStatusInOrderByCriadoEmAsc(ABERTOS)) {
            comandosAbertos.put(c.getIdTalhao(), c.getStatus());
        }

        List<IrrigacaoStatusDTO> resultado = new ArrayList<>();
        for (Talhao t : talhaoRepository.findAll()) {
            Cultura cultura = culturaDoTalhao(t, culturas);
            Optional<LeituraSensor> ultima = sensorRepository.findByIdTalhao(t.getIdTalhao()).stream()
                    .filter(this::sensorConfiavel)
                    .map(s -> leituraRepository.findFirstByIdSensorOrderByDataHoraDesc(s.getIdSensor()))
                    .flatMap(Optional::stream)
                    .max(Comparator.comparing(LeituraSensor::getDataHora));

            String situacao;
            if (cultura == null) situacao = "SEM_CULTURA";
            else if (ultima.isEmpty()) situacao = "SEM_DADOS";
            else if (ultima.get().getUmidadeSolo() < cultura.getUmidadeMinima()) situacao = "REGAR";
            else if (ultima.get().getUmidadeSolo() > cultura.getUmidadeMaxima()) situacao = "EXCESSO_UMIDADE";
            else situacao = "OK";

            Double rainProbability = null;
            if ("REGAR".equals(situacao)) {
                rainProbability = probabilityOfRain(t).orElse(null);
                if (rainProbability != null && rainProbability >= rainProbabilityThreshold) {
                    situacao = "AGUARDAR_CHUVA";
                }
            }
            StatusComando cmd = comandosAbertos.get(t.getIdTalhao());
            resultado.add(new IrrigacaoStatusDTO(
                    t.getIdTalhao(), t.getNome(),
                    cultura != null ? cultura.getNome() : null,
                    ultima.map(LeituraSensor::getUmidadeSolo).orElse(null),
                    cultura != null ? cultura.getUmidadeMinima() : null,
                    cultura != null ? cultura.getUmidadeMaxima() : null,
                    ultima.map(LeituraSensor::getDataHora).orElse(null),
                    situacao, cmd != null ? cmd.name() : null, rainProbability));
        }
        resultado.sort(Comparator
                .comparing((IrrigacaoStatusDTO d) -> !"REGAR".equals(d.situacao()))
                .thenComparing(IrrigacaoStatusDTO::talhao));
        return resultado;
    }

    /** Comandos aguardando o sistema de irrigação (usado no modo "polling"). */
    public List<ComandoIrrigacaoDTO> listarComandosPendentes() {
        Map<Long, Talhao> talhoes = talhaoRepository.findAll().stream()
                .collect(Collectors.toMap(Talhao::getIdTalhao, t -> t));
        Map<Long, Cultura> culturas = culturaRepository.findAll().stream()
                .collect(Collectors.toMap(Cultura::getIdCultura, c -> c));
        return comandoRepository.findByStatusInOrderByCriadoEmAsc(List.of(StatusComando.PENDENTE)).stream()
                .map(c -> {
                    Talhao t = talhoes.get(c.getIdTalhao());
                    Cultura cu = t != null ? culturaDoTalhao(t, culturas) : null;
                    return new ComandoIrrigacaoDTO(c.getIdComando(), c.getIdTalhao(),
                            t != null ? t.getNome() : null, cu != null ? cu.getNome() : null,
                            c.getUmidadeAtual(), c.getUmidadeMinima(), c.getStatus().name(), c.getCriadoEm());
                }).toList();
    }

    private Cultura culturaDoTalhao(Talhao talhao) {
        Cultura personalizada = culturaPersonalizada(talhao);
        if (personalizada != null) return personalizada;
        if (talhao.getIdCultura() == null) return null;
        return culturaRepository.findById(talhao.getIdCultura()).orElse(null);
    }

    private Cultura culturaDoTalhao(Talhao talhao, Map<Long, Cultura> culturas) {
        Cultura personalizada = culturaPersonalizada(talhao);
        if (personalizada != null) return personalizada;
        return talhao.getIdCultura() != null ? culturas.get(talhao.getIdCultura()) : null;
    }

    private Cultura culturaPersonalizada(Talhao talhao) {
        if (talhao.getUmidadeMinimaPersonalizada() == null
                || talhao.getUmidadeMaximaPersonalizada() == null) return null;
        return Cultura.outra(talhao.getUmidadeMinimaPersonalizada(), talhao.getUmidadeMaximaPersonalizada());
    }

    /** O sistema de irrigação confirma que executou. Retorna false se o comando não existe / já foi encerrado. */
    public boolean confirmarComando(Long idComando) {
        return comandoRepository.findById(idComando)
                .filter(c -> ABERTOS.contains(c.getStatus()))
                .map(c -> {
                    c.setStatus(StatusComando.CONCLUIDO);
                    c.setConcluidoEm(LocalDateTime.now());
                    comandoRepository.save(c);
                    return true;
                }).orElse(false);
    }

    private boolean sensorConfiavel(Sensor s) {
        return s.getStatus() == null || "ATIVO".equalsIgnoreCase(s.getStatus());
    }

    private Optional<Double> probabilityOfRain(Talhao talhao) {
        Fazenda fazenda = fazendaRepository.findById(talhao.getIdFazenda()).orElse(null);
        if (fazenda == null) return Optional.empty();
        Localizacao localizacao = localizacaoRepository.findById(fazenda.getIdLocalizacao()).orElse(null);
        if (localizacao == null || localizacao.getLatitude() == null || localizacao.getLongitude() == null) {
            return Optional.empty();
        }
        return forecastService.probabilityOfRainNext24Hours(localizacao.getLatitude(), localizacao.getLongitude());
    }
}
