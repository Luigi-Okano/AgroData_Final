package com.agrodata.service;

import com.agrodata.model.*;
import com.agrodata.repository.*;
import com.agrodata.dto.IrrigacaoStatusDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IrrigacaoServiceRainTest {

    @Test
    void doesNotCreateIrrigationCommandWhenRainProbabilityMeetsThreshold() {
        Fixture fixture = fixture(65.0, List.of());

        fixture.service().avaliarLeitura(fixture.reading());

        verify(fixture.commands(), never()).save(any(ComandoIrrigacao.class));
        verify(fixture.notifier(), never()).notificar(any(), any(), any());
    }

    @Test
    void createsIrrigationCommandWhenRainProbabilityIsBelowThreshold() {
        Fixture fixture = fixture(64.9, List.of());
        when(fixture.commands().save(any(ComandoIrrigacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        fixture.service().avaliarLeitura(fixture.reading());

        verify(fixture.commands()).save(any(ComandoIrrigacao.class));
        verify(fixture.notifier()).notificar(any(ComandoIrrigacao.class), eq("Talhão teste"), eq("Soja"));
    }

    @Test
    void cancelsPendingCommandWhenRainBecomesLikely() {
        ComandoIrrigacao pending = new ComandoIrrigacao(12L, 34L, 15.0, 30.0);
        Fixture fixture = fixture(65.0, List.of(pending));

        fixture.service().avaliarLeitura(fixture.reading());

        assertEquals(StatusComando.CANCELADO, pending.getStatus());
        verify(fixture.commands()).save(pending);
        verify(fixture.notifier(), never()).notificar(any(), any(), any());
    }

    @Test
    void reportsRainWaitingStateAndProbabilityForTheDashboard() {
        Fixture fixture = fixture(65.0, List.of());
        when(fixture.cultures().findAll()).thenReturn(List.of(fixture.culture()));
        when(fixture.commands().findByStatusInOrderByCriadoEmAsc(any())).thenReturn(List.of());
        when(fixture.plots().findAll()).thenReturn(List.of(fixture.plot()));
        when(fixture.sensors().findByIdTalhao(12L)).thenReturn(List.of(fixture.sensor()));

        IrrigacaoStatusDTO status = fixture.service().listarStatus().get(0);

        assertEquals("AGUARDAR_CHUVA", status.situacao());
        assertEquals(65.0, status.probabilidadeChuva());
    }

    private Fixture fixture(double rainProbability, List<ComandoIrrigacao> openCommands) {
        CulturaRepository cultures = mock(CulturaRepository.class);
        TalhaoRepository plots = mock(TalhaoRepository.class);
        SensorRepository sensors = mock(SensorRepository.class);
        LeituraRepository readings = mock(LeituraRepository.class);
        ComandoIrrigacaoRepository commands = mock(ComandoIrrigacaoRepository.class);
        IrrigacaoNotifier notifier = mock(IrrigacaoNotifier.class);
        FazendaRepository farms = mock(FazendaRepository.class);
        LocalizacaoRepository locations = mock(LocalizacaoRepository.class);
        OpenMeteoForecastService forecast = mock(OpenMeteoForecastService.class);

        Sensor sensor = new Sensor(12L, "Umidade do Solo", null, "ATIVO");
        sensor.setIdSensor(7L);
        LeituraSensor reading = new LeituraSensor(7L, LocalDateTime.now(), 15.0, 22.0);
        reading.setIdLeitura(34L);
        Talhao plot = new Talhao(12L, "Talhão teste", null, null);
        plot.setIdTalhao(12L);
        plot.setIdCultura(4L);
        Cultura culture = mock(Cultura.class);
        when(culture.getIdCultura()).thenReturn(4L);
        when(culture.getUmidadeMinima()).thenReturn(30.0);
        when(culture.getUmidadeMaxima()).thenReturn(65.0);
        when(culture.getNome()).thenReturn("Soja");
        Fazenda farm = new Fazenda(1L, 9L, "Fazenda", null);
        Localizacao location = new Localizacao();
        location.setLatitude(-23.5);
        location.setLongitude(-46.6);

        when(sensors.findById(7L)).thenReturn(Optional.of(sensor));
        when(readings.findFirstByIdSensorOrderByDataHoraDesc(7L)).thenReturn(Optional.of(reading));
        when(plots.findById(12L)).thenReturn(Optional.of(plot));
        when(cultures.findById(4L)).thenReturn(Optional.of(culture));
        when(commands.findByIdTalhaoAndStatusIn(eq(12L), any())).thenReturn(openCommands);
        when(farms.findById(12L)).thenReturn(Optional.of(farm));
        when(locations.findById(9L)).thenReturn(Optional.of(location));
        when(forecast.probabilityOfRainNext24Hours(-23.5, -46.6)).thenReturn(Optional.of(rainProbability));

        IrrigacaoService service = new IrrigacaoService(cultures, plots, sensors, readings, commands,
                notifier, farms, locations, forecast, 65.0);
        return new Fixture(service, reading, commands, notifier, cultures, plots, sensors, culture, plot, sensor);
    }

    private record Fixture(IrrigacaoService service, LeituraSensor reading,
                           ComandoIrrigacaoRepository commands, IrrigacaoNotifier notifier,
                           CulturaRepository cultures, TalhaoRepository plots, SensorRepository sensors,
                           Cultura culture, Talhao plot, Sensor sensor) {}
}
