package com.agrodata.dto;

import java.time.LocalDate;

public class SensorResponseDTO {

    private Long idSensor;
    private Long idTalhao;
    private String tipoSensor;
    private LocalDate dataInstalacao;
    private String status;

    public SensorResponseDTO() {}

    public SensorResponseDTO(Long idSensor, Long idTalhao, String tipoSensor, LocalDate dataInstalacao, String status) {
        this.idSensor = idSensor;
        this.idTalhao = idTalhao;
        this.tipoSensor = tipoSensor;
        this.dataInstalacao = dataInstalacao;
        this.status = status;
    }

    public Long getIdSensor() { return idSensor; }
    public void setIdSensor(Long idSensor) { this.idSensor = idSensor; }

    public Long getIdTalhao() { return idTalhao; }
    public void setIdTalhao(Long idTalhao) { this.idTalhao = idTalhao; }

    public String getTipoSensor() { return tipoSensor; }
    public void setTipoSensor(String tipoSensor) { this.tipoSensor = tipoSensor; }

    public LocalDate getDataInstalacao() { return dataInstalacao; }
    public void setDataInstalacao(LocalDate dataInstalacao) { this.dataInstalacao = dataInstalacao; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
