package com.agrodata.service;

import com.agrodata.model.LeituraSensor;
import com.agrodata.model.Sensor;
import com.agrodata.model.Talhao;
import com.agrodata.repository.CulturaRepository;
import com.agrodata.repository.LeituraRepository;
import com.agrodata.repository.SensorRepository;
import com.agrodata.repository.TalhaoRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeituraSimuladaServiceTest {

    @Test
    void createsInitialReadingsForANewSensor() {
        Sensor sensor = sensor();
        SensorRepository sensors = mock(SensorRepository.class);
        LeituraRepository readings = mock(LeituraRepository.class);
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        CulturaRepository culturas = mock(CulturaRepository.class);
        IrrigacaoService irrigacao = mock(IrrigacaoService.class);

        when(readings.countByIdSensor(sensor.getIdSensor())).thenReturn(0L);
        when(talhoes.findById(sensor.getIdTalhao())).thenReturn(Optional.of(new Talhao()));
        when(readings.saveAll(any())).thenAnswer(invocation -> {
            Iterable<LeituraSensor> source = invocation.getArgument(0);
            List<LeituraSensor> saved = new ArrayList<>();
            source.forEach(saved::add);
            return saved;
        });

        LeituraSimuladaService service = new LeituraSimuladaService(
                sensors, readings, talhoes, culturas, irrigacao, 3);

        assertEquals(3, service.gerarLeiturasIniciais(sensor).size());
        verify(readings).saveAll(any());
    }

    @Test
    void stopsGeneratingAtTwoThousandReadingsPerSensor() {
        Sensor sensor = sensor();
        SensorRepository sensors = mock(SensorRepository.class);
        LeituraRepository readings = mock(LeituraRepository.class);
        TalhaoRepository talhoes = mock(TalhaoRepository.class);
        CulturaRepository culturas = mock(CulturaRepository.class);
        IrrigacaoService irrigacao = mock(IrrigacaoService.class);

        when(sensors.findAll()).thenReturn(List.of(sensor));
        when(readings.countByIdSensor(sensor.getIdSensor())).thenReturn(1_999L, 2_000L);
        when(talhoes.findById(sensor.getIdTalhao())).thenReturn(Optional.of(new Talhao()));
        when(readings.findFirstByIdSensorOrderByDataHoraDesc(sensor.getIdSensor()))
                .thenReturn(Optional.empty());
        when(readings.save(any(LeituraSensor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeituraSimuladaService service = new LeituraSimuladaService(
                sensors, readings, talhoes, culturas, irrigacao, 48);

        service.gerarLeiturasPeriodicamente();
        service.gerarLeiturasPeriodicamente();

        verify(readings, times(1)).save(any(LeituraSensor.class));
        verify(irrigacao, times(1)).avaliarLeitura(any(LeituraSensor.class));
    }

    private Sensor sensor() {
        Sensor sensor = new Sensor(7L, "Umidade do solo", null, "ATIVO");
        sensor.setIdSensor(11L);
        return sensor;
    }
}
