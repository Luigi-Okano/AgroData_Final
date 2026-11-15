package com.agrodata.dto;

import java.time.LocalDateTime;

public class LeituraResponseDTO {

    private Long idLeitura;
    private Long idSensor;
    private LocalDateTime dataHora;
    private Double umidadeSolo;
    private Double temperatura;

    public LeituraResponseDTO() {}

    public LeituraResponseDTO(Long idLeitura, Long idSensor, LocalDateTime dataHora, Double umidadeSolo, Double temperatura) {
        this.idLeitura = idLeitura;
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
