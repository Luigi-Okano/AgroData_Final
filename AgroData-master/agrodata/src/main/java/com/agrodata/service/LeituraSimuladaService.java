package com.agrodata.service;

import com.agrodata.model.Cultura;
import com.agrodata.model.LeituraSensor;
import com.agrodata.model.Sensor;
import com.agrodata.model.Talhao;
import com.agrodata.repository.CulturaRepository;
import com.agrodata.repository.LeituraRepository;
import com.agrodata.repository.SensorRepository;
import com.agrodata.repository.TalhaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LeituraSimuladaService {

    private static final Logger log = LoggerFactory.getLogger(LeituraSimuladaService.class);
    private static final int MAX_LEITURAS_POR_SENSOR = 2_000;

    private final SensorRepository sensorRepository;
    private final LeituraRepository leituraRepository;
    private final TalhaoRepository talhaoRepository;
    private final CulturaRepository culturaRepository;
    private final IrrigacaoService irrigacaoService;
    private final int leiturasIniciais;

    public LeituraSimuladaService(SensorRepository sensorRepository,
                                  LeituraRepository leituraRepository,
                                  TalhaoRepository talhaoRepository,
                                  CulturaRepository culturaRepository,
                                  IrrigacaoService irrigacaoService,
                                  @Value("${agrodata.simulacao.leituras-por-talhao:48}") int leiturasIniciais) {
        this.sensorRepository = sensorRepository;
        this.leituraRepository = leituraRepository;
        this.talhaoRepository = talhaoRepository;
        this.culturaRepository = culturaRepository;
        this.irrigacaoService = irrigacaoService;
        this.leiturasIniciais = Math.max(1, Math.min(leiturasIniciais, MAX_LEITURAS_POR_SENSOR));
    }

    @Transactional
    public synchronized List<LeituraSensor> gerarLeiturasIniciais(Sensor sensor) {
        return persistirLeiturasIniciais(sensor, buscarCultura(sensor));
    }

    @Transactional
    public synchronized List<LeituraSensor> gerarLeiturasIniciais(Sensor sensor, Cultura cultura) {
        return persistirLeiturasIniciais(sensor, cultura);
    }

    @Scheduled(fixedRate = 60_000, initialDelay = 60_000)
    public void gerarLeiturasPeriodicamente() {
        for (Sensor sensor : sensorRepository.findAll()) {
            LeituraSensor leitura;
            try {
                leitura = gerarLeituraSeNecessario(sensor);
            } catch (RuntimeException e) {
                log.error("Falha ao gerar leitura automática para o sensor {}", sensor.getIdSensor(), e);
                continue;
            }

            if (leitura != null) {
                try {
                    irrigacaoService.avaliarLeitura(leitura);
                } catch (Exception e) {
                    log.error("Falha ao avaliar irrigação da leitura automática {}", leitura.getIdLeitura(), e);
                }
            }
        }
    }

    public synchronized LeituraSensor gerarLeituraSeNecessario(Sensor sensor) {
        if (leituraRepository.countByIdSensor(sensor.getIdSensor()) >= MAX_LEITURAS_POR_SENSOR) {
            return null;
        }

        Cultura cultura = buscarCultura(sensor);
        LeituraSensor anterior = leituraRepository
                .findFirstByIdSensorOrderByDataHoraDesc(sensor.getIdSensor())
                .orElse(null);
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        double umidadeAnterior = anterior == null
                ? umidadeInicial(cultura)
                : anterior.getUmidadeSolo();
        LeituraSensor nova = new LeituraSensor(sensor.getIdSensor(), agora,
                arredondar(umidadeSeguinte(umidadeAnterior)),
                arredondar(temperatura(agora)));
        return leituraRepository.save(nova);
    }

    private List<LeituraSensor> persistirLeiturasIniciais(Sensor sensor, Cultura cultura) {
        long existentes = leituraRepository.countByIdSensor(sensor.getIdSensor());
        int quantidade = (int) Math.min(leiturasIniciais,
                Math.max(0, MAX_LEITURAS_POR_SENSOR - existentes));
        if (quantidade == 0) {
            return List.of();
        }

        LocalDateTime agora = LocalDateTime.now().withNano(0);
        double umidade = umidadeInicial(cultura);
        List<LeituraSensor> leituras = new ArrayList<>(quantidade);
        for (int i = 0; i < quantidade; i++) {
            LocalDateTime dataHora = agora.minusHours(quantidade - 1L - i);
            umidade = umidadeSeguinte(umidade);
            leituras.add(new LeituraSensor(sensor.getIdSensor(), dataHora,
                    arredondar(umidade), arredondar(temperatura(dataHora))));
        }
        return leituraRepository.saveAll(leituras);
    }

    private Cultura buscarCultura(Sensor sensor) {
        Talhao talhao = talhaoRepository.findById(sensor.getIdTalhao())
                .orElseThrow(() -> new IllegalStateException(
                        "Talhão " + sensor.getIdTalhao() + " não encontrado para o sensor " + sensor.getIdSensor()));
        if (talhao.getIdCultura() == null) {
            return null;
        }
        return culturaRepository.findById(talhao.getIdCultura())
                .orElseThrow(() -> new IllegalStateException(
                        "Cultura " + talhao.getIdCultura() + " não encontrada para o talhão " + talhao.getIdTalhao()));
    }

    private double umidadeInicial(Cultura cultura) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double minima = cultura != null && cultura.getUmidadeMinima() != null
                ? cultura.getUmidadeMinima() : 30.0;
        double maxima = cultura != null && cultura.getUmidadeMaxima() != null
                ? cultura.getUmidadeMaxima() : 70.0;
        double inferior = Math.max(5.0, minima - 8.0);
        double superior = Math.min(95.0, maxima);
        if (superior <= inferior) {
            inferior = 5.0;
            superior = 95.0;
        }
        return random.nextDouble(inferior, superior);
    }

    private double umidadeSeguinte(double anterior) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double umidade = anterior + random.nextGaussian() * 3.0 - 0.3;
        if (random.nextDouble() < 0.05) {
            umidade += random.nextDouble(8.0, 20.0);
        }
        return Math.max(5.0, Math.min(95.0, umidade));
    }

    private double temperatura(LocalDateTime dataHora) {
        return 24.0 + 6.0 * Math.sin((dataHora.getHour() - 9) * Math.PI / 12)
                + ThreadLocalRandom.current().nextGaussian();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
