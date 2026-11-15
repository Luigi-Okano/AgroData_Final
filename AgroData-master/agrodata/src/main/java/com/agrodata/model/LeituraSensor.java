package com.agrodata.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LEITURA_SENSOR")
public class LeituraSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_leitura")
    private Long idLeitura;

    @Column(name = "id_sensor", nullable = false)
    private Long idSensor;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "umidade_solo", nullable = false)
    private Double umidadeSolo;

    @Column(nullable = false)
    private Double temperatura;

    public LeituraSensor() {}

    public LeituraSensor(Long idSensor, LocalDateTime dataHora, Double umidadeSolo, Double temperatura) {
        this.idSensor = idSensor;
        this.dataHora = dataHora;
        this.umidadeSolo = umidadeSolo;
        this.temperatura = temperatura;
    }

    public Long getIdLeitura() { return idLeitura; }
    public void setIdLeitura(Long idLeitura) { this.idLeitura = idLeitura; }

    public Long getIdSensor() { return idSensor; }
    public void setIdSensor(Long idSensor) { this.idSensor = idSensor; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public Double getUmidadeSolo() { return umidadeSolo; }
    public void setUmidadeSolo(Double umidadeSolo) { this.umidadeSolo = umidadeSolo; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }
}
