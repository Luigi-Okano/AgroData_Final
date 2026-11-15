package com.agrodata.service;

import com.agrodata.dto.SensorUpdateDTO;
import com.agrodata.model.Sensor;
import com.agrodata.repository.FazendaRepository;
import com.agrodata.repository.LeituraRepository;
import com.agrodata.repository.SensorRepository;
import com.agrodata.repository.TalhaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgroDataServiceSensorUpdateTest {

    private SensorRepository sensorRepository;
    private ProdutorAccessService produtorAccess;
    private AgroDataService service;

    @BeforeEach
    void setUp() {
        sensorRepository = mock(SensorRepository.class);
        produtorAccess = mock(ProdutorAccessService.class);
        service = new AgroDataService(
                mock(LeituraRepository.class),
                mock(FazendaRepository.class),
                sensorRepository,
                mock(TalhaoRepository.class),
                mock(IrrigacaoService.class),
                mock(LeituraSimuladaService.class),
                produtorAccess
        );
    }

    @Test
    void updatesSensorSettingsWithoutChangingItsPlot() {
        Sensor sensor = new Sensor(7L, "Umidade do Solo", LocalDate.of(2025, 1, 10), "ATIVO");
        sensor.setIdSensor(3L);
        when(sensorRepository.findById(3L)).thenReturn(Optional.of(sensor));
        when(sensorRepository.save(sensor)).thenReturn(sensor);
        SensorUpdateDTO dto = new SensorUpdateDTO();
        dto.setTipoSensor("Umidade e Temperatura do Solo");
        dto.setDataInstalacao(LocalDate.of(2025, 2, 1));
        dto.setStatus("manutencao");

        var result = service.atualizarSensor(3L, dto, "produtor", false);

        assertEquals(7L, result.getIdTalhao());
        assertEquals("Umidade e Temperatura do Solo", result.getTipoSensor());
        assertEquals(LocalDate.of(2025, 2, 1), result.getDataInstalacao());
        assertEquals("MANUTENCAO", result.getStatus());
        verify(produtorAccess).exigirAcessoTalhao(7L, "produtor", false);
    }

    @Test
    void doesNotUpdateSensorOutsideProducerAccess() {
        Sensor sensor = new Sensor(7L, "Umidade do Solo", null, "ATIVO");
        when(sensorRepository.findById(3L)).thenReturn(Optional.of(sensor));
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(produtorAccess).exigirAcessoTalhao(7L, "outro-produtor", false);
        SensorUpdateDTO dto = new SensorUpdateDTO();
        dto.setTipoSensor("Umidade do Solo");
        dto.setStatus("ATIVO");

        assertThrows(ResponseStatusException.class,
                () -> service.atualizarSensor(3L, dto, "outro-produtor", false));

        verify(sensorRepository, never()).save(sensor);
    }
}
