package com.agrodata.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class LeituraRequestDTO {

    @NotNull
    private Long idSensor;
    private LocalDateTime dataHora;
    @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
    private Double umidadeSolo;
    @NotNull @DecimalMin("-60.0") @DecimalMax("100.0")
    private Double temperatura;

    public LeituraRequestDTO() {}

    public LeituraRequestDTO(Long idSensor, LocalDateTime dataHora, Double umidadeSolo, Double temperatura) {
        this.idSensor = idSensor;
        this.dataHora = dataHora;
        this.umidadeSolo = umidadeSolo;
        this.temperatura = temperatura;
    }

    public Long getIdSensor() { return idSensor; }
    public void setIdSensor(Long idSensor) { this.idSensor = idSensor; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public Double getUmidadeSolo() { return umidadeSolo; }
    public void setUmidadeSolo(Double umidadeSolo) { this.umidadeSolo = umidadeSolo; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }
}
