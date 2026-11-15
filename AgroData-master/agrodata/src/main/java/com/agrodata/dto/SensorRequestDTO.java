package com.agrodata.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class SensorRequestDTO {

    @NotNull
    private Long idTalhao;
    @NotBlank @Size(max = 50)
    private String tipoSensor;
    private LocalDate dataInstalacao;
    @Size(max = 20)
    private String status;

    public SensorRequestDTO() {}

    public SensorRequestDTO(Long idTalhao, String tipoSensor, LocalDate dataInstalacao, String status) {
        this.idTalhao = idTalhao;
        this.tipoSensor = tipoSensor;
        this.dataInstalacao = dataInstalacao;
        this.status = status;
    }

    public Long getIdTalhao() { return idTalhao; }
    public void setIdTalhao(Long idTalhao) { this.idTalhao = idTalhao; }

    public String getTipoSensor() { return tipoSensor; }
    public void setTipoSensor(String tipoSensor) { this.tipoSensor = tipoSensor; }

    public LocalDate getDataInstalacao() { return dataInstalacao; }
    public void setDataInstalacao(LocalDate dataInstalacao) { this.dataInstalacao = dataInstalacao; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
