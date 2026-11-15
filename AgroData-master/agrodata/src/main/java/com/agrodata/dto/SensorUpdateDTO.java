package com.agrodata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class SensorUpdateDTO {

    @NotBlank @Size(max = 50)
    private String tipoSensor;
    private LocalDate dataInstalacao;
    @NotBlank @Size(max = 20)
    private String status;

    public String getTipoSensor() { return tipoSensor; }
    public void setTipoSensor(String tipoSensor) { this.tipoSensor = tipoSensor; }

    public LocalDate getDataInstalacao() { return dataInstalacao; }
    public void setDataInstalacao(LocalDate dataInstalacao) { this.dataInstalacao = dataInstalacao; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
