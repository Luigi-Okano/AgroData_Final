package com.agrodata.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "SENSOR")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sensor")
    private Long idSensor;

    @Column(name = "id_talhao", nullable = false)
    private Long idTalhao;

    @Column(name = "tipo_sensor", nullable = false, length = 50)
    private String tipoSensor;

    @Column(name = "data_instalacao")
    private LocalDate dataInstalacao;

    @Column(length = 20)
    private String status;

    public Sensor() {}

    public Sensor(Long idTalhao, String tipoSensor, LocalDate dataInstalacao, String status) {
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
